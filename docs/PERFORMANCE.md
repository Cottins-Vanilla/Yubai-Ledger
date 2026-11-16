# 动画与高刷新率

应用在 Activity 恢复时读取设备支持的显示模式，向系统请求最高不超过 144Hz 的刷新率。使用的是系统刷新率偏好，不强制切换分辨率或绕过省电设置。

Compose 动画和原生横向滚动跟随 Android 帧时钟；没有固定 60fps 的定时器，也不在静止时循环重绘。折线图使用 Canvas，一次绘制整月时间轴，滚动通过原生滚动容器移动；纵轴固定，日期刻度固定间距。饼图排除退款。

Room 读写、文件解析、导出、备份恢复及全量账本校验在后台线程执行。月份汇总在 Default 调度器计算；列表退款标记使用预计算索引，避免每个可见行重复扫描整本账目。

144Hz 单帧预算约 6.94ms，但硬件模式请求不等于每帧都达到目标。温度、省电模式、系统策略和设备支持均可能影响结果。本项目未获得 144Hz 真机实测数据，不承诺稳定 144fps。

## 建议真机验收

1. 安装签名 Release，关闭开发调试覆盖层，选择设备支持的高刷新率。
2. 分别测试空账本、小账本和接近 20,000 笔的账本。
3. 检查明细快速滚动、折线来回拖动、收支切换和弹层开关；同时通过 Perfetto / Android Studio Profiler 记录帧耗时。
4. 在省电、温度升高和后台恢复后复测，确认不会产生持续绘制或异常刷新率切换。

模拟器或 Robolectric 截图只能检查布局和功能，不能作为手机 GPU 帧率测试。

官方参考：[Android frame rate](https://developer.android.com/media/optimize/performance/frame-rate)、[Compose performance](https://developer.android.com/develop/ui/compose/performance)。
