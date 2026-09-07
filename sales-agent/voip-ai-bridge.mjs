import http from 'node:http';
import { randomUUID } from 'node:crypto';

const PORT=Number(process.env.VOIP_BRIDGE_PORT||8790);
const START_URL=process.env.TELEPHONY_AI_START_URL||'';
const STATUS_URL=process.env.TELEPHONY_AI_STATUS_URL||'';
const CONTROL_URL=process.env.TELEPHONY_AI_CONTROL_URL||'';
const TOKEN=process.env.TELEPHONY_AI_TOKEN||'';
const API_TOKEN=process.env.BRIDGE_API_TOKEN||'';
const calls=new Map();

function send(res,code,obj){res.writeHead(code,{'content-type':'application/json; charset=utf-8','access-control-allow-origin':'*','access-control-allow-headers':'authorization,content-type'});res.end(JSON.stringify(obj));}
async function read(req){const a=[];for await(const c of req)a.push(c);return a.length?JSON.parse(Buffer.concat(a).toString('utf8')):{};}
function auth(req){if(!API_TOKEN)return true;return req.headers.authorization===`Bearer ${API_TOKEN}`;}
async function provider(url,body){if(!url)throw new Error('telephony_provider_not_configured');const r=await fetch(url,{method:'POST',headers:{'content-type':'application/json',...(TOKEN?{authorization:`Bearer ${TOKEN}`}:{})},body:JSON.stringify(body)});const text=await r.text();let out={};try{out=JSON.parse(text)}catch{out={raw:text}}if(!r.ok)throw new Error(`provider_http_${r.status}:${text}`);return out;}

const server=http.createServer(async(req,res)=>{
  try{
    if(req.method==='OPTIONS')return send(res,204,{});
    if(req.url==='/health'&&req.method==='GET')return send(res,200,{ok:true,configured:Boolean(START_URL),mode:START_URL?'provider':'not-configured'});
    if(!auth(req))return send(res,401,{error:'unauthorized'});

    if(req.url==='/api/ai-call/start'&&req.method==='POST'){
      const body=await read(req); if(!body.phone)return send(res,400,{error:'phone_required'});
      const localId=randomUUID();
      const instruction=`تو دستیار هوشمند فروش نامی‌وین هستی. فارسی، کوتاه و طبیعی صحبت کن. خودت را دستیار هوشمند معرفی کن. هدف: تشخیص پروژه در و پنجره آلومینیومی، کرتین وال، نما، شیشه، بازسازی، کابینت، دکور مغازه یا غرفه نمایشگاهی. اگر مشتری درخواست عدم تماس کرد فوراً محترمانه پایان بده. اگر مشتری جدی شد یا مدیر را خواست، handoff درخواست کن.`;
      const payload={phone:body.phone,language:'fa-IR',instruction,campaign:body.campaign||'namiwin-sales-test',handoff:body.handoff||'operator'};
      const p=await provider(START_URL,payload);
      const providerCallId=p.callId||p.id||'';
      calls.set(localId,{id:localId,providerCallId,status:'queued',phone:body.phone,createdAt:new Date().toISOString(),provider:p});
      return send(res,200,{callId:localId,status:'queued',providerCallId});
    }

    const m=req.url.match(/^\/api\/ai-call\/([^/]+)$/);
    if(m&&req.method==='GET'){
      const c=calls.get(m[1]);if(!c)return send(res,404,{error:'call_not_found'});
      if(STATUS_URL&&c.providerCallId){try{const p=await provider(STATUS_URL,{callId:c.providerCallId});c.status=p.status||c.status;c.summary=p.summary||c.summary;c.last=p;}catch(e){c.lastError=String(e.message||e)}}
      return send(res,200,{callId:c.id,status:c.status,summary:c.summary||'',providerCallId:c.providerCallId,lastError:c.lastError||''});
    }

    const k=req.url.match(/^\/api\/ai-call\/([^/]+)\/control$/);
    if(k&&req.method==='POST'){
      const c=calls.get(k[1]);if(!c)return send(res,404,{error:'call_not_found'});const body=await read(req);
      if(!CONTROL_URL)return send(res,409,{error:'control_provider_not_configured'});
      const p=await provider(CONTROL_URL,{callId:c.providerCallId,action:body.action||'handoff'});c.status=body.action==='handoff'?'handoff_requested':c.status;c.lastControl=p;return send(res,200,{ok:true,status:c.status,provider:p});
    }
    return send(res,404,{error:'not_found'});
  }catch(e){return send(res,500,{error:String(e.message||e)});}
});
server.listen(PORT,()=>console.log(`NamiWin VoIP AI Bridge: http://0.0.0.0:${PORT}`));
