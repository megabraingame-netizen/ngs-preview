import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { URL } from 'node:url';
import { addLead, addCall, dashboard, getLead, listCalls, listLeads, nextQueue, patchLead } from './lib/store.mjs';
import { analyzeConversation, buildOpening } from './lib/agent.mjs';

const PORT = Number(process.env.PORT || 8787);
const root = path.resolve('sales-agent/public');

function send(res, status, body, type='application/json; charset=utf-8') {
  res.writeHead(status, { 'Content-Type': type, 'Access-Control-Allow-Origin': '*' });
  res.end(type.startsWith('application/json') ? JSON.stringify(body) : body);
}

async function json(req) {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  if (!chunks.length) return {};
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

async function startCall(lead) {
  const payload = { leadId: lead.id, phone: lead.phone, opening: buildOpening(lead), callbackUrl: process.env.PUBLIC_BASE_URL ? `${process.env.PUBLIC_BASE_URL}/api/telephony/result` : '/api/telephony/result' };
  if (!process.env.TELEPHONY_WEBHOOK_URL) {
    return addCall({ leadId: lead.id, phone: lead.phone, status: 'simulated', provider: 'mock', opening: payload.opening });
  }
  const r = await fetch(process.env.TELEPHONY_WEBHOOK_URL, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(process.env.TELEPHONY_TOKEN ? { authorization: `Bearer ${process.env.TELEPHONY_TOKEN}` } : {}) },
    body: JSON.stringify(payload)
  });
  const providerResult = await r.text();
  return addCall({ leadId: lead.id, phone: lead.phone, status: r.ok ? 'queued' : 'provider_error', provider: 'webhook', providerResult });
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === 'OPTIONS') return send(res, 204, '');
    const u = new URL(req.url, `http://${req.headers.host}`);

    if (u.pathname === '/api/dashboard' && req.method === 'GET') return send(res, 200, dashboard());
    if (u.pathname === '/api/leads' && req.method === 'GET') return send(res, 200, listLeads());
    if (u.pathname === '/api/leads' && req.method === 'POST') return send(res, 201, addLead(await json(req)));
    if (u.pathname === '/api/calls' && req.method === 'GET') return send(res, 200, listCalls());
    if (u.pathname === '/api/queue' && req.method === 'GET') return send(res, 200, nextQueue(Number(u.searchParams.get('limit') || 20)));

    const callMatch = u.pathname.match(/^\/api\/leads\/([^/]+)\/call$/);
    if (callMatch && req.method === 'POST') {
      const lead = getLead(callMatch[1]);
      if (!lead) return send(res, 404, { error: 'lead_not_found' });
      if (lead.stage === 'do_not_call') return send(res, 409, { error: 'do_not_call' });
      return send(res, 200, await startCall(lead));
    }

    if (u.pathname === '/api/campaign/run' && req.method === 'POST') {
      const body = await json(req);
      const limit = Math.min(Number(body.limit || dashboard().dailyLimit), 200);
      const queue = nextQueue(limit);
      const results = [];
      for (const lead of queue) results.push(await startCall(lead));
      return send(res, 200, { queued: results.length, results });
    }

    if (u.pathname === '/api/telephony/result' && req.method === 'POST') {
      const body = await json(req);
      const lead = getLead(body.leadId);
      if (!lead) return send(res, 404, { error: 'lead_not_found' });
      const analysis = await analyzeConversation({ transcript: body.transcript || '', lead });
      const updated = patchLead(lead.id, {
        score: analysis.score,
        stage: analysis.stage,
        nextCallAt: analysis.nextCallAt || lead.nextCallAt,
        lastSummary: analysis.summary,
        lastIntent: analysis.intent,
        extracted: analysis.extracted,
        handoffToOmid: analysis.handoffToOmid,
        doNotCall: analysis.doNotCall,
        lastCallAt: new Date().toISOString()
      });
      const call = addCall({ leadId: lead.id, phone: lead.phone, status: body.status || 'completed', provider: body.provider || 'external', transcript: body.transcript || '', recordingUrl: body.recordingUrl || '', durationSec: body.durationSec || 0, analysis });
      return send(res, 200, { lead: updated, call });
    }

    if (u.pathname === '/' || u.pathname === '/index.html') {
      return send(res, 200, fs.readFileSync(path.join(root, 'index.html'), 'utf8'), 'text/html; charset=utf-8');
    }
    return send(res, 404, { error: 'not_found' });
  } catch (e) {
    console.error(e);
    return send(res, 500, { error: 'server_error', detail: String(e.message || e) });
  }
});

server.listen(PORT, () => console.log(`NamiWin Smart Sales Agent: http://localhost:${PORT}`));
