import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const runtime = process.env.CODEX_ARTIFACT_RUNTIME || 'C:/Users/Jason Xie/.cache/codex-runtimes/codex-primary-runtime/dependencies';
process.env.RUNTIME_NODE_MODULES = path.join(runtime, 'node/node_modules');
const skill = 'C:/Users/Jason Xie/.codex/plugins/cache/openai-primary-runtime/presentations/26.1026.10807/skills/presentations';
const { FileBlob, PresentationFile } = await import(pathToFileURL(path.join(runtime, 'node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs')).href);
const out = path.join(root, 'output/documents');
const tmp = path.join(root, 'tmp/slides-f1');
const baseline = path.join(root, 'tools/assets/GroupB_F0_Baseline.pptx');
await fs.mkdir(path.dirname(baseline), { recursive: true });
await fs.mkdir(tmp, { recursive: true });
try { await fs.access(baseline); } catch { await fs.copyFile(path.join(out, 'GroupB_F0_Presentation.pptx'), baseline); }
const deck = await PresentationFile.importPptx(await FileBlob.load(baseline));
if (deck.slides.items.length !== 9) throw new Error('Expected the preserved nine-slide F0 baseline.');
const originals = [...deck.slides.items];
const before = await deck.inspect({ kind: 'slide,textbox,image,notes', maxChars: 100000 });
await fs.writeFile(path.join(tmp, 'before.json'), JSON.stringify(before, null, 2));
const c = { ink: '#172B40', muted: '#536478', accent: '#285F91', bg: '#FAFBFD' };
function text(s, value, x, y, w, h, size = 28, bold = false, tone = c.ink) {
  const box = s.shapes.add({ geometry: 'textbox', position: { left: x, top: y, width: w, height: h }, fill: 'none', line: { fill: 'none', width: 0 } });
  box.text = value; box.text.style = { typeface: 'Arial', fontSize: size, bold, color: tone };
  return box;
}
function replace(s, old, value) {
  const match = before.records.filter(r => r.kind === 'textbox' && r.slideIndex === originals.indexOf(s) && r.text === old);
  if (match.length !== 1) throw new Error(`Cannot identify text: ${old}`);
  deck.resolve(match[0].id).text.replace(old, value);
}
let anchor = originals[6];
function slide(title, notes) {
  const { slide: s } = deck.slides.insert({ after: anchor }); anchor = s; s.background.fill = c.bg;
  text(s, title, 72, 54, 1136, 72, 44, true);
  text(s, '0', 1160, 664, 48, 28, 18, false, c.muted);
  s.speakerNotes.text = notes; return s;
}
function item(s, title, body, y) {
  text(s, title, 76, y, 1090, 42, 30, true, c.accent);
  text(s, body, 76, y + 47, 1090, 85, 26);
}
const f1src = 'Sources: Assignment A, F1 (page 2); output/documents/F1_Implementation_Guide.md; current release 0.3.0 source. ';
replace(originals[0], 'Course TA F0', 'Course TA F0 + F1');
replace(originals[0], 'Assignment A  |  Group B  |  Release 0.1.0', 'Assignment A  |  Group B  |  Release 0.3.0');
replace(originals[4], 'Architecture', 'F0 architecture');
originals[0].speakerNotes.text = f1src + 'Introduce the course assistant and the three implemented smart actions. Fill in the real team members and matriculation numbers before submission. Suggested time: 30 seconds.';

let s = slide('F1 smart actions', f1src + 'The creative action is automatic comments for selected Java code. F1 uses a fixed Java-controlled request flow rather than model-directed tool calls. Suggested time: 1 minute.');
item(s, 'Scenario-based JUnit tests', 'Selected code + a natural-language scenario produce a JUnit 5 test-class draft.', 151);
item(s, 'Git commit messages', 'The complete staged diff produces a Conventional Commit message draft.', 305);
item(s, 'Creative action: code comments', 'Selected Java lines receive Chinese or English comments for review and application.', 459);

