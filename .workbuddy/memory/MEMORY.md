# GithubSrcReader 项目约定

- 包名 `com.error1015.githubsrcreader`；UI 全 Compose，不做 XML 布局。
- 设计风格：Miuix（澎湃/HyperOS），主入口底栏必须是**悬浮**样式（自绘 squircle 栏：外层圆角 26dp + 6dp 内边距 + 内层 20dp，同心对齐），不要改回普通 `NavigationBar`。底栏现在是四个分区（首页/项目/浏览/设置），**只给选中项显示文字**（否则英文标签超出屏宽）。
- Miuix 0.9.4 没有对话框/弹窗组件：菜单与输入框统一用 `ui/components/Popups.kt`（`PopupMenu` / `PopupPrompt` / `PopupConfirm`），**贴着触发点弹**，不用居中大对话框。触发控件挂 `Modifier.anchorTo(rememberAnchorHandle())` 记录窗口坐标；列表里必须用 `key(item.id)` 包住每一项，否则同一个 call site 的 `remember` 槽位被所有行共用，浮层会跑到别的行。
- 图标集只有 21 个，缺的图标自绘到 `ui/components/AppIcons.kt`，不要引入整包 material-icons。
- `data/model/Project` 一条记录两用：打开只写历史（`saved=false`，首页最近打开），显式「加入项目库」才 `saved=true`（项目页、可分组）。逻辑都在 `ProjectLibrary`（纯 Kotlin）：星标置顶、分组切分、`upsert` 保留 saved/分组/星标、`trimmed` 只裁历史（已入库永不删）。不要在 UI 层自行过滤或排序。
- 依赖版本集中在 `gradle/libs.versions.toml`；AGP 9 内置 Kotlin，升级 Kotlin 只能改根 `build.gradle.kts` 的 buildscript classpath，不要 apply `org.jetbrains.kotlin.android`。
- 数据层统一实现 `CodeSource` 接口（`list(path)` / `readText(path)`），新增来源（Gitee、GitLab、压缩包等）沿用该抽象。
- 敏感数据（GitHub Token 等）必须经 `data/security/SecretVault`（Keystore AES-GCM）密文落盘，禁止写明文；UI 只显示「是否已配置」，不回显令牌。
- 源码着色走 `CodeTokenizer`（纯 Kotlin，可单测），不要在 Composable 里做正则扫描。
- 保存文件到公共目录统一走 `data/download/FileDownloader`（MediaStore.Downloads），不要直接写 `/storage/emulated/0/Download`；图片等二进制交给系统查看器时走 `ui/util/ExternalFiles` + FileProvider。保存时的 MIME **必须**由 `ExternalFiles.mimeForSave` 按系统 `MimeTypeMap` 推导（查不到回退 `application/octet-stream`），直接给 `text/plain` 会让 MediaProvider 把 `Main.kt` 存成 `Main.kt.txt`。
- 顶栏（`SmallTopAppBar`）的 `subtitle` 是裸 `Text`，没有行数/省略号约束，宽度还被限制在标题区，**长文本不要放 subtitle**（会换行撑高工具栏），改放 `bottomContent` 自己控制单行省略。
- 预览页的缩放比例由 `ViewerScreen` 持有（`remember(state.path)`），`CodeBlock` 是受控组件（`zoom` / `onZoomChange`）：刷新保留缩放，离开或换文件重置。不要把缩放藏回 `CodeBlock` 内部，否则刷新时组合被替换就丢了。
- 横屏 / 分屏走紧凑布局：`LocalConfiguration.screenHeightDp < 480` 时省掉路径行与 `FileMeta`、收紧代码区 padding；`CodeBlock` 的定位 `LaunchedEffect` 不要拿行高当 key（缩放会把视图拽回顶部）。
- 界面文案**不许**在代码里写中文字面量：键在 `i18n/Keys.kt`，各语言值在 `i18n/texts/*Texts.kt`，调用统一用 `Strings.xxx`；新增文案要同时在所有语言表里补值（`StringsTest` 会校验键集合一致）。新增语言 = AppLanguage 枚举 + 新表文件 + `Strings.tables` 登记，调用点不动。
- 应用图标由 `design/icon/gen_icon.py` 生成（`</>` 字形 + 深蓝渐变）；自适应前景是矢量 `drawable/ic_launcher_foreground.xml`，改字形时脚本与矢量两处同步。
- release 签名读根目录 `keystore.properties`（已 gitignore），密钥库 `keystore/githubsrcreader.jks`。
- 提交前跑 `./gradlew :app:assembleDebug :app:testDebugUnitTest`。
