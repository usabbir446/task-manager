# TaskFlow v2.0 - High-Performance Productivity Task Manager

**TaskFlow** is an advanced Android productivity powerhouse built with Kotlin, Jetpack Compose, Material 3, Room Database, and WorkManager.

---

## 🌟 Newly Added Features

### 1. 🗣️ Enhanced Voice & Natural Language Understanding
* **Bilingual Date & Time Detection**:
  * English: `"Meeting tomorrow at 3 PM"`, `"Submit proposal by 10:30 am"`.
  * Bangla: `"কালকে সকাল ১০টায় রিপোর্ট সাবমিট করতে হবে"`, `"আজকে বিকেল ৪টায় মিটিং"`.
  * Automatically sets exact `AlarmManager` reminders at the spoken time without opening a calendar.
* **Sub-task / Checklist Extraction from Speech**:
  * Say: `"Launch new feature: code review, test on emulator, write release notes"`.
  * The colon (`:`) or comma/sentence cues automatically break speech into an interactive checklist under that task.
* **Quick Add via Quick Settings Tile**:
  * Android notification shade tile (`QuickAddTileService`) for 1-tap voice recording without opening the app first.

### 2. 📊 Daily Productivity Insights & Analytics
* **Daily Streak Counter**:
  * Tracks consecutive active days of completing tasks with a flame indicator 🔥.
* **Productivity Velocity & Peak Window**:
  * Analyzes completion timestamps to identify your peak productivity hours (e.g. `10 AM - 12 PM`).
* **Weekly Retrospective (Sunday 7 PM Briefing)**:
  * Digest notification showing weekly completed volume, most productive task categories, and carry-over tasks.

### 3. 🏢 Smart Work & Office Context
* **Geofence Arrival Trigger**:
  * Integrates `play-services-location` to trigger morning task briefing notifications when arriving at office coordinates.
* **Recurring Habits & Tasks**:
  * Support for recurring task schedules: `Daily`, `Weekdays`, and `Weekly`. When marked complete, the next instance automatically schedules.

### 4. 📱 Android Ecosystem Power-Ups
* **Interactive Home Screen Widget**:
  * Check off tasks and jump directly into voice recording from your phone's home screen.
* **Actionable Notification Buttons**:
  * `"Mark Top Done"` and `"Snooze 1h"` directly from the Android notification shade.
* **CSV & Markdown Export**:
  * Export task records to CSV or format them into a Markdown report and share via Google Drive, Email, or Slack.

---

## 📦 Verified APK
* **File**: `app/build/outputs/apk/debug/app-debug.apk`
* **Status**: `BUILD SUCCESSFUL`
