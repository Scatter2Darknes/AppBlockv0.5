package com.example.appblock.lock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.appblock.MainActivity
import com.example.appblock.MonitoredAppsActivity
import com.example.appblock.R
import com.example.appblock.databinding.FragmentLockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.Manifest
import android.os.Process
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.DialogFragment
import com.example.appblock.NotificationHelper


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

        checkPermissions()
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
            Toast.LENGTH_LONG
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