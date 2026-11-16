# 数据与文件格式 v1

Room 数据库包含三张表：`entries`、`categories`、`preferences`。金额均为整数分，交易日期为本地日历日期，避免因时区切换改变账目归属。

## JSON

```json
{
  "schemaVersion": 1,
  "currency": "CNY",
  "exportedAt": "2026-11-16T00:00:00Z",
  "preferences": {"openingCents": 0, "hideBalance": false},
  "categories": [
    {"type": "expense", "name": "购物消费", "icon": "bag"},
    {"type": "refund", "name": "退款", "icon": "refund"}
  ],
  "entries": [
    {"id": "purchase", "type": "expense", "amountCents": 19900, "category": "购物消费", "date": "2026-11-14", "time": "15:20", "note": "耳机", "linkedExpenseId": null},
    {"id": "return", "type": "refund", "amountCents": 5000, "category": "退款", "date": "2026-11-16", "time": "09:00", "note": "部分退回", "linkedExpenseId": "purchase"}
  ]
}
```

允许类型为 `income`、`expense`、`refund`；关联仅用于退款。账目 ID 长度 1–100，仅字母、数字、`-`、`_`。日期和时间使用 ISO 本地格式，备注最多 1000 字符。未知图标使用默认图标；未知有效分类可由导入创建。

同一 ID、内容完全相同视为重复，合并跳过；同一 ID 内容不同则阻止导入。缺少原支出、退款总额超限或日期早于原支出也会阻止写入，不产生半份账本。

## CSV

UTF-8 BOM，CRLF 换行，字段全部加双引号；内部双引号翻倍，保留逗号与字段内换行。

```text
id,date,type,category,amount,note,linkedExpenseId,time,textEncoding
```

`amount` 是正的人民币元十进制，最多两位小数，类型决定资金方向。`id` 可空，导入时生成新 ID；这类记录不会凭备注相同而自动删除，预览提示疑似重复。

最低必要列是 `date,type,category,amount,note`。`linkedExpenseId` 为空时退款不关联；`time` 缺失时使用 `00:00`。为实现本应用导出再导入的无损往返，应保留全部列。

本应用导出把分类、备注中以 `= + - @ Tab CR LF` 或单引号开头的文本添加一个前导单引号，并写 `textEncoding=apostrophe-v1`，防止在表格中执行公式。只有携带该标记的行才按此约定去除一层转义，外部 CSV 的原始单引号不会擅自移除。

日期范围导出可包含退款而不含原支出，恢复时需同时提供原支出或先明确调整关联。不要直接删除关联列后误认为仍是完整备份；换机优先用 JSON。

CSV 导入仅合并账目，不覆盖期初余额与余额隐藏设置。JSON 完整恢复才可替换这些设置，并需二次确认。
