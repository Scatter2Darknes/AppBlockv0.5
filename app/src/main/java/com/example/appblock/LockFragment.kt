package com.example.appblock

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.appblock.databinding.FragmentLockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Button
import android.widget.TextView

class LockFragment : Fragment() {
    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val requestUsageAccess = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissions() }

    private val requestOverlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissions() }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        when {
            isGranted -> handleNotificationGranted()
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                showNotificationRationaleDialog()
            }
            else -> showFinalWarningDialog()
        }
    }

    private fun handleNotificationGranted() {
        if (hasUsagePermission() && hasOverlayPermission()) {
            enableAppFeatures()
        } else {
            // Only request missing permissions
            checkPermissions()
        }
    }

    private fun showNotificationRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notifications Needed")
            .setMessage("Notifications help you:\n\n" +
                    "- See blocking status\n" +
                    "- Get delay countdown updates\n" +
                    "- Receive important alerts")
            .setPositiveButton("Try Again") { _, _ ->
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel") { _, _ -> checkPermissions() }
            .show()
    }

    override fun onResume() {
        super.onResume()

        val btnAccessibility = view?.findViewById<Button>(R.id.btn_enable_accessibility)
        val txtExplanation = view?.findViewById<TextView>(R.id.accessibility_explanation)

        val enabled = isAccessibilityServiceEnabled(requireContext())
        btnAccessibility?.isEnabled = !enabled

        if (enabled) {
            txtExplanation?.text = "Accessibility Service is enabled. AppBlock can now block apps in real time."
        } else {
            txtExplanation?.text = "AppBlock uses Android's Accessibility Service to detect and block apps in real time. No personal data is collected or transmitted. Please enable this service for effective blocking."
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/.AppBlockAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }



    private fun testOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(requireContext())) {
            Toast.makeText(requireContext(), "Enable overlay permission first", LENGTH_LONG).show()
            return
        }

        val intent = Intent(requireContext(), BlockingOverlayService::class.java).apply {
            putExtra("delaySeconds", 10L)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun testOverlayManually() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(requireContext())) {
            Toast.makeText(requireContext(), "Overlay permission needed", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), BlockingOverlayService::class.java).apply {
            putExtra("packageName", "com.android.chrome") // Test with Chrome
            putExtra("delaySeconds", 10L)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }

        Toast.makeText(requireContext(), "Testing overlay...", Toast.LENGTH_SHORT).show()
    }

    private fun testOverlaySafely() {
        try {
            // 1. Direct permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(requireContext())) {
                Toast.makeText(requireContext(), "Overlay permission denied", LENGTH_LONG).show()
                return
            }

            // 2. Start service directly
            val intent = Intent(requireContext(), BlockingOverlayService::class.java)
            requireContext().startService(intent)

            // 3. Quick visual feedback
            Toast.makeText(requireContext(), "Service started", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Security error: ${e.message}", LENGTH_LONG).show()
        } catch (e: WindowManager.BadTokenException) {
            Toast.makeText(requireContext(), "Window error: ${e.message}", LENGTH_LONG).show()
        }
    }




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PERMISSION_FLOW", "LockFragment view created")

        // Initialize button click listeners
        binding.btnAppList.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }

        binding.btnMonitoredApps.setOnClickListener {
            startActivity(Intent(requireContext(), MonitoredAppsActivity::class.java))
        }

        val btnAccessibility = view.findViewById<Button>(R.id.btn_enable_accessibility)
        btnAccessibility.setOnClickListener {
            // Open the Accessibility Settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun testOverlay(showRed: Boolean, blockTouches: Boolean) {
        try {
            if (!hasOverlayPermission()) {
                Toast.makeText(requireContext(), "Overlay permission needed", LENGTH_LONG).show()
                return
            }

            val intent = Intent(requireContext(), BlockingOverlayService::class.java).apply {
                putExtra("delaySeconds", 10L)
                putExtra("showRed", showRed)
                putExtra("blockTouches", blockTouches)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.javaClass.simpleName}", LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermissions() {
        Log.d("PERMISSION_CHECK", "Checking permissions...")

        when {
            !hasUsagePermission() -> {
                Log.d("PERMISSION_CHECK", "Usage Access permission missing")
                showUsagePermissionDialog()
            }
            !hasOverlayPermission() -> {
                Log.d("PERMISSION_CHECK", "Overlay permission missing")
                showOverlayPermissionDialog()
            }
            !hasNotificationPermission() -> {
                Log.d("PERMISSION_CHECK", "Notifications permission missing")
                showNotificationPermissionDialog()
            }
            else -> {
                Log.d("PERMISSION_CHECK", "All permissions granted")
                enableAppFeatures()
            }
        }
    }

    private fun showUsagePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("App Usage Access Required")
            .setMessage("This app needs access to app usage data to monitor which apps you open.")
            .setPositiveButton("Continue to Settings") { _, _ ->
                requestUsageAccess.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Cancel") { _, _ -> blockAppAccess() }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Display Over Other Apps Required")
            .setMessage("To show blocking screens, please allow this app to display over others.")
            .setPositiveButton("Continue to Settings") { _, _ ->
                requestOverlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
            .setNegativeButton("Cancel") { _, _ -> blockAppAccess() }
            .setCancelable(false)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Notifications Required")
                    .setMessage("Notifications are needed to show blocking status updates.")
                    .setPositiveButton("Grant") { _, _ ->
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("Cancel") { _, _ -> blockAppAccess() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(requireContext())
        } else true
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun enableAppFeatures() {
        Log.d("PERMISSION_FLOW", "Enabling app features")
        binding.apply {
            permissionStatus.visibility = View.GONE
            btnAppList.isEnabled = true
            btnMonitoredApps.isEnabled = true
        }

        // Clear any existing dialogs
        parentFragmentManager.fragments.forEach {
            if (it is DialogFragment) it.dismiss()
        }

        // Secure notification test with explicit permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(requireContext())
                    .notify(1, NotificationCompat.Builder(requireContext(), NotificationHelper.CHANNEL_ID)
                        .setContentTitle("App Block Active")
                        .setContentText("Notifications working!")
                        .setSmallIcon(R.drawable.ic_block)
                        .build())
            }
        }
    }

    private fun blockAppAccess() {
        binding.apply {
            permissionStatus.text = getString(R.string.permissions_required_warning)
            permissionStatus.visibility = View.VISIBLE
            btnAppList.isEnabled = false
            btnMonitoredApps.isEnabled = false
        }
        Toast.makeText(
            requireContext(),
            "All permissions are required to use this app",
            LENGTH_LONG
        ).show()
    }

    private fun showFinalWarningDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("To use all features, please grant:\n\n" +
                    "1. App Usage Access\n" +
                    "2. Display Over Other Apps\n" +
                    "3. Notifications")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                })
            }
            .setNegativeButton("Exit") { _, _ ->
                requireActivity().finishAffinity()
            }
            .setCancelable(false)
            .show()
    }
}