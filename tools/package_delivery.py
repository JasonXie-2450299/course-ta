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
for suite, filename in (('CoreTest','core-tests.xml'),('SmartActionsTest','f1-actions-tests.xml'),('GitStagedChangesTest','f1-git-tests.xml'),('CommentGeneratorTest','f1-comments-tests.xml')):
    test_file=root/f'core/build/test-results/test/TEST-edu.groupb.courseta.core.{suite}.xml'
    test=ET.parse(test_file).getroot()
    assert all(test.attrib[key]=='0' for key in ('failures','errors','skipped'))
    test_count+=int(test.attrib['tests'])
    shutil.copy2(test_file,out/'evidence'/filename)
assert test_count>=53
assert len(PdfReader(out/'documents/GroupB_F0_Report.pdf').pages)==10
presentation_pages=len(PdfReader(out/'documents/GroupB_F0_F1_Presentation.pdf').pages)
assert presentation_pages==17
with zipfile.ZipFile(out/'documents/GroupB_F0_F1_Presentation.pptx') as slides:
    assert slides.testzip() is None
    assert len([p for p in slides.namelist() if re.fullmatch(r'ppt/slides/slide\d+\.xml',p)])==presentation_pages
with zipfile.ZipFile(plugin) as z:
    assert z.testzip() is None
    assert f'course-ta/lib/course-ta-{version}.jar' in z.namelist()
validation={
    'release':version,'scope':'F0 + all F1 actions; creative action is selected-code comment generation','date':'2026-11-02','ideTarget':'IntelliJ IDEA 2026.1.1 / 261.*',
    'buildPlugin':'passed','coreTests':test_count,'testFailures':0,'reportPages':10,'presentationSlides':presentation_pages,
    'idePluginDiscovery':'0.1.0 previously discovered in sandbox; not re-run for 0.3.0',
    'interactiveIdeTest':'not run for 0.3.0; Computer Use native pipe unavailable in earlier attempts',
    'f1Validation':'10 prompt/output/local HTTP tests, 7 real temporary Git tests and 15 comment insertion tests passed',
    'commentValidation':'Source preservation, stale drafts, strings/text blocks, unsafe insertions, local HTTP, original/annotated sample compilation and behavior',
    'documentBaseline':'PDF report remains F0 0.1.0; presentation now covers F0 + all F1 actions in 0.3.0. See F1_Implementation_Guide.md for details.',
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
target=out/f'GroupB_F0_F1_{version}.zip'
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
