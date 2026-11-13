const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const APK_PATH = path.join(__dirname, 'Hajiri-v1.0.apk');

const HTML_CONTENT = `<!DOCTYPE html>
<html lang="hi">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>हाजिरी (Hajiri) — Android APK Download</title>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet"/>
  <style>
    :root {
      --bg: #F4F6FB;
      --card: rgba(255, 255, 255, 0.92);
      --border: rgba(255, 255, 255, 0.8);
      --primary: #4F46E5;
      --primary-dark: #4338CA;
      --green: #059669;
      --green-bg: #D1FAE5;
      --text: #0F172A;
      --text-muted: #64748B;
    }
    * { margin:0; padding:0; box-sizing:border-box; font-family:'Plus Jakarta Sans', system-ui, sans-serif; }
    body {
      background: linear-gradient(135deg, #EEF2FF 0%, #F5F3FF 50%, #FCE7F3 100%);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .container {
      width: 100%;
      max-width: 480px;
      background: var(--card);
      border: 1.5px solid var(--border);
      border-radius: 32px;
      padding: 36px 28px;
      backdrop-filter: blur(24px);
      box-shadow: 0 20px 50px rgba(79, 70, 229, 0.12), 0 4px 12px rgba(0,0,0,0.04);
      text-align: center;
    }
    .badge-icon {
      width: 84px;
      height: 84px;
      margin: 0 auto 16px;
      border-radius: 24px;
      background: linear-gradient(135deg, #6366F1, #4338CA);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 42px;
      box-shadow: 0 12px 28px rgba(79, 70, 229, 0.35);
    }
    h1 { font-size: 26px; font-weight: 800; color: var(--text); margin-bottom: 6px; }
    .sub { font-size: 14px; color: var(--text-muted); margin-bottom: 24px; line-height: 1.5; }
    .version-chip {
      display: inline-block;
      background: var(--green-bg);
      color: var(--green);
      font-size: 12px;
      font-weight: 700;
      padding: 4px 14px;
      border-radius: 50px;
      margin-bottom: 24px;
    }
    .btn-download {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      background: linear-gradient(135deg, #10B981, #059669);
      color: white;
      text-decoration: none;
      font-size: 16px;
      font-weight: 800;
      padding: 18px 24px;
      border-radius: 18px;
      box-shadow: 0 10px 25px rgba(16, 185, 129, 0.35);
      transition: transform 0.15s, box-shadow 0.15s;
    }
    .btn-download:active { transform: scale(0.98); }
    .file-info {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 10px;
      margin-bottom: 28px;
    }
    .steps-box {
      text-align: left;
      background: rgba(241, 245, 249, 0.8);
      border-radius: 20px;
      padding: 20px;
      margin-top: 10px;
    }
    .steps-title { font-size: 13px; font-weight: 700; margin-bottom: 12px; color: var(--text); }
    .step-item {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      font-size: 12.5px;
      color: #334155;
      margin-bottom: 8px;
      line-height: 1.4;
    }
    .step-item:last-child { margin-bottom: 0; }
    .step-num {
      width: 20px;
      height: 20px;
      background: var(--primary);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 700;
      flex-shrink: 0;
    }
    .github-box {
      margin-top: 20px;
      padding-top: 16px;
      border-top: 1px solid rgba(0,0,0,0.06);
      font-size: 12px;
      color: var(--text-muted);
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="badge-icon">📋</div>
    <h1>हाजिरी (Hajiri)</h1>
    <div class="sub">दैनिक हाजिरी, ओवरटाइम, तनख्वाह और एडवांस का हिसाब-किताब</div>
    <div><span class="version-chip">v1.0.0 • Liquid Glass Edition</span></div>

    <a href="/Hajiri-v1.0.apk" download="Hajiri-v1.0.apk" class="btn-download">
      <span>⬇️</span> डाउनलोड करें (Direct APK)
    </a>
    <div class="file-info">📦 Hajiri-v1.0.apk • ~10.8 MB • Android 8.0+</div>

    <div class="steps-box">
      <div class="steps-title">📱 फोन में इनस्टॉल कैसे करें:</div>
      <div class="step-item">
        <span class="step-num">1</span>
        <span>ऊपर दिए गए हरे बटन पर टैप करके APK डाउनलोड करें।</span>
      </div>
      <div class="step-item">
        <span class="step-num">2</span>
        <span>डाउनलोड पूरा होने के बाद नोटिफिकेशन या फाइल्स में <b>Hajiri-v1.0.apk</b> पर क्लिक करें।</span>
      </div>
      <div class="step-item">
        <span class="step-num">3</span>
        <span>यदि "Install from Unknown Sources" पूछे, तो Chrome/Browser को "Allow" करें।</span>
      </div>
      <div class="step-item">
        <span class="step-num">4</span>
        <span><b>Install</b> पर क्लिक करें और हाजिरी ऐप का आनंद लें!</span>
      </div>
    </div>

    <div class="github-box">
      ⚡ <b>GitHub Release:</b> कोड में <code>.github/workflows/release.yml</code> जोड़ दिया गया है। जैसे ही आप रिपॉजिटरी पुश करेंगे, GitHub Actions अपने आप नया रिलीज और APK तैयार कर देगा!
    </div>
  </div>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  if (url === '/Hajiri-v1.0.apk' || url === '/app-debug.apk') {
    if (fs.existsSync(APK_PATH)) {
      const stat = fs.statSync(APK_PATH);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="Hajiri-v1.0.apk"',
        'Cache-Control': 'no-cache'
      });
      fs.createReadStream(APK_PATH).pipe(res);
      return;
    }
  }

  // Default: serve the download page
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(HTML_CONTENT);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Hajiri download server running at http://0.0.0.0:${PORT}`);
});
