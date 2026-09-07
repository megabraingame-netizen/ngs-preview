import fs from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';

const dataDir = path.resolve('sales-agent/data');
const file = path.join(dataDir, 'db.json');

function ensure() {
  fs.mkdirSync(dataDir, { recursive: true });
  if (!fs.existsSync(file)) {
    fs.writeFileSync(file, JSON.stringify({ leads: [], calls: [], settings: { dailyLimit: 80 } }, null, 2), 'utf8');
  }
}

function read() {
  ensure();
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function write(db) {
  ensure();
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(db, null, 2), 'utf8');
  fs.renameSync(tmp, file);
}

export function listLeads() {
  return read().leads.sort((a,b) => (b.score ?? 0) - (a.score ?? 0));
}

export function addLead(input) {
  const db = read();
  const lead = {
    id: randomUUID(),
    company: input.company || '',
    contact: input.contact || '',
    phone: input.phone || '',
    category: input.category || 'ساختمان',
    city: input.city || 'اصفهان',
    source: input.source || 'manual',
    stage: input.stage || 'new',
    score: Number(input.score || 50),
    nextCallAt: input.nextCallAt || new Date().toISOString(),
    notes: input.notes || '',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  db.leads.push(lead);
  write(db);
  return lead;
}

export function patchLead(id, patch) {
  const db = read();
  const lead = db.leads.find(x => x.id === id);
  if (!lead) return null;
  Object.assign(lead, patch, { updatedAt: new Date().toISOString() });
  write(db);
  return lead;
}

export function getLead(id) {
  return read().leads.find(x => x.id === id) || null;
}

export function addCall(call) {
  const db = read();
  const item = { id: randomUUID(), createdAt: new Date().toISOString(), ...call };
  db.calls.push(item);
  write(db);
  return item;
}

export function listCalls() {
  return read().calls.sort((a,b) => b.createdAt.localeCompare(a.createdAt));
}

export function dashboard() {
  const db = read();
  const now = Date.now();
  const due = db.leads.filter(x => new Date(x.nextCallAt).getTime() <= now);
  return {
    totalLeads: db.leads.length,
    dueToday: due.length,
    hot: db.leads.filter(x => (x.score ?? 0) >= 80).length,
    warm: db.leads.filter(x => (x.score ?? 0) >= 60 && (x.score ?? 0) < 80).length,
    calls: db.calls.length,
    dailyLimit: db.settings.dailyLimit
  };
}

export function nextQueue(limit = 20) {
  const now = Date.now();
  return listLeads()
    .filter(x => ['new','followup','warm','hot'].includes(x.stage) && new Date(x.nextCallAt).getTime() <= now)
    .slice(0, limit);
}
