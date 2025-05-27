// File: LockFragment.kt
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.appblock.databinding.FragmentLockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    // ───── Launchers ─────────────────────────────────────
    private val reqUsage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateUiForPermissions() }

    private val reqOverlay = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateUiForPermissions() }

    private val reqNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updateUiForPermissions() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
        updateUiForPermissions()
    }

    override fun onResume() {
        super.onResume()
        // give Settings a moment to take effect
        binding.root.postDelayed({ updateUiForPermissions() }, 300)
    }

    // ───── UI State ──────────────────────────────────────
    private fun updateUiForPermissions() {
        val all = hasUsage()
                && hasOverlay()
                && hasNotification()
                && hasAccessibility()

        binding.permissionStatus.visibility    = if (all) View.GONE else View.VISIBLE
        binding.btnGrantPermissions.visibility = if (all) View.GONE else View.VISIBLE
        binding.btnAppList.visibility          = if (all) View.VISIBLE else View.GONE
        binding.btnMonitoredApps.visibility    = if (all) View.VISIBLE else View.GONE
    }

    // ───── Button Hooks ─────────────────────────────────
    private fun setupButtonListeners() {
        binding.btnGrantPermissions.setOnClickListener {
            promptNextPermission()
        }
        binding.btnAppList.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        binding.btnMonitoredApps.setOnClickListener {
            startActivity(Intent(requireContext(), MonitoredAppsActivity::class.java))
        }
    }

    // ───── Drive the next missing permission ────────────
    private fun promptNextPermission() {
        when {
            !hasUsage()         -> showUsageDialog()
            !hasOverlay()       -> showOverlayDialog()
            !hasNotification()  -> showNotificationDialog()
            !hasAccessibility() -> showAccessibilityDialog()
            else                -> { /* nothing left */ }
        }
    }

    // ───── Individual Checks ────────────────────────────
    private fun hasUsage(): Boolean {
        val ops = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlay(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(requireContext())
        else true

    private fun hasNotification(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        else true

    private fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val service = AppBlockAccessibilityService::class.java.name
        val expected = "${requireContext().packageName}/$service"
        Log.d("LockFragment", "Enabled services: $enabled")
        Log.d("LockFragment", "Expecting       : $expected")
        return enabled.split(':').any { it.equals(expected, true) }
    }

    // ───── Permission Dialogs ───────────────────────────
    private fun showUsageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("App Usage Access Required")
            .setMessage("We need Usage Access to know which apps you open.")
            .setPositiveButton("Open Settings") { _, _ ->
                reqUsage.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Display Over Other Apps")
            .setMessage("Allow overlay so we can show reminders/shame screens.")
            .setPositiveButton("Open Settings") { _, _ ->
                val i = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                reqOverlay.launch(i)
            }
            .setCancelable(false)
            .show()
    }

    private fun showNotificationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notifications Permission")
            .setMessage("Enable notifications so we can remind/shame you.")
            .setPositiveButton("Grant") { _, _ ->
                reqNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setCancelable(false)
            .show()
    }

    private fun showAccessibilityDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accessibility Service Required")
            .setMessage("Turn on our Accessibility Service for real-time blocking.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
