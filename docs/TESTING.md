# 1.0.7 验证记录

日期：2026-11-17。构建环境：Windows、JDK 17、Android SDK 35、AGP 8.9.2、Gradle 8.11.1。

## 自动化结果

**37 项测试通过，0 失败、0 错误。Release Lint：No issues found。**

| 测试组 | 数量 | 内容 |
|---|---:|---|
| LedgerTest | 16 | 整数金额、负期初余额、退款不进入收支、部分/全退、累计限额、日期与关联、编辑排除旧记录、合并去重/冲突、JSON 往返、CSV 中文/逗号/引号/换行/公式转义、损坏格式 |
| DatabaseTest | 3 | Room 原 ID 更新不新增、事务失败后原账目和期初余额保留、提交退款后的 Flow 读取与余额 |
| NativeUiTest | 5 | 退款编辑与关联保留、新建数字键盘与保存、已退款标记、收入/支出统计切换、导入自定义退款分类不变成普通收入 |
| LauncherBrandingTest | 1 | 从 Manifest 读取“余白”显示名、保持原包名、自适应图标加载和原生图形渲染 |
| DatePickerTest | 3 | 周日开始、闰年及六行月份、底部日历布局、跨年切换、取消保留原账目 |
| SheetCloseButtonTest | 1 | 高密度日期与时间面板的关闭按钮完整圆弧、顶部与底部留白、48dp 触控范围 |
| WheelGestureTest | 2 | 空白和单位区域拖动中/松手后面板不移动；两列行间空白仍能调整时间 |
| TimePickerTest | 4 | 日期确认后打开时间、回填保存原 ID、双向循环、触摸滚动、取消回滚、仅改时间的未保存提示 |
| TimePersistenceTest | 1 | 文件数据库关闭重开后保留旧记录，修改时间不丢退款关联/分类/设置，JSON 和 CSV 时间往返 |
| AppSmokeTest | 1 | 实际 MainActivity 启动、60dp 导航及上浮位置、至少 48dp 触控范围、页面切换、UI 录入并通过 Room 保存，Activity 重建后继续读取记录 |

数据库和 UI 测试运行在 Robolectric Android API 28 环境；不是模拟器安装或真机测试。UI 使用原生 Compose 代码，通过 Android 原生图形模式渲染并检查交互。

## 手势问题修复证据

在旧代码加上同一回归测试后，`blankBesideColumnsDoesNotPullDownSheet` 失败：拖动中面板顶部从 499px 变成 563px。应用修复后两项手势测试通过，原有 35 项测试继续通过。测试来自 JVM Android 环境，本版手势修复尚无用户真机验收结果。

## 1.0.6 用户真机反馈

用户在 2026-11-17 对 rc2 修复反馈“通过”，随后授权提交发布。验收针对日期面板外观和关闭按钮显示；未提供设备型号、Android 版本、144Hz 帧耗时或完整账本核对记录。正式版与 rc2 修复代码一致，仅调整版本号与签名打包。

## 截图

`docs/screenshots/native-*.png` 是上述 Kotlin / Compose 原生代码在 JVM Android 测试环境中的渲染，不是网页截图，也不是手机截图。

- `native-launcher-icon.png`：通过应用 Manifest 加载的自适应图标；`icon-b-preview.png` 为该资源渲染与“余白”名称的排版预览，不是真机桌面截图。
- `native-app-home.png`：应用级测试通过 UI 新增 32 元支出后的主页，包含底部导航。
- `native-home.png`、`native-statistics.png`：使用同一组测试账目的独立界面渲染。
- `native-date-sheet.png`：匹配 HTML 预览的日期底部面板；`close-button.png` 为高密度原生关闭按钮局部图。
- `native-time-picker.png`：日期确认后、初始时间 08:45 的原生时间选择弹层。
- `native-entry.png`、`native-refund.png`：金额输入与已关联退款编辑。

`preview/` 中的旧截图来自网页交互原型，用于保留设计过程，不能作为 Android 运行证据。

## Release 检查

- 签名 Release 构建完成，R8 和资源缩减已开启。
- `apksigner verify` 通过；RSA 3072 位，APK Signature Scheme v2，适用于最低 Android API 26。
- 包名 `com.clottins.dailyledger`，版本 `1.0.7 (10)`，min SDK 26，target SDK 35。
- 应用显示名“余白”，采用 B「轻盈钱包」；包含 API 26 自适应和 API 33 单色主题资源。
- 不包含联网、通讯录或整个存储空间权限。
- `zipalign -c -P 16 -v 4` 通过。
- Release 不启用 Debug 演示账目入口，新安装不会自动写入示例账目。
- 数据库 schema 与 1.0.4 相同；本次不执行数据迁移，兼容性与局限见 [覆盖更新说明](UPGRADING.md)。
- APK 校验值见 Release 附件和 `releases/SHA256SUMS.txt`（本地交付）。

## 未实测范围

本机 Android 35 / Android 9 模拟器在无可用硬件加速时未完成启动，因此没有声称通过模拟器安装测试，也没有修改用户 Windows 功能或要求重启。

尚无 144Hz 手机 GPU 帧耗时、不同品牌系统文件选择器、系统节电及温控策略的真机结果。Android 8.0 至新版系统的完整设备矩阵亦未遍历。APK 已通过构建、签名、格式和上述自动化验证，后续应根据实际安装设备补充验收记录。

## 重现

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
```

所有版本固定，不需要发布私钥即可执行测试。完整签名构建配置见 [BUILD.md](BUILD.md)。
