# 🔥 Firebase Setup — हाजिरी ऐप

## Firebase क्यों चाहिए?
आपका डेटा क्लाउड में सुरक्षित रहे, और मोबाइल नंबर से login करने पर वापस मिले।

---

## Step 1 — Firebase Project बनाएं

1. **https://console.firebase.google.com** पर जाएं
2. **"Add Project"** → Project name: `hajiri-app` → Create
3. Left sidebar में **Firestore Database** → **Create Database**
   - Mode: **Test Mode** चुनें (30 दिन free)
   - Location: `asia-south1 (Mumbai)` चुनें

---

## Step 2 — Web App Register करें

1. Firebase Console → Project Settings (⚙️ icon)
2. **"Add App"** → Web (</> icon)
3. App nickname: `hajiri-web` → Register
4. आपको एक **config object** मिलेगा जो कुछ ऐसा दिखेगा:

```js
const firebaseConfig = {
  apiKey: "AIzaSy...",
  authDomain: "hajiri-app-xxxxx.firebaseapp.com",
  projectId: "hajiri-app-xxxxx",
  storageBucket: "hajiri-app-xxxxx.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdef123456"
};
```

---

## Step 3 — `index.html` में Config डालें

[index.html](file:///data/user/0/com.termux/files/home/projects/hajiri-app/index.html) फाइल खोलें और
**line ~27** पर यह section ढूंढें:

```js
const firebaseConfig = {
  apiKey: "AIzaSyDEMO_REPLACE_WITH_YOUR_KEY",
  ...
```

इसे अपने असली config से replace करें।

---

## Step 4 — Firestore Security Rules

Firebase Console → Firestore → **Rules** tab → यह paste करें:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if true;
    }
  }
}
```

**Publish** करें।

---

## 🎉 Done! अब आप:
- मोबाइल नंबर + PIN से Login कर सकते हैं
- किसी भी डिवाइस पर Data sync होगा  
- Real-time updates मिलेंगे

---

## ⚠️ Firebase Config नहीं है तो?

**कोई बात नहीं!** App **Offline Mode** में भी पूरी तरह काम करती है।
Login screen पर **"बिना Login के उपयोग करें"** tap करें।
Data उस फोन में save रहेगा।

---

## 📱 App खोलने के तरीके

**Option 1 — Browser में:**
```bash
# Termux में run करें:
termux-open /data/user/0/com.termux/files/home/projects/hajiri-app/index.html
```

**Option 2 — Simple HTTP Server:**
```bash
cd ~/projects/hajiri-app
python3 -m http.server 8080
# फिर browser में: http://localhost:8080
```
