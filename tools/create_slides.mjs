import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const runtime=process.env.CODEX_ARTIFACT_RUNTIME || 'C:/Users/Jason Xie/.cache/codex-runtimes/codex-primary-runtime/dependencies';
process.env.RUNTIME_NODE_MODULES=path.join(runtime,'node/node_modules');
const {Presentation,PresentationFile}=await import(pathToFileURL(path.join(runtime,'node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs')).href);
const skill='C:/Users/Jason Xie/.codex/plugins/cache/openai-primary-runtime/presentations/26.1025.11240/skills/presentations';
const tmp=path.join(root,'tmp/slides'), out=path.join(root,'output/documents');
await fs.mkdir(tmp,{recursive:true}); await fs.mkdir(out,{recursive:true});
const audit=JSON.parse(await fs.readFile(path.join(root,'build/audit/course-audit.json'),'utf8'));
const deck=Presentation.create({slideSize:{width:1280,height:720}});
const color={ink:'#172B40',muted:'#536478',accent:'#285F91',bg:'#FAFBFD'};
function text(slide,value,x,y,w,h,size=28,bold=false,tone=color.ink){
  const box=slide.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{fill:'none',width:0}});
  box.text=value; box.text.style={typeface:'Arial',fontSize:size,bold,color:tone}; return box;
}
function slide(title,notes=''){
  const s=deck.slides.add();s.background.fill=color.bg;
  text(s,title,72,54,1136,72,44,true);
  text(s,String(deck.slides.items.length),1160,664,48,28,18,false,color.muted);
  s.speakerNotes.text=notes;return s;
}
function item(s,title,body,y,w=1090){text(s,title,76,y,w,42,30,true,color.accent);text(s,body,76,y+47,w,85,26);}
const sources='Implementation: src/main/java and core/src/main/java. Assignment A, CST3492, AY 2026-2027, pp. 1-3. F0 release only.';
let s=slide('Course TA F0',sources);
text(s,'CST3492 Java Enterprise Application Development',76,152,1100,48,30);
text(s,'Assignment A  |  Group B  |  Release 0.1.0',76,213,1100,42,27,false,color.muted);
text(s,'Full names and matriculation numbers',76,307,1100,45,30,true);
text(s,'Name ____________________   ID ____________________\n\nName ____________________   ID ____________________\n\nName ____________________   ID ____________________\n\nName ____________________   ID ____________________',76,369,1100,255,25);

s=slide('F0 functionality',sources);
item(s,'Course questions with evidence','Local lecture retrieval supplies relevant passages to a configurable LLM.',153);
item(s,'Document and page references','Course answers cite source IDs that resolve to the original document and page.',310);
item(s,'Questions about selected Java code','An editor action attaches the selected text and line range to the tool window.',467);

s=slide('Course material retrieval',sources+' UI: production Swing component rendered offscreen; retrieval preview, not a live LLM response.');
s.images.add({blob:new Uint8Array(await fs.readFile(path.join(root,'build/ui-samples/retrieval-preview.png'))),contentType:'image/png',alt:'Production Course TA retrieval preview',fit:'contain',position:{left:735,top:140,width:405,height:518}});
text(s,'Import the five lecture PDFs.\n\nPreview a question before configuring a model.\n\nInspect the source document and physical page number.',76,168,588,332,29);
text(s,'Example\nPolymorphism → 2.pdf, page 22',76,525,610,90,28,true,color.accent);
text(s,'Offscreen UI sample; no LLM call',735,664,430,24,16,false,color.muted);

s=slide('Selected code context',sources+' UI: production Swing component rendered offscreen with Demo.java.');
s.images.add({blob:new Uint8Array(await fs.readFile(path.join(root,'build/ui-samples/selected-code.png'))),contentType:'image/png',alt:'Production Course TA with selected Java code',fit:'contain',position:{left:735,top:140,width:405,height:518}});
text(s,'1  Select code in the editor.\n\n2  Right-click Ask Course TA About Selected Code.\n\n3  Enter a question and review the attachment.\n\n4  Click Ask TA.',76,161,590,377,28);
text(s,'The rest of the project is not sent.',76,576,600,55,27,true,color.accent);
text(s,'Offscreen UI component sample',735,664,430,24,16,false,color.muted);

