# Course TA for CST3492

IntelliJ IDEA teaching assistant for Assignment A, Group B. Release **0.3.0** implements **F0** and all three **F1** actions: scenario-based JUnit generation, staged Git commit messages, and selected-code comment generation as the creative action.

## Features

- Import a single PDF, UTF-8 TXT or Markdown file, or an entire course folder; PDF chunks retain their physical page numbers.
- Local BM25 retrieval, persisted index and four-excerpt prompt budget.
- Configurable Chat Completions endpoint, model and API key stored in IDEA PasswordSafe.
- Course answers with source IDs, document names and page/line scope; explicit general-knowledge notice when no course evidence is used.
- Course TA tool window and editor right-click action for selected-code questions.
- Offline retrieval preview, cancellation, import warnings and HTTP error handling.
- JUnit 5 test generation from a required natural-language scenario, selected Java code and its current file context.
- Complete staged-diff preview and Conventional Commit message generation, including repositories without a first commit.
- Editable result preview, copy, and save-as for new test files; no automatic staging or committing.
- Chinese/English comments for selected Java lines, with validated comment-only insertions, preview, explicit apply and IDE undo support.

## Build and run

Use **JDK 21 or later** and **IntelliJ IDEA 2026.1.x**. Java sources target Java 21. The Gradle wrapper is included. First build requires network access to Maven and JetBrains repositories.

On this Windows machine:

```powershell
.\tools\build.ps1 -Task Build
.\tools\build.ps1 -Task Audit
.\tools\build.ps1 -Task Run
```

The helper reuses the installed IDEA 2026.1.1 SDK and works around a Windows temporary-socket-path issue only for the child build. To use another installed 2026.1 IDE, pass `-IdePath 'C:\path\to\IntelliJ IDEA'`.

Portable Gradle commands (without a local SDK, Gradle downloads IDEA 2026.1.1):

```sh
./gradlew :core:test buildPlugin
./gradlew :core:auditCourse -PcourseDir=course-materials
./gradlew runIde
```

Install `build/distributions/course-ta-0.3.0.zip` via **Settings > Plugins > gear > Install Plugin from Disk**, then restart when prompted. Open a local project and choose **View > Tool Windows > Course TA**. Compatibility is deliberately limited to IDEA build `261.*`, the target used for this release.

## Use the assistant

1. Click **Import / Rebuild** and select a PDF, TXT or Markdown file, or the included `course-materials` folder. Selecting a folder imports supported files recursively; selecting a file imports only that file. Each successful import replaces the previous course index. For all five lectures, select `course-materials`. Keep assignment instructions and exam files outside this folder.
2. Click **Preview retrieval** after typing a question to inspect actual excerpts without sending data to a provider. This is a diagnostic mode, not an LLM answer.
3. Open **Settings** in the tool window. Enter the full endpoint ending in `/chat/completions`, a model supported by your provider, and an API key. The initial endpoint is `https://api.deepseek.com/chat/completions`; change the endpoint/model together for another provider. Local compatible servers may use HTTP on localhost.
4. Click **Ask TA**. This sends the question, attached selection and up to four retrieved excerpts to the chosen provider. The rest of the project is not transmitted.
5. For code questions, select code in the editor, right-click **Ask Course TA About Selected Code**, enter your question, then review the attachment and click **Ask TA**. The action does not modify code.
6. Use **Clear code** to remove an attachment, **Cancel** to stop the current request and **Clear chat** to remove visible conversation. Each request is independent; previous turns are not sent automatically.

Try `What is polymorphism?`, `How do checked exceptions work?`, `What is retrieval augmented generation?`, or `什么是多态`. An unrelated query such as `astronomy galaxy telescope` exercises the no-evidence branch.

## F1 actions

Configure your model in **Course TA > Course Q&A > Settings**; all F1 actions reuse these settings and PasswordSafe credentials. They do not require a course index.

**JUnit:** Select a class or method in a `.java` editor, right-click **Generate JUnit Tests with Course TA**, enter a scenario such as “when the ID is null or blank, readUser must throw IOException”, and click **Generate**. The current unsaved file is included to supply the package, imports and declaring class. Review/edit the draft in **F1 Actions**, then **Copy result** or **Save test as...** into your project's test source directory. Existing files are never overwritten. Tests target JUnit Jupiter 5; add that dependency if needed, then compile and run the generated class yourself. The plugin does not automatically execute generated code.

