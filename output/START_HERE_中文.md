# Group B：F0 + F1 三项功能交付（0.3.0）

已实现 F0，以及 F1 的场景驱动 JUnit 生成、暂存区 Git 提交信息生成、选中 Java 代码自动生成注释。第三项是本组的 creative action。小组成员、学号和联系方式按要求留空；展示稿已更新为包含全部 F1 的 17 页版本，独立 PDF 报告仍需从 F0 基线更新。

## 文件

- `documents/GroupB_F0_Report.pdf`：10 页英文报告。
- `documents/GroupB_F0_F1_Presentation.pdf`：17 页英文展示稿，第 8-15 页为 F1 功能、操作、示例和实现原理。
- `documents/GroupB_F0_F1_Presentation.pptx`：可编辑展示稿，可填写第一页成员信息；备注中有演示提示。
- 旧名 `GroupB_F0_Presentation.pptx` / `.pdf` 已同步更新为同一份 17 页内容，便于从原路径打开。
- `documents/GroupB_F0_Report_Source.md`：报告文字源稿。
- `plugin/course-ta-0.3.0.zip`：包含 F0 和全部三项 F1 功能的 IDEA 插件安装包，适配 2026.1.x。
- `documents/F1_Implementation_Guide.md`：英文 F1 使用说明、架构、限制和演示步骤。
- `documents/F0_0.1.1_Changes.md`：PDF 导入修复说明；独立 PDF 报告仍保留 0.1.0 阶段内容。
- `evidence/`：课件全量审计和自动化测试记录。
- 完整交付 ZIP 中的 `source/`：项目源码、Gradle Wrapper、五份课程课件和构建脚本。

## 使用

安装 0.3.0 后，Course TA 有 **Course Q&A** 和 **F1 Actions** 两个页签。模型配置在 Course Q&A → Settings，F0 与 F1 共用。

**JUnit：**在 Java 编辑器选中一个类或方法 → 右键 Generate JUnit Tests with Course TA → 输入测试场景 → Generate。可以先用 samples/Demo.java，输入“id 为 null、空字符串或纯空格时抛出 IOException；id 为 42 时返回 User: 42”。生成后在 F1 Actions 审阅、编辑、Copy result 或 Save test as... 另存为新的 .java 文件。请选择目标项目的测试源码目录，并保持类名、文件名、包名一致。目标项目需要 JUnit Jupiter 5 依赖；测试需自行编译运行。发送给模型的是场景、选中代码以及当前 Java 文件全文（含未保存编辑）。

**Git：**先暂存需要提交的文件或代码块 → 在 F1 Actions 确认仓库目录（可用 Choose repository 切换） → Preview staged diff 本地查看全部暂存区差异 → Generate commit message。也可从 Tools 菜单调用。复制结果到自己的 Git 提交界面；暂存区变化后重新生成。多个仓库分别选择。IDE 进程的 PATH 需要能执行 git。

**自动注释（creative action）：**选中 Java 代码 → 右键 Generate Comments with Course TA（或 F1 面板中的 Comments from selection）→ 选择 Chinese / English → Generate。预览中显示带注释的代码，点击 Copy result 复制，或 Apply comments 应用到原文件。应用后可在源码编辑器使用撤销恢复。选区会扩展到覆盖的完整行，避免把注释放进单词中；生成后如果源文件发生变化，会要求重新生成。

模型返回注释插入位置和内容，程序仅插入注释块，保留原代码。注释预览只读；自然语言解释仍需审阅。字符串、文本块和已有块注释内部不能插入。含 Java Unicode 转义序列的文件暂不支持自动注释，普通中文字符不受影响。

插件不会自动执行生成的测试，另存测试时不会覆盖已有文件，也不会自动暂存、提交或推送。只有点击 Apply comments 才会将注释写入原文件的编辑器文档。

**F0 操作：**

1. 在 IntelliJ IDEA 2026.1.x 中打开 Settings → Plugins → 齿轮 → Install Plugin from Disk，选择插件 ZIP，按提示重启。
2. 打开一个本地 Java 项目，在 View → Tool Windows → Course TA 打开侧栏。
3. 点击 Import / Rebuild，可以直接选择一个 PDF、TXT、Markdown 文件，也可以选择源码中的 `course-materials` 文件夹。每次成功导入会用当前选择的资料重建并替换知识库；要同时使用五份课件，请选择整个文件夹。
4. 导入整个课件文件夹或其中的 `2.pdf` 后，输入 `What is polymorphism?`，点击 Preview retrieval；应显示 `2.pdf | p. 22` 的课程片段。
5. 点击 Settings，填写服务商的完整 Chat Completions 地址、模型和 API Key，再点 Ask TA 生成回答。默认地址为 DeepSeek；模型名请按服务商账户实际可用模型填写。
6. 选中 Java 代码后，右键 Ask Course TA About Selected Code，填写问题、检查侧栏附加的代码，再点击 Ask TA。

源码构建：在源码目录运行 `.\tools\build.ps1 -Task Build`；复现课件审计运行 `.\tools\build.ps1 -Task Audit`。需要 JDK 21 或更高版本。首次构建需联网下载依赖。

## 已验证与待验证

0.3.0 已通过 53 项自动化测试并成功构建：21 项 F0、17 项前两项 F1 功能测试，以及 15 项注释生成测试。新增验证包括原代码保留、源文件变化检查、字符串/文本块保护、注释内容校验，以及原始/添加注释后的示例均能编译并保持相同行为。全量课件审计记录见 evidence/course-audit.json。旧版 0.1.0 曾被 IDEA 沙盒日志识别；本次未重新进行沙盒启动与界面点击验证。

HTTP 测试使用本地模拟服务，并未调用真实 LLM。电脑操作工具连接失败，完整 IDEA 点击流程也未验证。展示前请配置真实服务并按报告的演示流程检查。报告中的 UI 示例来自实际 Swing 组件的离屏渲染，不是真实 LLM 答复截图。

当前源码、安装包与新版展示稿归档为 GroupB_F0_F1_0.3.0.zip。Group B 最终提交前还需完成真实服务演示，补齐成员信息和真实分工，并把全部 F1 内容合并到独立英文 PDF 报告；展示稿已包含 F1。
