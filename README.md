# हाजिरी (Hajiri) — Android Attendance & Salary Tracker

A modern, native Android application built with **Kotlin** and **Jetpack Compose** (Material 3) for tracking daily attendance, overtime, salary, advances, and monthly reports in Hindi.

## Features

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

- **Settings & User Management (⚙️ सेटिंग्स):**
  - Custom monthly salary and OT hourly rate.
  - Working days per month setting.
  - User login, registration, and offline mode.
  - Data reset controls for clearing month data or all data.

- **Design & Offline-First Storage:**
  - Dark glassmorphism theme with modern typography.
  - SharedPreferences and reactive StateFlow persistence with `kotlinx.serialization`.
