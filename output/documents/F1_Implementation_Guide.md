# Group B F1 implementation - release 0.3.0

This release implements all three F1 actions on top of F0. The team's creative action is automatic comment generation for selected Java code. Names, matriculation numbers and contact details remain for the group to fill in. The 17-slide GroupB_F0_F1_Presentation.pptx and matching PDF cover F0 and all F1 actions; the separate PDF report still needs to be updated from its F0 baseline before submission.

## 1. Scenario-based JUnit generation

1. Configure the full Chat Completions endpoint, model and key in Course TA > Course Q&A > Settings.
2. Open a Java file and select a class or method.
3. Right-click **Generate JUnit Tests with Course TA**. A multiline input asks for a natural-language scenario before any model request.
4. For `samples/Demo.java`, try: `Test that readUser throws IOException when id is null, empty, or whitespace-only, and returns User: 42 for id 42.`
5. Click Generate. The selected code, filename, line range, scenario and complete current Java file (including unsaved edits) go to the configured model. Other project files and the course index are not included.
6. Review the editable Java draft in the F1 Actions tab. Copy it or choose **Save test as...** and select a new `.java` path in your test source folder. Use the generated class name and package. Existing files are not overwritten.
7. Ensure JUnit Jupiter 5 is a test dependency in the target project, then compile and run the draft. Generation does not execute the tests. Assertions and setup must be checked against the intended behavior.

The prompt asks for one complete JUnit 5 class, meaningful assertions tied to the scenario, correct source package/signatures, and explicit assumptions in comments for missing context. Responses are checked for basic class/JUnit structure. These checks do not prove compilation or scenario coverage.

## 2. Staged Git commit messages

1. Stage the intended files or hunks using your usual Git workflow.
2. In Course TA > F1 Actions, check the repository directory. Use Choose repository for another repository or a project whose root is not inside Git.
3. **Preview staged diff** displays the entire staged snapshot locally without a model call.
4. **Generate commit message** reads the current snapshot again and sends it to the configured model. The exact snapshot used is displayed with the result. You can also use Tools > Generate Commit Message with Course TA.
5. Review/edit the Conventional Commit message, then Copy result into your usual Git commit UI. If the staged files change, regenerate the draft.

Collection resolves the repository root, then executes `git diff --cached --no-ext-diff --no-textconv --no-color --binary --full-index --no-renames --ignore-submodules=none --submodule=short --`. All staged files in that repository are included, even when the configured directory is a subfolder. Unstaged edits and untracked files are excluded. Initial commits without HEAD are supported. Renames are represented by old/new paths as delete/add changes. Binary patch data is included, but the model is instructed not to infer unsupported binary semantics.

The plugin never stages, commits, pushes, or changes Git configuration. Git must be on the IDE's PATH. Multiple repositories are handled one at a time by selecting the intended repository.

## 3. Creative action: selected-code comments

1. Select a method, class or code segment in a Java editor. Right-click **Generate Comments with Course TA** or click **Comments from selection** in F1 Actions.
2. Choose Chinese (the default) or English, then Generate. The covered physical lines and the full current Java document, including unsaved edits, are sent to the configured model.
3. Review the commented code in the result pane. The preview is read-only to retain the validated insertion plan. **Copy result** copies the annotated selection, not the entire file.
4. Click **Apply comments** to replace the original covered lines with their commented version. All original characters within and outside those lines are retained; only comment blocks are inserted.
5. Use Undo in the source editor to revert the named `Apply Course TA Comments` write command. If any part of the source changed after generation, the plugin refuses to apply the stale draft and asks you to regenerate.

The model returns JSON such as `{"comments":[{"line":1,"kind":"javadoc","text":"Returns twice the input.\n@param n input value\n@return twice n"}]}`. Line numbers are relative to the selection. Allowed kinds are `javadoc` and `block`; the plugin supplies delimiters and indentation. Existing code is never taken from the model response.

