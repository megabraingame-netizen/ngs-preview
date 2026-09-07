const pending = new Map();

function now(){ return new Date().toISOString(); }

export function createHandoff({ lead, callId, reason, transcriptSnippet }) {
  const item = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2,8)}`,
    leadId: lead.id,
    company: lead.company || '',
    contact: lead.contact || '',
    customerPhone: lead.phone || '',
    callId: callId || '',
    reason: reason || 'مشتری داغ / درخواست صحبت با مدیر',
    transcriptSnippet: transcriptSnippet || '',
    status: 'pending',
    createdAt: now(),
    updatedAt: now()
  };
  pending.set(item.id, item);
  return item;
}

export function listHandoffs(){
  return [...pending.values()].sort((a,b)=>b.createdAt.localeCompare(a.createdAt));
}

export function getHandoff(id){ return pending.get(id) || null; }

export function patchHandoff(id, patch){
  const item = pending.get(id);
  if (!item) return null;
  Object.assign(item, patch, { updatedAt: now() });
  return item;
}