**Commit:** Stage the intended changes with your usual Git workflow. In **F1 Actions**, choose a directory within the intended repository, click **Preview staged diff**, then **Generate commit message**. Alternatively use **Tools > Generate Commit Message with Course TA**. The generator re-reads the complete current staged diff, displays the exact snapshot used, and returns an editable Conventional Commit draft. Copy it into your Git commit UI. It never stages, commits or pushes. For multiple repositories, select one repository at a time; all staged files within that repository are included regardless of the selected subdirectory.

Git must be available as `git` on the IDE process's PATH. No Git plugin configuration is required. Requests are bounded to 6,000 scenario characters, 12,000 selected-code characters, a 60,000-character current Java file, and a 120,000-character staged diff. Oversize input is rejected as a whole, never silently truncated. Git commands time out after 30 seconds; model requests use the existing HTTP client timeouts. Binary diffs are included but semantic content may not be inferable from their metadata. Model output checks cover structure only; compilation and correctness need review.

**Comments (creative action):** Select Java code and right-click **Generate Comments with Course TA**, or click **Comments from selection** in F1 Actions. Choose Chinese (default) or English and click Generate. The selection expands to complete physical lines to avoid splitting Java tokens. Review the read-only commented-code preview, then **Copy result** or **Apply comments**. Applying uses a named IntelliJ write command and can be undone in the source editor. If any part of the source changed after generation, regenerate before applying.

The model returns JSON insertions (relative line, block/Javadoc kind, comment text), not rewritten code. The plugin validates locations against the full file's lexical state and constructs the result using original source text. It rejects insertions inside strings, text blocks or existing block comments, comment terminators and Java Unicode escapes. Files containing Java Unicode escape sequences are conservatively rejected by this action; ordinary Chinese characters are supported. No source change occurs during generation. Comments are limited to 40 insertions of 1,500 characters each. Comment accuracy still needs review.

The complete F1 implementation and demo guide is in `output/documents/F1_Implementation_Guide.md`.

## Project layout

| Path | Purpose |
|---|---|
| `core/src/main/java` | Ingestion, BM25, persistence, RAG, HTTP, F1 prompts, staged Git diff collection and validated comment insertions |
| `src/main/java` | IDEA services, Q&A and F1 tabs, settings and editor/menu actions |
| `src/main/resources/META-INF/plugin.xml` | Plugin registration |
| `core/src/test/java` | Automated tests and full-corpus audit |
| `course-materials` | Original lecture PDFs used for validation |
| `samples/Demo.java` | Selected-code demo |
| `tools` | Build and document generation helpers |
| `output` | Report and delivery package |

The index is stored under the opened project's `.idea/course-ta/index.json`. Rebuild after adding, changing or deleting materials. Normal settings are in project workspace state; credentials are in PasswordSafe. Conversation is in memory only. Exclude `.idea` and API keys from submissions.

## Limits and validation

PDF parsing supports extractable text, not OCR. PDF page numbers are physical 1-based page positions, which may differ from printed slide labels. TXT/Markdown citations use inclusive line ranges. Corrupt files and empty PDF pages produce visible warnings. Input file size is capped at 50 MiB.

BM25 is lexical retrieval, with simple plural normalization, camel-case splitting and a small Chinese topic alias dictionary. It is not a multilingual embedding model; paraphrases can miss relevant slides. Citation validation verifies source IDs and refuses missing/invalid references, but does not prove that every generated claim follows from the cited excerpt. Review answers against the displayed evidence.

Unit and local HTTP contract tests do not establish the quality of a live provider. A real model/API key must be configured for the final live demonstration. The report records the checks actually completed.

## Group B completion

All three F1 actions are implemented. The 17-slide `output/documents/GroupB_F0_F1_Presentation.pptx` and matching PDF include all F1 features, workflows, implementation and validation results. Before final Group B submission, validate with your live model, update the separate F0 report, and fill in team names, matriculation numbers and contact details. The F1 Markdown guide documents this release and its validation limits.

## References

- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [Tool windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [PasswordSafe](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html)
- [PDFBox 3 migration guide](https://pdfbox.apache.org/3.0/migration.html)
- [DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)
- [Git staged diff](https://git-scm.com/docs/git-diff)
- [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

Lecture content remains attributed to Dr. Fan Hongfei and is supplied here for this coursework.
