# Group B：F0 + F1 两项功能交付（0.2.0）

已实现 F0，以及 F1 的场景驱动 JUnit 生成、暂存区 Git 提交信息生成。按本次要求，creative action 暂不实现，因此这还不是完整的 Group B 最终提交。小组成员、学号和联系方式按要求留空。

## 文件

- `documents/GroupB_F0_Report.pdf`：10 页英文报告。
- `documents/GroupB_F0_Presentation.pdf`：9 页英文展示稿。
- `documents/GroupB_F0_Presentation.pptx`：可编辑展示稿，可填写第一页成员信息。
- `documents/GroupB_F0_Report_Source.md`：报告文字源稿。
- `plugin/course-ta-0.2.0.zip`：包含 F0 和两项 F1 功能的 IDEA 插件安装包，适配 2026.1.x。
- `documents/F1_Implementation_Guide.md`：英文 F1 使用说明、架构、限制和演示步骤。
- `documents/F0_0.1.1_Changes.md`：本次修复说明；原 PDF 报告和展示稿保留 0.1.0 阶段内容。
- `evidence/`：课件全量审计和自动化测试记录。
- 完整交付 ZIP 中的 `source/`：项目源码、Gradle Wrapper、五份课程课件和构建脚本。

## 使用

安装 0.2.0 后，Course TA 有 **Course Q&A** 和 **F1 Actions** 两个页签。模型配置在 Course Q&A → Settings，F0 与 F1 共用。

**JUnit：**在 Java 编辑器选中一个类或方法 → 右键 Generate JUnit Tests with Course TA → 输入测试场景 → Generate。可以先用 samples/Demo.java，输入“id 为 null、空字符串或纯空格时抛出 IOException；id 为 42 时返回 User: 42”。生成后在 F1 Actions 审阅、编辑、Copy result 或 Save test as... 另存为新的 .java 文件。请选择目标项目的测试源码目录，并保持类名、文件名、包名一致。目标项目需要 JUnit Jupiter 5 依赖；测试需自行编译运行。发送给模型的是场景、选中代码以及当前 Java 文件全文（含未保存编辑）。

**Git：**先暂存需要提交的文件或代码块 → 在 F1 Actions 确认仓库目录（可用 Choose repository 切换） → Preview staged diff 本地查看全部暂存区差异 → Generate commit message。也可从 Tools 菜单调用。复制结果到自己的 Git 提交界面；暂存区变化后重新生成。多个仓库分别选择。IDE 进程的 PATH 需要能执行 git。

插件不会自动执行生成的测试，不覆盖已有文件，也不会自动暂存、提交或推送。

**F0 操作：**

1. 在 IntelliJ IDEA 2026.1.x 中打开 Settings → Plugins → 齿轮 → Install Plugin from Disk，选择插件 ZIP，按提示重启。
2. 打开一个本地 Java 项目，在 View → Tool Windows → Course TA 打开侧栏。
3. 点击 Import / Rebuild，可以直接选择一个 PDF、TXT、Markdown 文件，也可以选择源码中的 `course-materials` 文件夹。每次成功导入会用当前选择的资料重建并替换知识库；要同时使用五份课件，请选择整个文件夹。
4. 导入整个课件文件夹或其中的 `2.pdf` 后，输入 `What is polymorphism?`，点击 Preview retrieval；应显示 `2.pdf | p. 22` 的课程片段。
5. 点击 Settings，填写服务商的完整 Chat Completions 地址、模型和 API Key，再点 Ask TA 生成回答。默认地址为 DeepSeek；模型名请按服务商账户实际可用模型填写。
6. 选中 Java 代码后，右键 Ask Course TA About Selected Code，填写问题、检查侧栏附加的代码，再点击 Ask TA。

源码构建：在源码目录运行 `.\tools\build.ps1 -Task Build`；复现课件审计运行 `.\tools\build.ps1 -Task Audit`。需要 JDK 21 或更高版本。首次构建需联网下载依赖。

## 已验证与待验证

0.2.0 已通过 38 项自动化测试并成功构建，包括原有 21 项 F0 测试和新增 17 项 F1 测试。验证使用本地模拟 HTTP 服务与真实临时 Git 仓库，覆盖部分暂存、未跟踪排除、首次提交、删除、重命名、二进制差异和过大输入等情况。全量课件审计记录见 evidence/course-audit.json。旧版 0.1.0 曾被 IDEA 沙盒日志识别；本次未重新进行沙盒启动与界面点击验证。

HTTP 测试使用本地模拟服务，并未调用真实 LLM。电脑操作工具连接失败，完整 IDEA 点击流程也未验证。展示前请配置真实服务并按报告的演示流程检查。报告中的 UI 示例来自实际 Swing 组件的离屏渲染，不是真实 LLM 答复截图。

当前完整交付包为 GroupB_F0_F1_Partial_Delivery.zip。Group B 最终提交前还需要完成 deferred creative action，补齐成员信息和真实分工，并把当前 F1 内容合并到最终英文 PDF 报告与展示稿中；现有 F0 PDF 保留为基线文档。