s = slide('JUnit generation in IntelliJ', f1src + 'Sources: GenerateJUnitAction.java, SmartActionPanel.java and SmartActions.java. The complete current Java file includes unsaved edits. Save test as uses a new-file-only write. Add JUnit Jupiter 5 in the target project. Generated tests are not compiled or executed by the plugin. Suggested time: 1 minute 30 seconds.');
text(s, '1  Select a class or method in a Java editor.', 76, 155, 1110, 47, 29);
text(s, '2  Right-click Generate JUnit Tests with Course TA.', 76, 224, 1110, 47, 29);
text(s, '3  Describe the test scenario, then click Generate.', 76, 293, 1110, 47, 29);
text(s, '4  Review the draft in F1 Actions. Copy result or Save test as...', 76, 362, 1110, 80, 29);
text(s, 'Run the saved tests in the target project', 76, 487, 1100, 43, 30, true, c.accent);
text(s, 'Use JUnit Jupiter 5 and the correct package / class name.\nCheck the assertions and dependencies before running.', 76, 540, 1100, 86, 26);

s = slide('JUnit context and scenario', f1src + 'Sources: samples/Demo.java; SmartActions.java. The assertion excerpt is illustrative, hand-authored for explanation, not captured live model output. The actual action requests one complete test class. The generator checks basic class and Jupiter structure, not compilation or scenario coverage. Suggested time: 1 minute 30 seconds.');
text(s, 'Example scenario: Demo.readUser', 76, 148, 1100, 45, 31, true, c.accent);
text(s, 'Reject null, empty and whitespace-only IDs.\nFor ID "42", return "User: 42".', 76, 203, 1100, 92, 29);
text(s, 'Context sent to the model', 76, 338, 495, 45, 29, true, c.accent);
text(s, 'Scenario and selected code\n\nFilename and line range\n\nComplete current Java file', 76, 400, 490, 197, 26);
text(s, 'Illustrative assertion excerpt', 640, 338, 555, 45, 29, true, c.accent);
text(s, 'assertThrows(IOException.class,\n    () -> Demo.readUser(" "));\n\nassertEquals("User: 42",\n    Demo.readUser("42"));', 640, 401, 552, 192, 25);
text(s, 'Basic output checks verify class / JUnit structure; review still determines correctness.', 76, 622, 1100, 35, 22, false, c.muted);

s = slide('Git commit messages', f1src + 'Sources: GitStagedChanges.java, SmartActions.java and SmartActionPanel.java. Collection uses git diff --cached with binary/full-index/no-renames and read-only options, resolves the repository root, includes all staged files and supports initial commits. No Git staging, commit or push is executed. The message below is illustrative. Suggested time: 2 minutes including a staged/unstaged demonstration.');
item(s, '1  Stage the intended files or hunks', 'Modified files must be staged before the plugin can read them.', 146);
item(s, '2  Choose the repository and preview', 'Preview staged diff shows all staged changes. Unstaged edits are excluded.', 287);
item(s, '3  Generate, review and copy the message', 'Generate commit message reads a fresh snapshot for the model request.', 428);
text(s, 'Illustrative message:  feat: add selected-code comment generation', 76, 580, 1100, 40, 25, true, c.accent);
text(s, '“No staged changes” means the staging area is empty. Stage changes, then retry.', 76, 632, 1100, 31, 22, false, c.muted);

s = slide('Creative action: code comments', f1src + 'Sources: GenerateCommentsAction.java and SmartActionPanel.java. Select full methods or meaningful segments. The action expands the selection to complete physical lines. Chinese is the default; English is also available. Application uses WriteCommandAction with the name Apply Course TA Comments. The command is designed to support Undo; live IDE undo has not been verified for this release. Suggested time: 1 minute 30 seconds.');
text(s, '1  Select Java code and right-click', 76, 150, 1100, 45, 29);
text(s, 'Generate Comments with Course TA.', 115, 198, 1060, 45, 29, true, c.accent);
text(s, '2  Choose Chinese or English, then Generate.', 76, 274, 1110, 47, 29);
text(s, '3  Review the read-only preview in F1 Actions.', 76, 350, 1110, 47, 29);
text(s, '4  Copy result or Apply comments to the source editor.', 76, 426, 1110, 47, 29);
text(s, 'The applied edit uses an undoable IDE command.', 76, 526, 1100, 46, 29, true, c.accent);
text(s, 'If the source changes after generation, regenerate the draft before applying.', 76, 585, 1100, 67, 25, false, c.muted);

