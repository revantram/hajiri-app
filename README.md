# 📋 हाजिरी (Hajiri) — Android Attendance & Salary Tracker

[![Download Latest APK](https://img.shields.io/badge/📱_Download_APK-GitHub_Releases-10B981?style=for-the-badge)](../../releases/latest)
[![Direct APK Download](https://img.shields.io/badge/⬇️_Direct_APK-Hajiri_v1.2-4F46E5?style=for-the-badge)](../../raw/HEAD/Hajiri-Release-v1.2.apk)
[![GitHub Actions Build](https://img.shields.io/badge/⚙️_Actions-Auto_Release-0EA5E9?style=for-the-badge)](../../actions)

---

### 📥 ऐप डाउनलोड कैसे करें? (How to Download & Install)

1. **Releases बटन से (Recommended):** ऊपर हरे रंग के **[📱 Download APK — GitHub Releases](../../releases/latest)** बटन पर क्लिक करें (या दाईं ओर **Releases** सेक्शन खोलें) और **`Hajiri-v1.2-Release.apk`** डाउनलोड करें।
2. **सीधा फाइल डाउनलोड:** आप ऊपर नीले **[⬇️ Direct APK](../../raw/HEAD/Hajiri-Release-v1.2.apk)** बटन या [Hajiri-Release-v1.2.apk](./Hajiri-Release-v1.2.apk) से भी सीधे APK डाउनलोड कर सकते हैं।
3. **ऐप के अंदर से ऑटो अपडेट:** ऐप खोलने पर नया GitHub Release अपने आप चेक होता है और 1-Click में नया APK डाउनलोड लिंक खुल जाता है।
4. डाउनलोड के बाद अपने Android फोन में APK फाइल पर टैप करके **Install** करें।

---

## ✨ Features

A modern, native Android application built with **Kotlin** and **Jetpack Compose** (Material 3) featuring an **iPhone 18 Liquid Glass Light Theme** for tracking daily attendance, overtime, salary, advances, and monthly reports in Hindi.

- **Daily Attendance Marking (आज की हाजिरी):**
  - One-tap marking for Present (हाजिर), Absent (गैरहाजिर), Half Day (आधा दिन), and Leave (छुट्टी).
  - Daily note attachment for recording specific day remarks.
  - Active status pill and intuitive unmarking.

- **Attendance Streak (स्ट्रीक):**
  - Consecutive attendance streak calculation with fire badge.
  - All-time highest streak record tracking.

- **Salary & Earnings Estimation (इस महीने की कमाई):**
  - Real-time earnings calculation: `(Present × Per Day) + (Half Day × Per Day / 2) + Overtime - Advances`.
  - Monthly workdays and salary configuration (default 26 days).
  - Progress bar showing earnings against full monthly wage.
  - In-hand ("हाथ में आएगा") calculation.

- **Overtime Tracker (⏰ ओवरटाइम):**
  - Quick addition of overtime hours per day (step 0.5h).
  - Hourly OT rate setting with automatic monthly OT summation.

- **Advance & Loan Tracker (💳 एडवांस / उधार):**
  - Track salary advances and loans taken with reason and timestamp.
  - Monthly deduction itemization with one-tap removal.

- **Interactive Calendar (📅 कैलेंडर):**
  - Full-month grid with Hindi month and day names (रवि, सोम, मंगल, बुध, गुरु, शुक्र, शनि).
  - Color-coded badges for each attendance status, Sunday highlights, and note dots.
  - Tap any day to cycle status (cannot mark future dates).

- **Reports & Analytics (📊 रिपोर्ट):**
  - Attendance percentage circular gauge with working day summary.
  - 6-month comparative bar chart for worked days.
  - Full breakdown of Present, Absent, Half, Leave, OT, and Advances.
  - Formatted text report copy to clipboard.
  - Native WhatsApp share integration via Android Share Sheet.

- **History (📜 इतिहास):**
  - Chronological list of attendance records per month with notes and OT.
  - Tap to edit or update directly from history.

- **Firebase Cloud Sync & Google Account (☁️ क्लाउड सिंक):**
  - Secure Google Sign-In via Jetpack Credential Manager.
  - Real-time Cloud Firestore sync scoped per user — attendance, OT, advances, and salary settings stay safe even if you uninstall the app or switch phones.
  - Custom profile name and optional mobile number stored privately in your cloud profile.
  - Data reset controls for clearing month data or all data.

- **Design & Modern Android Architecture:**
  - **iPhone 18 Liquid Glass Light Theme** with frosted acrylic cards, pastel iridescent backdrop, and floating liquid dock.
  - Kotlin, Jetpack Compose (Material 3), Coroutines/Flow, Firebase Auth, and Cloud Firestore with offline caching.

