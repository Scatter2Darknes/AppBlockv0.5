# 🛑 App Block 🛑
## _Stop the Scroll, Embrace the Control_

> An Android app to help you curb distracting apps by shaming, delaying, and summarizing your usage.

---

## Table of Contents

- [Features](#features)  
- [Demo](#demo)  
- [Getting Started](#getting-started)  
  - [Prerequisites](#prerequisites)  
  - [Installation](#installation)  
- [Usage](#usage)  
  - [Granting Permissions](#granting-permissions)  
  - [Blocking Modes](#blocking-modes)  
  - [Summary & Tasks](#summary--tasks)  
- [Configuration](#configuration)  
- [Project Structure](#project-structure)  
- [License](#license)  
- [Contact](#contact)  

---

## Features

- **Real-time App Detection** via Accessibility Service  
- **Soft-lock** with customizable delay and shame notifications  
- **Time-based Locks**: set “do not disturb” windows per app  
- **Persistent Notifications** and redirections  
- **Usage Summary** for self-reflection
- **Tasks** to keep being productive

---

## Demo

<img src="docs/screenshot_lock.png" alt="Lock screen" width="200" />  
<img src="docs/screenshot_summary.png" alt="Usage summary" width="200" />

---

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later  
- Android SDK 21+  
- Kotlin 1.7+  

### Installation

##### If running on Android Studio
1. Clone this repo  
   ```bash
   git clone https://github.com/Scatter2Darknes/AppBlockv1.0
   cd appblock
2. Open in Android Studio
3. Build & run on your device/emulator
    
##### 📦 Download For Android Mobile Device

Grab the latest APK and sideload it onto your device:

**❗(Currently working on it, not ready yet)❗**

<!--- **Latest stable**:  -->
<!--  [Download AppBlock v1.0.0 (APK)](https://github.com/yourusername/appblock/releases/download/v1.0.0/appblock-v1.0.0.apk)-->
 
> **Tip:** You may need to allow “Install unknown apps” in your Android settings for your browser or file manager.

---

## Usage

1. **First Launch & Permissions**  
   - Open the app.  
   - Tap **Grant Permissions** to cycle through each required setting:  
     1. **App Usage Access** → toggles the Usage-Access screen  
     2. **Display Over Other Apps** → toggles the Overlay screen  
     3. **Notifications** → system dialog for POST_NOTIFICATIONS  
     4. **Accessibility Service** → toggles the Accessibility screen  
   - Once _all four_ are granted, the **Manage Blocked Apps** and **View Blocked Apps** buttons appear.

2. **Manage Your Blocked Apps**  
   - Tap **Manage Blocked Apps**.  
   - In the list, toggle the checkbox next to any app you want to block.  
   - Configure each app’s **Delay** (soft-lock), or **Time Lock** window.

3. **Using Soft-Lock & Shame Screens**  
   - When you launch a blocked app:  
     - If you’ve configured a delay, you’ll get a countdown notification.  
     - If no delay is set, or during your “off hours,” you’ll see a brief “shame” screen reminding you of your goal.

4. **View Your Stats**  
   - Switch to the **Summary** tab via the bottom navigation to see your weekly usage graph.  
   - Switch to the **Tasks** tab to schedule recurring reminders (e.g., “Check screen time daily”).

5. **Disabling a Block**  
   - Reopen **Manage Blocked Apps** and untoggle an app’s checkbox to remove its block.  
   - All per-app settings (delay & time windows) are saved across sessions.

6. **Manage Tasks**
    - Switch to the **Tasks** tab via the bottom navigation to (see/add/modify) your tasks.
    - Can check tasks as **completed** via clicking checkbox or edit tasks by clicking on them.
    
## Configuration

You can tweak the following settings to customize how AppBlock behaves:

1. **Build & Manifest**  
   - `minSdkVersion` (in `build.gradle`): minimum supported Android version (default: 21)  
   - `targetSdkVersion` (in `build.gradle`): should match latest SDK (e.g. 35)  
   - `AndroidManifest.xml`:  
     ```xml
     <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
     <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
     <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
     <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
     <meta-data android:name="android.allow_usage_access" android:value="true"/>
     ```

2. **Resources**  
   - `res/values/strings.xml` (notification channel):  
     ```xml
     <string name="blocking_channel_name">Blocking Notifications</string>
     <string name="blocking_channel_description">Reminders and status updates when you open a blocked app.</string>
     ```  
   - `res/values/colors.xml`:  
     ```xml
     <color name="purple_500">#6200EE</color>
     <color name="red">#F44336</color>
     ```

3. **Defaults & Constants**  
   ```kotlin
   // DEFAULT_BLOCK_DELAY_SEC in AppConstants.kt
   const val DEFAULT_BLOCK_DELAY_SEC = 10

   // pollInterval in AppLaunchDetector.kt
   private val pollInterval = 1000L // (1 second)
   private val notificationCooldown = 2000L // (2 seconds)

4. **Excluded Apps**
    - Edit `ExcludedApps.kt` to add/remove package names that should never be blocked
    - Default packages include:
    -- `com.android.settings` System Settings
    -- `com.example.appblock`  App Block itself
    -- `com.android.phone` Phone dialer
    -- `com.google.android.dialer`
    -- `com.android.emergency` Emergency Services
5. **Menu & Layout**
    - `res/menu/bottom_nav_menu.xml`: replace icons or title as needed
    - `res/layout/overlay_blocking.xml`: customize your overlay/shame screen appearance

# 📁 Project Structure (TODO)

```
app-block/
├── app/
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/com/example/appblock/
│ │ │ │ ├── accessibility/
│ │ │ │ │ └── AppBlockAccessibilityService.kt
│ │ │ │ ├── data/
│ │ │ │ │ ├── AppInfo.kt
│ │ │ │ │ └── TimeRange.kt
│ │ │ │ ├── detectors/
│ │ │ │ │ └── AppLaunchDetector.kt
│ │ │ │ ├── storage/
│ │ │ │ │ └── StorageHelper.kt
│ │ │ │ ├── services/
│ │ │ │ │ └── BlockingOverlayService.kt
│ │ │ │ ├── ui/
│ │ │ │ │ ├── main/
│ │ │ │ │ │ └── MainActivity.kt
│ │ │ │ │ ├── dashboard/
│ │ │ │ │ │ ├── DashboardActivity.kt
│ │ │ │ │ │ └── fragments/
│ │ │ │ │ │ ├── LockFragment.kt
│ │ │ │ │ │ ├── SummaryFragment.kt
│ │ │ │ │ │ └── TaskFragment.kt
│ │ │ │ │ └── adapter/
│ │ │ │ │ └── AppAdapter.kt
│ │ │ │ ├── utils/
│ │ │ │ │ └── NotificationHelper.kt
│ │ │ │ └── AppBlockApplication.kt
│ │ │ ├── res/
│ │ │ │ ├── layout/
│ │ │ │ │ ├── activity_main.xml
│ │ │ │ │ ├── activity_dashboard.xml
│ │ │ │ │ ├── fragment_lock.xml
│ │ │ │ │ └── dialog_app_details.xml
│ │ │ │ ├── menu/
│ │ │ │ │ └── bottom_nav_menu.xml
│ │ │ │ ├── drawable/
│ │ │ │ ├── values/
│ │ │ │ └── xml/
│ │ │ │ ├── accessibility_service_config.xml
│ │ │ │ └── backup_rules.xml
│ │ │ └── AndroidManifest.xml
│ │ └── test/…
│ ├── build.gradle
│ └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle/
│ └── wrapper/
├── README.md
└── LICENSE
```

## License

Copyright (c) 2025 Kevin Chan, Jasper Ha, Justin Guan, Sahir Mukadam & Grishen Hestiyas

All Rights Reserved.

This software and associated documentation files (the “Software”) are the exclusive property of [Your Name or Organization]. No part of the Software may be reproduced, distributed, modified, transmitted, or used in any form or by any means, whether electronic or mechanical, without the prior written permission of the copyright holder.
