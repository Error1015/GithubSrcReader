# GithubSrcReader 项目约定

- 包名 `com.error1015.githubsrcreader`；UI 全 Compose，不做 XML 布局。
- 设计风格：Miuix（澎湃/HyperOS），主入口底栏必须是**悬浮**样式（自绘 squircle 栏：外层圆角 26dp + 6dp 内边距 + 内层 20dp，同心对齐），不要改回普通 `NavigationBar`。
- 依赖版本集中在 `gradle/libs.versions.toml`；AGP 9 内置 Kotlin，升级 Kotlin 只能改根 `build.gradle.kts` 的 buildscript classpath，不要 apply `org.jetbrains.kotlin.android`。
- 数据层统一实现 `CodeSource` 接口（`list(path)` / `readText(path)`），新增来源（Gitee、GitLab、压缩包等）沿用该抽象。
- 敏感数据（GitHub Token 等）必须经 `data/security/SecretVault`（Keystore AES-GCM）密文落盘，禁止写明文；UI 只显示「是否已配置」，不回显令牌。
- 源码着色走 `CodeTokenizer`（纯 Kotlin，可单测），不要在 Composable 里做正则扫描。
- 保存文件到公共目录统一走 `data/download/FileDownloader`（MediaStore.Downloads），不要直接写 `/storage/emulated/0/Download`；图片等二进制交给系统查看器时走 `ui/util/ExternalFiles` + FileProvider。
- 界面文案**不许**在代码里写中文字面量：键在 `i18n/Keys.kt`，各语言值在 `i18n/texts/*Texts.kt`，调用统一用 `Strings.xxx`；新增文案要同时在所有语言表里补值（`StringsTest` 会校验键集合一致）。新增语言 = AppLanguage 枚举 + 新表文件 + `Strings.tables` 登记，调用点不动。
- 应用图标由 `design/icon/gen_icon.py` 生成（`</>` 字形 + 深蓝渐变）；自适应前景是矢量 `drawable/ic_launcher_foreground.xml`，改字形时脚本与矢量两处同步。
- release 签名读根目录 `keystore.properties`（已 gitignore），密钥库 `keystore/githubsrcreader.jks`。
- 提交前跑 `./gradlew :app:assembleDebug :app:testDebugUnitTest`。
