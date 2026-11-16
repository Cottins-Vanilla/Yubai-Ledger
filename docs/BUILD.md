# 构建和发布

## 固定工具版本

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.11.1，包含官方 SHA-256 |
| Kotlin / Compose compiler plugin | 2.1.21 |
| Compose BOM | 2025.05.01 |
| KSP | 2.1.21-2.0.1 |
| Room | 2.7.2 |
| compile / target SDK | 35 / 35 |
| min SDK | 26 |

Android Studio 打开根目录，安装提示的 SDK。命令行可配置 `ANDROID_HOME`，或在未跟踪的 `local.properties` 中写入 `sdk.dir=你的SDK路径`。

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

默认 Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。调试签名由 Android 构建工具自动生成。

## 本项目的本地临时目录

开发期间源代码放在 `E:\Code\ChatGPT\YubaiLedger`。临时工具、SDK、依赖缓存、测试输出及构建产物放在 `E:\Code\ChatGPT\Temp\yubai-android`。其他机器无需使用此绝对路径。

```powershell
$env:GRADLE_USER_HOME='E:\Code\ChatGPT\Temp\yubai-android\gradle-home'
$env:LEDGER_BUILD_DIR='E:\Code\ChatGPT\Temp\yubai-android\outputs'
$env:LEDGER_TEST_DIR='E:\Code\ChatGPT\Temp\yubai-android\test-temp'
$env:LEDGER_SCREENSHOTS='E:\Code\ChatGPT\Temp\yubai-android\native-screenshots'
.\gradlew.bat --project-cache-dir 'E:\Code\ChatGPT\Temp\yubai-android\project-cache' :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

`LEDGER_BUILD_DIR` 设置后，app 的输出在该目录的 `app/` 下；未设置时使用标准 Gradle 输出路径。截图变量未设置时测试仍执行，但不保存截图。首次 Robolectric 测试会下载 Android 测试运行库。

## Release 签名

不要把签名文件或密码提交到 Git。构建脚本通过以下环境变量读取：

```text
LEDGER_KEYSTORE        发布 keystore 的绝对路径
LEDGER_STORE_PASSWORD  keystore 密码
LEDGER_KEY_PASSWORD    dailyledger 别名的私钥密码
```

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

密钥别名为 `dailyledger`。未提供签名环境变量时 Release 是未签名产物，不能直接安装。仓库不附带公开发布用私钥；自行构建应使用自己的密钥，无法覆盖原发布签名的安装包。

本机发布密钥与密码文件保存在项目 `.signing/`（整目录被 Git 忽略），用于后续签名升级。请单独妥善备份它们，不要上传 GitHub、放入 APK 或对外分享。密钥丢失后无法用新密钥覆盖旧应用，只能改包名或备份后重装。

## 发布检查

1. 运行金额、备份、Room 和 UI 测试及 Android Lint。
2. 构建签名 Release，使用 SDK `apksigner verify --verbose` 检查签名。
3. 核对包名、最低 Android 版本、版本号、权限及 APK SHA-256。
4. 只把 APK、校验文件和发布说明作为 Release 附件；密钥、数据库、真实账目和缓存不上传。

GitHub Actions 自动构建 Debug 和测试报告，不持有发布密钥。