s=slide('Architecture',sources+' JetBrains tool windows: https://plugins.jetbrains.com/docs/intellij/tool-windows.html');
item(s,'IDE layer','Tool window, editor action, settings and background task control.',151);
item(s,'Local RAG layer','PDFBox extraction → page-preserving chunks → BM25 index → source IDs.',305);
item(s,'LLM layer','Question + selected code + retrieved excerpts → HTTP request → citation checks.',459);

s=slide('Chunking and retrieval',sources+' PDFBox: https://pdfbox.apache.org/3.0/migration.html');
text(s,'1,400 characters per chunk\n200-character target overlap\nNo chunk crosses a PDF page',76,159,1090,160,34,true,color.accent);
text(s,'BM25 ranks local passages using an inverted index.\n\nThe relevance gate requires score ≥ 1.0 and ≥ 20% query-term coverage.\n\nAt most four excerpts are included in each model request.',76,366,1090,238,29);

s=slide('Citation and fallback behavior',sources+' DeepSeek request schema: https://api-docs.deepseek.com/api/create-chat-completion/');
item(s,'Relevant course evidence','The prompt assigns [S1], [S2] and other IDs. The answer must cite valid IDs.',149);
item(s,'No supporting course evidence','The answer explicitly states that it is based on general knowledge.',303);
item(s,'Missing or invalid references','The application withholds the answer and asks for a retry. Semantic accuracy still requires review.',457);

s=slide('Validation results','Evidence: core/build/test-results/test/TEST-edu.groupb.courseta.core.CoreTest.xml and build/audit/course-audit.json. Tests use a local HTTP stub; no real provider credential was supplied.');
text(s,`${audit.filesRead} lecture PDFs   |   ${audit.pdfPages} pages   |   ${audit.chunks} chunks`,76,164,1100,70,40,true,color.accent);
text(s,'18 automated tests passed\n\nPDF provenance, retrieval, persistence, prompt limits, citation gates and HTTP handling are covered.\n\nPlugin ZIP built against IntelliJ IDEA 2026.1.1.',76,278,1090,247,30);
text(s,'Live provider and interactive IDE demonstration remain to be checked by the team.',76,578,1090,70,24,false,color.muted);

s=slide('Team demonstration and F1 milestone',sources);
text(s,'F0 demonstration',76,148,540,46,31,true,color.accent);
text(s,'Course question and page citation\n\nSelected-code question\n\nGeneral-knowledge fallback\n\nArchitecture and design limits',76,211,545,300,28);
text(s,'Group B next milestone',688,148,520,46,31,true,color.accent);
text(s,'Scenario-based JUnit tests\n\nStaged-diff commit messages\n\nOne creative smart action',688,211,510,270,28);
text(s,'Actual team roles and collaboration evidence: __________________________',76,583,1100,75,24,false,color.muted);

for(const [i,sl] of deck.slides.items.entries()){
  const blob=await deck.export({slide:sl,format:'png',scale:1});
  await fs.writeFile(path.join(tmp,`slide-${String(i+1).padStart(2,'0')}.png`),new Uint8Array(await blob.arrayBuffer()));
}
const candidatePath=path.join(tmp,'candidate.pptx');
await(await PresentationFile.exportPptx(deck)).save(candidatePath);
const {finalizePresentation}=await import(pathToFileURL(path.join(skill,'container_tools/artifact_tool_utils.mjs')).href);
let finalPath=path.join(out,'GroupB_F0_Presentation.pptx');
try { await fs.access(finalPath); finalPath=path.join(out,`GroupB_F0_Presentation_${Date.now()}.pptx`); } catch {}
await finalizePresentation({workspaceDir:root,candidatePath,finalPath,
  pythonExecutable:path.join(runtime,'python/python.exe'),
  integrityValidatorPath:path.join(skill,'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath:path.join(skill,'container_tools/inspect_presentation_layout_geometry.py'),
  layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-bullet-geometry','--validate-heading-fit'],
  fontPolicy:{basis:'design',families:['Arial']},verifyArtifactToolImport:true,
  receiptPath:path.join(tmp,'validation.json')});
try {
  const pdf=await deck.export({format:'pdf'});
  await fs.writeFile(path.join(out,'GroupB_F0_Presentation.pdf'),new Uint8Array(await pdf.arrayBuffer()));
} catch(error) {
  const {execFileSync}=await import('node:child_process');
  execFileSync(path.join(runtime,'python/python.exe'),[path.join(root,'tools/create_slide_pdf.py')],{stdio:'inherit'});
}
console.log('Exported 9-slide F0 presentation and PDF.');
