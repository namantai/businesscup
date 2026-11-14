"""Manage only AI Compass child processes, with PID reuse checks before stopping."""

import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.runtime'
STATE = RUNTIME / 'processes.json'


def identity(pid):
    try:
        # The process name is parenthesized and can contain spaces.
        stat = Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()
        return None if stat[0] == 'Z' else stat[19]
    except FileNotFoundError:
        return None


def active(record):
    return record.get('start') is not None and identity(record['pid']) == record['start']


def stop(records):
    for record in records:
        if active(record):
            try:
                os.killpg(record['pid'], signal.SIGTERM)
            except ProcessLookupError:
                pass
    deadline = time.monotonic() + 20
    while any(active(record) for record in records) and time.monotonic() < deadline:
        time.sleep(.2)
    for record in records:
        if active(record):
            raise SystemExit('Process did not stop; inspect .runtime logs. No forced kill performed.')
    STATE.unlink(missing_ok=True)


def wait_ready(url):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    if '/actuator/' not in url or json.load(response).get('status') == 'UP':
                        return
        except (OSError, ValueError):
            pass
        time.sleep(.5)
    raise RuntimeError('Readiness timeout; inspect .runtime logs: ' + url)


def main():
    RUNTIME.mkdir(exist_ok=True)
    os.umask(0o077)
    records = json.loads(STATE.read_text()) if STATE.exists() else []
    if sys.argv[1] == 'stop':
        stop(records)
        print('Stopped AI Compass application processes.')
        return
    if any(active(record) for record in records):
        raise SystemExit('AI Compass is already running. Run dev-down.sh before restarting.')
    for key in ['BACKEND_PORT', 'FRONTEND_PORT']:
        with socket.socket() as probe:
            # Match server bind behavior: TIME_WAIT after our stop is not a listener.
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                probe.bind(('127.0.0.1', int(os.environ[key])))
            except OSError:
                raise SystemExit(key + ' is busy. Change .env; no foreign process was stopped.')
    records = []
    processes = [
        ('backend', [str(Path(os.environ['JAVA_HOME']) / 'bin/java'), '-jar', str(ROOT / 'backend/target/ai-compass-0.1.0.jar')], ROOT),
        ('frontend', [str(ROOT / '.tools/node-v24.22.3-linux-x64/bin/node'), 'node_modules/vite/bin/vite.js'], ROOT / 'frontend'),
    ]
    try:
        for name, command, cwd in processes:
            with (RUNTIME / f'{name}.log').open('wb') as log:
                child = subprocess.Popen(command, cwd=cwd, stdout=log, stderr=log, stdin=subprocess.DEVNULL,
                                         start_new_session=True, env={**os.environ, 'COOKIE_SECURE': 'false'})
            records.append({'name': name, 'pid': child.pid, 'start': identity(child.pid)})
            STATE.write_text(json.dumps(records))
        wait_ready(f"http://127.0.0.1:{os.environ['BACKEND_PORT']}/actuator/health/readiness")
        wait_ready(f"http://127.0.0.1:{os.environ['FRONTEND_PORT']}/")
    except Exception:
        stop(records)
        raise
    print('Frontend and backend are ready on loopback.')


if __name__ == '__main__':
    main()
