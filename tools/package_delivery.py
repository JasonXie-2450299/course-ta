from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile
from pypdf import PdfReader
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
out=root/'output'
(out/'plugin').mkdir(exist_ok=True)
(out/'evidence').mkdir(exist_ok=True)
version=re.search(r'^version = "([^"]+)"', (root/'build.gradle.kts').read_text(encoding='utf-8'), re.MULTILINE).group(1)
plugin=root/f'build/distributions/course-ta-{version}.zip'
shutil.copy2(plugin,out/'plugin'/plugin.name)
shutil.copy2(root/'build/audit/course-audit.json',out/'evidence/course-audit.json')
test_count=0
for suite, filename in (('CoreTest','core-tests.xml'),('SmartActionsTest','f1-actions-tests.xml'),('GitStagedChangesTest','f1-git-tests.xml')):
    test_file=root/f'core/build/test-results/test/TEST-edu.groupb.courseta.core.{suite}.xml'
    test=ET.parse(test_file).getroot()
    assert all(test.attrib[key]=='0' for key in ('failures','errors','skipped'))
    test_count+=int(test.attrib['tests'])
    shutil.copy2(test_file,out/'evidence'/filename)
assert test_count>=38
assert len(PdfReader(out/'documents/GroupB_F0_Report.pdf').pages)==10
assert len(PdfReader(out/'documents/GroupB_F0_Presentation.pdf').pages)==9
with zipfile.ZipFile(plugin) as z:
    assert z.testzip() is None
    assert f'course-ta/lib/course-ta-{version}.jar' in z.namelist()
validation={
    'release':version,'scope':'F0 + F1 JUnit and staged commit actions; creative action deferred by user','date':'2026-11-01','ideTarget':'IntelliJ IDEA 2026.1.1 / 261.*',
    'buildPlugin':'passed','coreTests':test_count,'testFailures':0,'reportPages':10,'presentationSlides':9,
    'idePluginDiscovery':'0.1.0 previously discovered in sandbox; not re-run for 0.2.0',
    'interactiveIdeTest':'not run; Computer Use native pipe unavailable (os error 2)',
    'f1Validation':'10 prompt/output/local HTTP tests and 7 tests with real temporary Git repositories passed',
    'documentBaseline':'PDF report and slides describe F0 0.1.0; see F0_0.1.1_Changes.md and F1_Implementation_Guide.md for updates',
    'liveLlmTest':'not run; no real provider credential supplied',
    'gradleConfigurationAdvisory':'until-build intentionally restricts this release to the target 261.* series',
    'uiSamples':'production Swing component rendered offscreen',
    'slidePdf':'reviewed raster slide renders; editable source in PPTX'
}
(out/'evidence/release-validation.json').write_text(json.dumps(validation,indent=2),encoding='utf-8')
records=[]
for folder in ('documents','plugin','evidence'):
    for file in sorted((out/folder).glob('*')):
        if file.is_file() and (folder!='plugin' or file.name==plugin.name): records.append(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.relative_to(out).as_posix()}')
(out/'SHA256SUMS.txt').write_text('\n'.join(records)+'\n',encoding='utf-8')
target=out/'GroupB_F0_F1_Partial_Delivery.zip'
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
    for name in ('.gitignore','README.md','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','core/build.gradle.kts'):
        z.write(root/name,'source/'+name)
    for folder in ('src','core/src','gradle','course-materials','samples','tools'):
        for file in sorted((root/folder).rglob('*')):
            if file.is_file() and not any(part in {'.idea','build','__pycache__','.cache'} for part in file.relative_to(root).parts):
                z.write(file,'source/'+file.relative_to(root).as_posix())
    for folder in ('documents','plugin','evidence'):
        for file in sorted((out/folder).glob('*')):
            if file.is_file() and (folder!='plugin' or file.name==plugin.name): z.write(file,file.relative_to(out).as_posix())
    z.write(out/'START_HERE_中文.md','START_HERE_中文.md')
    z.write(out/'SHA256SUMS.txt','SHA256SUMS.txt')
with zipfile.ZipFile(target) as z:
    assert z.testzip() is None
    assert not any('/.idea/' in p or '/build/' in p or '/.gradle/' in p for p in z.namelist())
    print(f'Validated delivery: {len(z.namelist())} files, {target.stat().st_size:,} bytes')
print(target)
