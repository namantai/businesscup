#!/usr/bin/env python3
"""Own, temporary PostgreSQL + app processes. Never touches the dev catalog or volumes."""
import json
import os
from pathlib import Path
import secrets
import signal
import socket
import subprocess
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parent.parent
RUN = ROOT / '.runtime' / ('e2e-' + uuid.uuid4().hex[:12])
RUN.mkdir(mode=0o700, parents=True)
NAME = 'ai-compass-' + RUN.name
IMAGE = 'postgres@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536'
processes = []
logs = []
created = False


def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def ready(url, process):
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError('Own application exited; inspect ignored E2E logs: ' + str(RUN))
        try:
            with urllib.request.urlopen(url, timeout=1) as response:
                if response.status == 200:
                    return
        except (OSError, TimeoutError):
            pass
        time.sleep(0.25)
    raise RuntimeError('Own application readiness timed out: ' + str(RUN))


def start(args, cwd, env, name):
    log = open(RUN / (name + '.log'), 'w')
    logs.append(log)
    process = subprocess.Popen(args, cwd=cwd, env=env, stdout=log, stderr=log, start_new_session=True)
    processes.append(process)
    return process


def interrupted(signum, frame):
    raise KeyboardInterrupt


signal.signal(signal.SIGTERM, interrupted)
try:
    env = os.environ.copy()
    env.update(POSTGRES_PASSWORD=secrets.token_hex(24), POSTGRES_DB='compass_e2e', POSTGRES_USER='compass_e2e')
    subprocess.run(['docker', 'run', '--rm', '-d', '--name', NAME,
                    '--label', 'com.aicompass.scope=p03-e2e', '--tmpfs', '/var/lib/postgresql/data:rw',
                    '-p', '127.0.0.1::5432', '-e', 'POSTGRES_PASSWORD', '-e', 'POSTGRES_DB', '-e', 'POSTGRES_USER', IMAGE],
                   env=env, check=True, stdout=subprocess.DEVNULL)
    created = True
    for _ in range(60):
        if subprocess.run(['docker', 'exec', NAME, 'pg_isready', '-U', 'compass_e2e'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0:
            break
        time.sleep(0.25)
    else:
        raise RuntimeError('Isolated PostgreSQL readiness failed')
    binding = subprocess.check_output(['docker', 'port', NAME, '5432/tcp'], text=True).strip()
    assert binding.startswith('127.0.0.1:')
    backend_port = free_port()
    frontend_port = free_port()
    while frontend_port == backend_port:
        frontend_port = free_port()
    env.update(DB_PORT=binding.split(':')[-1], DB_NAME='compass_e2e', DB_USER='compass_e2e', DB_PASSWORD=env['POSTGRES_PASSWORD'],
               BACKEND_PORT=str(backend_port), FRONTEND_PORT=str(frontend_port), SPRING_PROFILES_ACTIVE='test',
               COMPASS_CATALOG_ALLOW_DEMO='true', COOKIE_SECURE='false', LLM_MODE='OFF', PAYMENTS_MODE='OFF',
               EMAIL_MODE='LOCAL', SOURCE_FETCH_ENABLED='false', COMPASS_E2E_DIR=str(RUN),
               PLAYWRIGHT_BROWSERS_PATH=str(ROOT / '.cache' / 'playwright'))
    backend = start(['java', '-jar', str(ROOT / 'backend/target/ai-compass-0.1.0.jar')], ROOT, env, 'backend')
    ready(f'http://127.0.0.1:{backend_port}/actuator/health/readiness', backend)
    frontend = start(['npm', 'run', 'dev', '--', '--host', '127.0.0.1', '--port', str(frontend_port)], ROOT / 'frontend', env, 'frontend')
    ready(f'http://127.0.0.1:{frontend_port}', frontend)
    result = subprocess.run(['npm', 'run', 'test:e2e'], cwd=ROOT / 'frontend', env=env)
    raise SystemExit(result.returncode)
finally:
    for process in reversed(processes):
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=5)
    for log in logs:
        log.close()
    if created:
        # --rm with tmpfs: no persistent volume is created or deleted.
        subprocess.run(['docker', 'stop', '-t', '5', NAME], check=True, stdout=subprocess.DEVNULL)