s = slide('Comment insertion design', f1src + 'Source: core/src/main/java/edu/groupb/courseta/core/CommentGenerator.java. The JSON schema has comments with line, kind and text. Line is 1-based relative to the selection. Kinds are javadoc and block. At most 40 unique positions, 1500 characters per comment. The complete source lexer tracks strings, characters, comments and text blocks. Comment terminators, Java Unicode escape sequences and invalid controls are rejected. Full-source equality is checked before applying the draft. Generated explanation accuracy still requires review. Suggested time: 1 minute 30 seconds.');
item(s, 'The model proposes structured insertions', 'JSON contains the relative line number, comment kind and comment text.', 151);
item(s, 'Java validates the insertion plan', 'Only code-state line starts are accepted. Strings and existing comment blocks are protected.', 305);
item(s, 'The original code supplies the final draft', 'The plugin inserts comment blocks into the captured source; every original character is retained.', 459);

s = slide('Code comments: before and after', f1src + 'Illustrative hand-authored example, not a real model response. It shows the insertion approach: the original method stays identical and a Javadoc block is added above it. The executable regression test compiles original and annotated fixtures and compares behavior for positive and negative inputs. That controlled test does not prove all generated prose is correct. Suggested time: 1 minute.');
text(s, 'Selected Java code', 76, 151, 495, 45, 31, true, c.accent);
text(s, 'public int twice(int n) {\n    return n * 2;\n}', 76, 220, 490, 155, 27);
text(s, 'Preview after insertion', 640, 151, 555, 45, 31, true, c.accent);
text(s, '/**\n * Returns twice the input.\n * @param n input value\n * @return twice n\n */\npublic int twice(int n) {\n    return n * 2;\n}', 640, 220, 545, 300, 26);
text(s, 'Original method body preserved', 76, 468, 485, 90, 31, true, c.accent);
text(s, 'Illustrative example. Review the explanation before applying it.', 76, 605, 1100, 45, 25, false, c.muted);

s = slide('F1 architecture', f1src + 'Sources: src/main/java/edu/groupb/courseta/ide/GenerateJUnitAction.java, GenerateCommitAction.java, GenerateCommentsAction.java, SmartActionPanel.java, TaProjectService.java; core/src/main/java/edu/groupb/courseta/core/SmartActions.java, CommentGenerator.java, GitStagedChanges.java and LlmClient.java. F1 does not use BM25 retrieval or model-directed tool calling. Shared configuration is under Course Q&A > Settings. Git/HTTP/export run in background tasks; stale/cancelled responses do not update the panel. Context limits: scenario 6000 chars, selected code 12000, full file 60000, staged diff 120000. Over-limit context is rejected without truncation. Suggested time: 1 minute 30 seconds.');
text(s, 'IDE entry points', 76, 146, 350, 40, 29, true, c.accent);
text(s, 'GenerateJUnitAction / GenerateCommitAction / GenerateCommentsAction', 76, 194, 1110, 45, 26);
text(s, 'Shared interaction', 76, 268, 350, 40, 29, true, c.accent);
text(s, 'SmartActionPanel: input, preview, background work, export and apply', 76, 316, 1110, 45, 26);
text(s, 'Core implementation', 76, 390, 400, 40, 29, true, c.accent);
text(s, 'SmartActions: tests / commit prompts     GitStagedChanges: staged snapshot\nCommentGenerator: insertion plan and source-preserving draft', 76, 438, 1110, 94, 26);
text(s, 'LlmClient and PasswordSafe settings are shared with F0.', 76, 568, 1100, 42, 26, true, c.accent);
text(s, 'Fixed Java-controlled flow. Oversize requests are rejected without partial context.', 76, 619, 1100, 39, 23, false, c.muted);

