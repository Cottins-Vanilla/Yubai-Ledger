# 余白 1.0.6 · 日期面板与关闭按钮修复

- 编辑账目的日期选择改为与 HTML 预览相符的浅色底部日历，去掉系统居中弹窗和大绿色日期头。
- 日历周日开头，支持月份切换、跨年、闰年；日期确认后继续选择具体时间。
- 修复日期和时间面板 X 按钮顶部裁切，使用 48dp 点击区域和独立的 40dp 圆形背景。
- rc2 修复代码已获用户真机验证通过；正式版修复代码完全相同，仅调整版本号重新打包。

35 项本地自动化测试与 Release Lint 通过，包含高密度关闭按钮圆弧检查、日期到时间流程、取消回滚和账目保留测试。

沿用原包名、签名、数据库和时间字段，可直接覆盖旧版及 rc1/rc2 更新。不要在未备份时卸载或清除应用数据。

[日期面板预览](https://github.com/Cottins-Vanilla/Yubai-Ledger/blob/main/docs/screenshots/native-date-sheet.png) · [验证记录](https://github.com/Cottins-Vanilla/Yubai-Ledger/blob/main/docs/TESTING.md) · [更新说明](https://github.com/Cottins-Vanilla/Yubai-Ledger/blob/main/docs/UPGRADING.md)

SHA-256：77e8a0d0f8e992c24bf964fd30c86faeef1d16621444bd759b7b10c04f38ad0f