Validation accepts at most 40 unique insertion locations and 1,500 characters per comment. A scan of the complete document tracks code, line/block comments, strings, characters and text blocks. Insertions are allowed only at code-state line starts; blank lines and existing comment-start lines are rejected. Comment terminators, Java Unicode escapes and invalid control characters are rejected. Files that already contain Java Unicode escape sequences are conservatively unsupported by this action; ordinary Chinese text is supported. The IDE supplies normalized LF document text. Invalid JSON, rewritten-code output and out-of-range locations are rejected without changing the source.

The model is asked to explain purpose, parameters, return values, exceptions and non-obvious logic. These descriptions still require human review; insertion validation does not prove that the natural-language explanation is correct.

## 4. Architecture

| Component | Responsibility |
|---|---|
| GenerateJUnitAction | Captures selected Java code and the current document snapshot; opens scenario input. |
| GenerateCommitAction | Tools-menu entry to the commit workflow. |
| GenerateCommentsAction | Captures the complete selected lines and immutable source snapshot for the comment workflow. |
| SmartActionPanel | Scenario/language dialogs, repository selector, previews, background tasks, cancellation, copy, test export and undoable comment application. |
| SmartActions | Builds separate test/commit prompts, invokes LlmClient, validates basic output structure. |
| CommentGenerator | Builds the comment prompt, validates JSON insertions and lexical positions, constructs the draft and checks source freshness. |
| GitStagedChanges | Resolves the Git root and reads all staged changes using bounded read-only processes. |
| TaToolWindowFactory | Registers Course Q&A and F1 Actions tabs; existing Q&A action selects its own tab. |
| TaProjectService / LlmClient | Reused provider settings, PasswordSafe credentials, HTTP requests and error handling. |

These F1 actions use a fixed Java-controlled flow. They do not use BM25 or model-directed tool calling. Source and diff text are framed as data in the prompts. The IDE thread handles UI work, while Git, HTTP and file export run in the background. Cancelled/obsolete responses cannot update the panel.

## 5. Limits and failure behavior

- Scenario: 6,000 characters; selected Java code: 12,000; complete current file: 60,000; staged diff: 120,000.
- A request that exceeds a context limit is rejected without sending a partial context. Split large staged batches deliberately, or choose a smaller source file.
- Git process timeout: 30 seconds. HTTP connect/request limits: 15/90 seconds.
- Empty staging, invalid repository, missing Git, HTTP failures and malformed model output show actionable messages.
- Export uses a new-file-only write. The user selects the destination; no model-generated filesystem path is executed.
- No project-wide Java dependency analysis, automatic compilation, automatic execution or semantic proof of generated output is performed.

## 6. Validation

53 automated tests passed: 21 F0 tests, 10 test/commit prompt and HTTP tests, 7 temporary Git repository tests, and 15 comment-generation tests. Comment tests cover source preservation, relative line offsets, stale-source rejection, existing comments, strings/text blocks, invalid insertion data, malicious delimiters/Unicode escapes, context limits and a local HTTP contract. A Java compiler test compiles both the original and annotated sample, then checks matching behavior for positive and negative inputs. These are controlled fixtures, not live model output. Git coverage includes partially staged files, unstaged and untracked exclusions, nested directories, Unicode/space paths, initial commits, deletion/rename/binary changes, empty staging, oversize diffs and missing Git.

The plugin builds successfully for IDEA 2026.1.x. Interactive clicks and IDE undo have not been verified for 0.3.0; the Computer Use native pipe was unavailable during earlier attempts. A live provider run has not been performed because no provider credential was supplied for testing. Configure your own service and perform the demo steps above before presentation.

The English F0 report remains a baseline document. The updated English presentation covers all three F1 features, their workflows, implementation and validation limits. Add actual demo evidence and team details, and update the separate PDF report before submission.

## References

- Assignment A, F1 on page 2 (user-supplied course assignment).
- Git diff documentation: https://git-scm.com/docs/git-diff
- Conventional Commits: https://www.conventionalcommits.org/en/v1.0.0/
