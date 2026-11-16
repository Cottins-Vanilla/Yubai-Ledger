// Shared accounting rules for the UI prototype; all monetary values are integer cents.
window.LedgerRules=(()=>{
function validAmount(cents){return Number.isSafeInteger(cents)&&cents>0&&cents<=99999999999}
function add(a,b){const result=a+b;if(!Number.isSafeInteger(result))throw Error('金额汇总超出范围');return result}
function totals(records,openingCents=0){if(!Number.isSafeInteger(openingCents))throw Error('无效期初余额');let income=0,expense=0,refund=0;for(const r of records){if(!validAmount(r.cents))throw Error('无效金额');if(r.type==='income')income=add(income,r.cents);else if(r.type==='expense')expense=add(expense,r.cents);else if(r.type==='refund')refund=add(refund,r.cents);else throw Error('未知账目类型')}return {income,expense,refund,balance:add(add(add(openingCents,income),-expense),refund),netFlow:income-expense}}
function refundedCents(records,expenseId,excludeId){return records.filter(r=>r.type==='refund'&&r.linkedExpenseId===expenseId&&r.id!==excludeId).reduce((s,r)=>s+r.cents,0)}
function status(records,expenseId){const original=records.find(r=>r.id===expenseId&&r.type==='expense');if(!original)return 'none';const returned=refundedCents(records,expenseId);return returned<=0?'none':returned<original.cents?'partial':'full'}
function validateRefund(candidate,records){if(!validAmount(candidate.cents))return '请输入有效退款金额';if(!candidate.linkedExpenseId)return null;const original=records.find(r=>r.id===candidate.linkedExpenseId&&r.type==='expense');if(!original)return '请选择一笔支出账单';if(candidate.date<original.date)return '退款日期不能早于原账单日期';const remaining=original.cents-refundedCents(records,original.id,candidate.id);return candidate.cents>remaining?'退款金额超过这笔账单的剩余可退金额':null}
return {totals,refundedCents,status,validateRefund};
})();
