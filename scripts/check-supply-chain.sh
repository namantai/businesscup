#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
python3 - <<'PY'
import json, re, urllib.parse, xml.etree.ElementTree as ET
from pathlib import Path
manifest=json.loads(Path('frontend/package.json').read_text())
lock=json.loads(Path('frontend/package-lock.json').read_text())
assert lock['lockfileVersion']==3
for group in ('dependencies','devDependencies'):
    assert lock['packages'][''][group]==manifest[group]
    for name,version in manifest[group].items():
        assert re.fullmatch(r'\d+\.\d+\.\d+',version), name+' is not pinned'
        assert lock['packages']['node_modules/'+name]['version']==version
for name,item in lock['packages'].items():
    if not name: continue
    assert item.get('integrity','').startswith('sha512-'),name+' lacks SHA512 integrity'
    url=urllib.parse.urlsplit(item['resolved'])
    assert url.scheme=='https' and url.hostname=='registry.npmjs.org',name+' has an unapproved source'
root=ET.parse('backend/pom.xml').getroot(); ns={'m':'http://maven.apache.org/POM/4.0.0'}
assert root.find('m:parent/m:version',ns).text=='4.1.2'
for version in root.findall('.//m:version',ns):
    assert not re.search(r'SNAPSHOT|LATEST|RELEASE|[\[\](),]|-RC|-M\d',version.text,re.I), 'Unstable Maven version'
assert not root.findall('m:repositories',ns), 'Review custom Maven repositories'
wrapper=Path('backend/.mvn/wrapper/maven-wrapper.properties').read_text()
assert re.search(r'distributionSha256Sum=[a-f0-9]{64}',wrapper)
ci=Path('.github/workflows/ci.yml').read_text()
for action in re.findall(r'uses:\s+(\S+)',ci):
    assert re.fullmatch(r'[^@]+@[a-f0-9]{40}',action), 'Unpinned CI action'
print(f'PASS: pinned manifests/lock, {len(lock["packages"])-1} SHA512 registry entries, Maven parent/wrapper and CI action pins.')
PY
(cd frontend && npm ls --all --json > ../.runtime/npm-resolved-tree.json)
# Already cached plugin; offline forbids new downloads and does not claim a live CVE audit.
maven -o org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree -DoutputFile="$COMPASS_ROOT/.runtime/maven-resolved-tree.txt"
if rg -q 'SNAPSHOT|:LATEST|:RELEASE' .runtime/maven-resolved-tree.txt; then
    echo 'FAIL: unstable resolved Maven dependency'; exit 1
fi
echo 'PASS: installed npm tree and offline Maven resolution. NOT_RUN: current external vulnerability feed, signature/provenance audit.'
