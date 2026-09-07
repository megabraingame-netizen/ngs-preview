function fallbackAnalysis({ transcript = '', lead = {} }) {
  const t = transcript.toLowerCase();
  let score = Number(lead.score || 50);
  const positives = ['قیمت','پیش فاکتور','نقشه','متراژ','جلسه','قرارداد','پنجره','کرتین','نما','واتساپ','کاتالوگ'];
  const negatives = ['نیاز ندارم','مزاحم','تماس نگیرید','پروژه نداریم'];
  for (const x of positives) if (t.includes(x)) score += 5;
  for (const x of negatives) if (t.includes(x)) score -= 15;
  score = Math.max(0, Math.min(100, score));
  const hot = score >= 80;
  const blocked = t.includes('تماس نگیرید') || t.includes('مزاحم');
  const stage = blocked ? 'do_not_call' : hot ? 'hot' : score >= 60 ? 'warm' : 'followup';
  const days = hot ? 1 : score >= 60 ? 3 : 14;
  return {
    score,
    stage,
    handoffToOmid: hot,
    summary: transcript.slice(0, 280) || 'تماس بدون متن ثبت شد.',
    intent: hot ? 'فرصت فروش جدی' : stage === 'warm' ? 'نیازمند پیگیری' : 'سرنخ سرد/نامشخص',
    nextCallAt: blocked ? null : new Date(Date.now() + days * 86400000).toISOString(),
    doNotCall: blocked,
    extracted: { project: '', location: lead.city || '', need: '', timeline: '', decisionMaker: '' }
  };
}

export async function analyzeConversation({ transcript, lead }) {
  if (!process.env.OPENAI_API_KEY) return fallbackAnalysis({ transcript, lead });

  const prompt = `تو مدیر فروش NamiWin هستی. مکالمه تلفنی فارسی را تحلیل کن. خروجی فقط JSON معتبر باشد با کلیدهای score(0-100), stage(new|followup|warm|hot|do_not_call), handoffToOmid(boolean), summary, intent, nextCallAt(ISO|null), doNotCall(boolean), extracted{project,location,need,timeline,decisionMaker}. اگر مشتری درخواست عدم تماس داشت do_not_call=true. اگر پروژه واقعی، متراژ، نقشه، استعلام قیمت، جلسه یا تصمیم خرید دارد امتیاز بالا بده. مشتری بسیار داغ را برای امید handoff کن.\n\nLead: ${JSON.stringify(lead)}\nTranscript: ${transcript}`;

  const response = await fetch('https://api.openai.com/v1/responses', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ model: process.env.OPENAI_MODEL || 'gpt-5.6-luna', input: prompt })
  });
  if (!response.ok) return fallbackAnalysis({ transcript, lead });
  const data = await response.json();
  const text = data.output_text || data.output?.flatMap(x => x.content || []).map(x => x.text || '').join('') || '';
  try { return JSON.parse(text); } catch { return fallbackAnalysis({ transcript, lead }); }
}

export function buildOpening(lead) {
  const category = lead.category || 'پروژه ساختمانی';
  return `سلام وقت بخیر. از مجموعه نامی‌وین تماس می‌گیرم. در زمینه در و پنجره آلومینیومی، کرتین‌وال، نما، شیشه، بازسازی و دکوراسیون فعالیت داریم. دیدم مجموعه شما در حوزه ${category} فعال است. خواستم ببینم در پروژه‌های در حال اجرا یا پیشِ رو، بخشی هست که هنوز برای اجرا یا استعلام قیمت تعیین تکلیف نشده باشد؟`;
}
