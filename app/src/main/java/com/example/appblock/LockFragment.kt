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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.appblock.databinding.FragmentLockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    // Launchers for settings
    private val requestUsageAccess = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAndRequestPermissions() }

    private val requestOverlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAndRequestPermissions() }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { checkAndRequestPermissions() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFeatureButtons()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Small delay to allow Accessibility setting to register
        binding.root.postDelayed({
            checkAndRequestPermissions()
        }, 300)
    }


    // --- CENTRALIZED PERMISSION CHECK FLOW ---
    private fun checkAndRequestPermissions() {
        when {
            !hasUsagePermission() -> {
                showUsagePermissionDialog()
                return
            }
            !hasOverlayPermission() -> {
                showOverlayPermissionDialog()
                return
            }
            !hasNotificationPermission() -> {
                showNotificationPermissionDialog()
                return
            }
            !hasAccessibilityPermission() -> {
                showAccessibilityDialog()
                return
            }
            else -> {
                enableAppFeatures()
            }
        }
    }

    private fun hasAllCorePermissions(): Boolean =
        hasUsagePermission() && hasOverlayPermission() && hasNotificationPermission() && hasAccessibilityPermission()

    // --- INDIVIDUAL PERMISSION CHECKS ---
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

    private fun hasAccessibilityPermission(): Boolean {
        // You need the service name here!
        val expectedComponent = "${requireContext().packageName}/.AppBlockAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    // --- PERMISSION DIALOGS ---
    private fun showUsagePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("App Usage Access Required")
            .setMessage("This app needs access to app usage data to monitor which apps you open.")
            .setPositiveButton("Continue to Settings") { _, _ ->
                requestUsageAccess.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Exit") { _, _ -> blockAppAccess() }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Display Over Other Apps Required")
            .setMessage("To show blocking screens, please allow this app to display over others.")
            .setPositiveButton("Continue to Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                requestOverlayPermission.launch(intent)
            }
            .setNegativeButton("Exit") { _, _ -> blockAppAccess() }
            .setCancelable(false)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Notifications Required")
                    .setMessage("Notifications are needed to show blocking status updates.")
                    .setPositiveButton("Grant") { _, _ ->
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("Exit") { _, _ -> blockAppAccess() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun showAccessibilityDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accessibility Service Required")
            .setMessage("To enable real-time app blocking, you must enable the Accessibility Service for AppBlock. On the next screen, please find AppBlock and turn it on.")
            .setPositiveButton("Open Accessibility Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Exit") { _, _ -> blockAppAccess() }
            .setCancelable(false)
            .show()
    }

    // --- ENABLE/DISABLE UI BASED ON PERMISSIONS ---
    private fun enableAppFeatures() {
        binding.permissionStatus.visibility = View.GONE
        binding.btnAppList.isEnabled = true
        binding.btnMonitoredApps.isEnabled = true
    }

    private fun blockAppAccess() {
        binding.apply {
            permissionStatus.text = "All permissions are required to use this app."
            permissionStatus.visibility = View.VISIBLE
            btnAppList.isEnabled = false
            btnMonitoredApps.isEnabled = false
        }
        Toast.makeText(
            requireContext(),
            "All permissions are required to use this app",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun setupFeatureButtons() {
        binding.btnAppList.setOnClickListener {
            if (hasAllCorePermissions()) {
                startActivity(Intent(requireContext(), MainActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Grant all permissions to use this feature.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMonitoredApps.setOnClickListener {
            if (hasAllCorePermissions()) {
                startActivity(Intent(requireContext(), MonitoredAppsActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Grant all permissions to use this feature.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
