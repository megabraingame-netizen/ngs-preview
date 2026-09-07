# NamiWin Smart Sales Agent v0.1

نسخه اولیه بازاریاب هوشمند تلفنی متصل به CRM.

## قابلیت‌های فعلی
- ثبت و مدیریت سرنخ
- امتیازدهی Hot / Warm / Follow-up
- صف تماس روزانه
- تماس دستی یا اجرای کمپین تا 200 سرنخ در هر اجرا
- حالت Mock برای تست بدون خط تلفن
- Webhook عمومی برای اتصال به SIP / Asterisk / VoIP provider
- دریافت transcript، recording URL، duration و status تماس
- تحلیل مکالمه و استخراج پروژه، محل، نیاز، زمان خرید و تصمیم‌گیرنده
- علامت Do Not Call
- ارجاع سرنخ داغ به Omid
- داشبورد فارسی

## اجرا
از ریشه مخزن:

```bash
node sales-agent/server.mjs
```

سپس مرورگر را روی این آدرس باز کنید:

```text
http://localhost:8787
```

نیازی به npm install نیست؛ پروژه فقط از ماژول‌های داخلی Node.js 20+ استفاده می‌کند.

## اتصال تماس واقعی
در `.env` این متغیرها را تنظیم کنید:

- `TELEPHONY_WEBHOOK_URL`: آدرس gateway که تماس خروجی را روی SIP/Asterisk/VoIP ایجاد می‌کند.
- `TELEPHONY_TOKEN`: توکن اختیاری gateway.
- `PUBLIC_BASE_URL`: آدرس عمومی همین CRM تا gateway نتیجه تماس را به `/api/telephony/result` برگرداند.

### Payload تماس خروجی

```json
{
  "leadId": "...",
  "phone": "09...",
  "opening": "متن شروع مکالمه",
  "callbackUrl": "https://.../api/telephony/result"
}
```

### Payload نتیجه تماس

```json
{
  "leadId": "...",
  "status": "completed",
  "provider": "asterisk",
  "durationSec": 92,
  "recordingUrl": "https://.../call.wav",
  "transcript": "متن کامل مکالمه"
}
```

## تحلیل هوشمند
اگر `OPENAI_API_KEY` تنظیم شود، مکالمه با Responses API تحلیل می‌شود؛ در غیر این صورت موتور rule-based داخلی فعال است و پروژه بدون API هم کار می‌کند.

## مرحله بعدی
برای تبدیل v0.1 به تماس‌گیر واقعی باید gateway صوتی به خط SIP/VoIP متصل شود و صوت دوطرفه + STT/TTS یا Realtime voice به آن اضافه گردد. منطق CRM و callback از همین نسخه آماده است.
