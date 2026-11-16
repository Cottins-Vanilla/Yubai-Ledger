# 第三方组件

本项目源码采用 MIT，第三方组件保留各自许可。

| 组件 | 用途 | 许可 / 来源 |
|---|---|---|
| Kotlin | 语言及协程生态 | [Apache-2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) |
| AndroidX Compose / Material / Room / Lifecycle / Activity | 界面、图标、本地数据库、生命周期 | [AndroidX Apache-2.0](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt) |
| KSP | 编译期 Room 代码生成 | [Apache-2.0](https://github.com/google/ksp/blob/main/LICENSE) |
| Gradle | 构建与 Wrapper | [Apache-2.0](https://github.com/gradle/gradle/blob/master/LICENSE) |
| JUnit / Robolectric | 测试，不打包入 Release | [JUnit](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt)、[Robolectric](https://github.com/robolectric/robolectric/blob/master/LICENSE) |

应用使用 Android 系统的 `org.json`；独立 `org.json` 依赖仅用于本机 JVM 测试。
