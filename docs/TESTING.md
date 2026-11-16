# 1.0.2 验证记录

日期：2026-11-16。构建环境：Windows、JDK 17、Android SDK 35、AGP 8.9.2、Gradle 8.11.1。

## 自动化结果

**25 项测试通过，0 失败、0 错误。Release Lint：No issues found。**

| 测试组 | 数量 | 内容 |
|---|---:|---|
| LedgerTest | 16 | 整数金额、负期初余额、退款不进入收支、部分/全退、累计限额、日期与关联、编辑排除旧记录、合并去重/冲突、JSON 往返、CSV 中文/逗号/引号/换行/公式转义、损坏格式 |
| DatabaseTest | 3 | Room 原 ID 更新不新增、事务失败后原账目和期初余额保留、提交退款后的 Flow 读取与余额 |
| NativeUiTest | 5 | 退款编辑与关联保留、新建数字键盘与保存、已退款标记、收入/支出统计切换、导入自定义退款分类不变成普通收入 |
| AppSmokeTest | 1 | 实际 MainActivity 启动、60dp 导航及上浮位置、至少 48dp 触控范围、页面切换、UI 录入并通过 Room 保存，Activity 重建后继续读取记录 |

数据库和 UI 测试运行在 Robolectric Android API 28 环境；不是模拟器安装或真机测试。UI 使用原生 Compose 代码，通过 Android 原生图形模式渲染并检查交互。

## 截图

`docs/screenshots/native-*.png` 是上述 Kotlin / Compose 原生代码在 JVM Android 测试环境中的渲染，不是网页截图，也不是手机截图。

- `native-app-home.png`：应用级测试通过 UI 新增 32 元支出后的主页，包含底部导航。
- `native-home.png`、`native-statistics.png`：使用同一组测试账目的独立界面渲染。
- `native-entry.png`、`native-refund.png`：金额输入与已关联退款编辑。

`preview/` 中的旧截图来自网页交互原型，用于保留设计过程，不能作为 Android 运行证据。

## Release 检查

- 签名 Release 构建完成，R8 和资源缩减已开启。
- `apksigner verify` 通过；RSA 3072 位，APK Signature Scheme v2，适用于最低 Android API 26。
- 包名 `com.clottins.dailyledger`，版本 `1.0.2 (3)`，min SDK 26，target SDK 35。
- 不包含联网、通讯录或整个存储空间权限。
- `zipalign -c -P 16 -v 4` 通过。
- Release 不启用 Debug 演示账目入口，新安装不会自动写入示例账目。
- APK 校验值见 Release 附件和 `releases/SHA256SUMS.txt`（本地交付）。

## 未实测范围

本机 Android 35 / Android 9 模拟器在无可用硬件加速时未完成启动，因此没有声称通过模拟器安装测试，也没有修改用户 Windows 功能或要求重启。

尚无 144Hz 手机 GPU 帧耗时、不同品牌系统文件选择器、系统节电及温控策略的真机结果。Android 8.0 至新版系统的完整设备矩阵亦未遍历。APK 已通过构建、签名、格式和上述自动化验证，后续应根据实际安装设备补充验收记录。

## 重现

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
```

所有版本固定，不需要发布私钥即可执行测试。完整签名构建配置见 [BUILD.md](BUILD.md)。
