# 余白 1.0.5 · 账目具体时间

- 新增原生时间滚轮：日期点击“确定”后选择小时和分钟，确认后一起回填。
- 时间范围 00:00–23:59，支持 23↔00、59↔00 双向循环、惯性滑动及居中吸附。
- 使用确认的 38sp 数字、68dp 行距及较淡的相邻数字，突出选中时间。
- 编辑已有账目读取原时间，取消选择保留原日期和时间；仅改时间也有未保存提醒。
- 31 项本地测试和 Release Lint 通过，包含循环、触摸、保存、取消、旧账本保留和 JSON / CSV 时间往返。

覆盖更新：沿用原包名和签名，Room schema 仍为 1，复用已有时间字段。直接安装本 APK 更新，避免先卸载或清除应用数据。建议更新前导出一次 JSON 完整备份。

[覆盖更新说明](https://github.com/Cottins-Vanilla/Yubai-Ledger/blob/main/docs/UPGRADING.md) · [原生弹层截图](https://github.com/Cottins-Vanilla/Yubai-Ledger/blob/main/docs/screenshots/native-time-picker.png)

原生截图来自 JVM Android 图形渲染；尚未进行手机 APK 覆盖安装和 144Hz 帧耗时实测。

SHA-256：4a8ce0c3a575ac0090e2455cd1b1686d657bfe342f39f10d6c2bcf3b0180cbc4