replace(originals[7], '18 automated tests passed\n\nPDF provenance, retrieval, persistence, prompt limits, citation gates and HTTP handling are covered.\n\nPlugin ZIP built against IntelliJ IDEA 2026.1.1.', '53 automated tests passed\n\n21 F0  |  10 test / commit  |  7 Git  |  15 comments\n\nPlugin ZIP built against IntelliJ IDEA 2026.1.1.');
replace(originals[7], 'Live provider and interactive IDE demonstration remain to be checked by the team.', 'Local HTTP stubs and controlled Java fixtures were tested.\nLive provider calls and interactive IDE / Undo checks remain pending.');
originals[7].speakerNotes.text = 'Evidence: output/evidence/core-tests.xml, f1-actions-tests.xml, f1-git-tests.xml, f1-comments-tests.xml and course-audit.json. Counts are 21 + 10 + 7 + 15 = 53, with zero failures/errors/skips. Git tests use real temporary repositories. Comment tests include original/annotated Java compilation and behavior matching. HTTP tests use a local stub. No real provider credential was supplied. Interactive clicks and IDE Undo have not been verified for 0.3.0. Suggested time: 1 minute.';
replace(originals[8], 'Team demonstration and F1 milestone', 'Team demonstration');
replace(originals[8], 'Group B next milestone', 'F1 demonstration');
replace(originals[8], 'Scenario-based JUnit tests\n\nStaged-diff commit messages\n\nOne creative smart action', 'Scenario-based JUnit draft\n\nStaged-diff commit message\n\nComments: preview and apply');
originals[8].speakerNotes.text = f1src + 'Use the live plugin only after configuring a provider and rehearsing. For F0, ask a course question, inspect the physical page citation, attach selected code and show the general-knowledge fallback. For F1, use samples/Demo.java, stage an intended Git edit and preview its exact snapshot, then generate comments, review, apply and test Undo. Suggested schedule: 5 minutes for slides 1-7; 11.5 minutes for slides 8-15 including short demonstrations; 1 minute for slide 16; 2.5 minutes for this final demonstration / questions, total 20 minutes. Replace the roles placeholder with actual collaboration evidence; do not invent team contributions.';

for (const [i, sl] of deck.slides.items.entries()) {
  const number = sl.shapes.items.find(x => x.position.left >= 1150 && x.position.top >= 660 && /^\d+$/.test(x.text.toString()));
  if (!number) throw new Error(`Missing page marker on slide ${i + 1}`);
  number.text = String(i + 1);
  const png = await deck.export({ slide: sl, format: 'png', scale: 1.5 });
  await fs.writeFile(path.join(tmp, `slide-${String(i + 1).padStart(2, '0')}.png`), new Uint8Array(await png.arrayBuffer()));
  console.log(`Rendered slide ${i + 1}`);
}
await fs.writeFile(path.join(tmp, 'after.json'), JSON.stringify(await deck.inspect({ kind: 'slide,textbox,image,notes', maxChars: 200000 }), null, 2));
const candidatePath = path.join(tmp, 'candidate.pptx');
await (await PresentationFile.exportPptx(deck)).save(candidatePath);
const { finalizePresentation } = await import(pathToFileURL(path.join(skill, 'container_tools/artifact_tool_utils.mjs')).href);
let finalPath = path.join(out, 'GroupB_F0_F1_Presentation.pptx');
try { await fs.access(finalPath); finalPath = path.join(out, `GroupB_F0_F1_Presentation_${Date.now()}.pptx`); } catch {}
await finalizePresentation({ workspaceDir: root, candidatePath, finalPath,
  pythonExecutable: path.join(runtime, 'python/python.exe'),
  integrityValidatorPath: path.join(skill, 'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath: path.join(skill, 'container_tools/inspect_presentation_layout_geometry.py'),
  explicitTotalSlideCount: 17,
  layoutArgs: ['--expected-slide-size-emu', '12192000,6858000', '--validate-bullet-geometry', '--validate-heading-fit'],
  fontPolicy: { basis: 'reference', families: ['Arial'], referencePath: baseline, referenceSha256: crypto.createHash('sha256').update(await fs.readFile(baseline)).digest('hex') },
  verifyArtifactToolImport: true, receiptPath: path.join(tmp, 'validation.json') });
await fs.writeFile(path.join(tmp, 'manifest.json'), JSON.stringify({ finalPath, slides: deck.slides.items.length, renderDirectory: tmp }, null, 2));
console.log(`Validated ${deck.slides.items.length} editable slides: ${finalPath}`);
