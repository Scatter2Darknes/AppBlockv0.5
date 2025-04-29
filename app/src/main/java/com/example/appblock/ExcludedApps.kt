package com.example.appblock

class ExcludedApps {
    companion object {
        // Add more package names as needed
        private val excludedPackages = setOf(
            "com.android.settings", // system settings
            "com.example.appblock", // app blocker itself
            "com.android.phone",       // Phone dialer
            "com.google.android.dialer",
            "com.android.emergency"    // Emergency services
        )

        fun isExcluded(packageName: String): Boolean {
            return excludedPackages.contains(packageName)
        }
    }
}