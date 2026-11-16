// UI prototype fixtures only. These records are never written to a real ledger.
window.demoLedgerRecords={
  lunch:{id:'lunch',type:'expense',cents:3200,category:'餐饮美食',date:'2026-11-16',time:'12:30',note:'午餐',icon:'bowl'},
  metro:{id:'metro',type:'expense',cents:600,category:'交通出行',date:'2026-11-16',time:'09:15',note:'地铁通勤',icon:'train'},
  coffee:{id:'coffee',type:'expense',cents:3050,category:'餐饮美食',date:'2026-11-16',time:'08:40',note:'给自己一杯咖啡',icon:'cup'},
  shopping:{id:'shopping',type:'expense',cents:19900,category:'购物消费',date:'2026-11-14',time:'15:20',note:'降噪耳机',icon:'wallet'},
  salary:{id:'salary',type:'income',cents:1500000,category:'工资收入',date:'2026-11-15',time:'10:00',note:'十一月工资',icon:'wallet'}
};

window.demoRefundRecord={id:'refund-shopping',type:'refund',cents:19900,category:'退款',date:'2026-11-16',time:'15:10',note:'耳机退货',linkedExpenseId:'shopping',icon:'wallet'};
