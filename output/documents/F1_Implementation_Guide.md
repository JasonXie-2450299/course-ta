# Group B F1 implementation - release 0.2.0

This release adds the first two F1 actions to the existing F0 plugin. The creative action is deliberately not implemented, as requested. Names, matriculation numbers and contact details remain for the group to fill in. This is a partial F1 milestone, not the final complete Group B submission.

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

## 3. Architecture

| Component | Responsibility |
|---|---|
| GenerateJUnitAction | Captures selected Java code and the current document snapshot; opens scenario input. |
| GenerateCommitAction | Tools-menu entry to the commit workflow. |
| SmartActionPanel | Scenario dialog, repository selector, staged preview, background tasks, cancellation, editable output, copy and test export. |
| SmartActions | Builds separate test/commit prompts, invokes LlmClient, validates basic output structure. |
| GitStagedChanges | Resolves the Git root and reads all staged changes using bounded read-only processes. |
| TaToolWindowFactory | Registers Course Q&A and F1 Actions tabs; existing Q&A action selects its own tab. |
| TaProjectService / LlmClient | Reused provider settings, PasswordSafe credentials, HTTP requests and error handling. |

These F1 actions use a fixed Java-controlled flow. They do not use BM25 or model-directed tool calling. Source and diff text are framed as data in the prompts. The IDE thread handles UI work, while Git, HTTP and file export run in the background. Cancelled/obsolete responses cannot update the panel.

## 4. Limits and failure behavior

- Scenario: 6,000 characters; selected Java code: 12,000; complete current file: 60,000; staged diff: 120,000.
- A request that exceeds a context limit is rejected without sending a partial context. Split large staged batches deliberately, or choose a smaller source file.
- Git process timeout: 30 seconds. HTTP connect/request limits: 15/90 seconds.
- Empty staging, invalid repository, missing Git, HTTP failures and malformed model output show actionable messages.
- Export uses a new-file-only write. The user selects the destination; no model-generated filesystem path is executed.
- No project-wide Java dependency analysis, automatic compilation, automatic execution or semantic proof of generated output is performed.

## 5. Validation

38 automated tests passed: the existing 21 F0 tests, 10 F1 prompt/output/HTTP tests, and 7 tests against temporary real Git repositories. Git coverage includes partially staged files, unstaged and untracked exclusions, nested directories, Unicode and spaces in paths, initial commits, deletion/rename/binary changes, empty staging, oversize diffs, and missing Git. The HTTP contract test uses a local fixture server for both actions; its responses are controlled fixtures, not live model output.

The plugin builds successfully for IDEA 2026.1.x. Interactive IDE clicks have not been verified because the Windows Computer Use native pipe is unavailable. A live provider run has not been performed because no provider credential was supplied for testing. Configure your own service and perform the demo steps above before presentation.

The existing English F0 report and presentation PDFs remain baseline documents. This guide records the new features; update the final PDF report and slides after the deferred creative action and team details are settled.

## References

- Assignment A, F1 on page 2 (user-supplied course assignment).
- Git diff documentation: https://git-scm.com/docs/git-diff
- Conventional Commits: https://www.conventionalcommits.org/en/v1.0.0/
