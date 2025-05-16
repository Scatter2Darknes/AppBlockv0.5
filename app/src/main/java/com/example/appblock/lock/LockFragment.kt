package com.example.appblock.lock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.appblock.MainActivity
import com.example.appblock.MonitoredAppsActivity
import com.example.appblock.R
import com.example.appblock.databinding.FragmentLockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class LockFragment : Fragment() {
    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    // Permission check contract
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PERMISSION", "Fragment view created")

        try {
            setupButtonListeners()
            Log.d("PERMISSION", "Buttons initialized")
            checkPermissionsAutomatically()
        } catch (e: Exception) {
            Log.e("PERMISSION", "Initialization failed", e)
        }
    }

    private fun checkPermissionsAutomatically() {
        Log.d("PERMISSION", "Checking permissions...")
        if (!hasUsagePermission()) {
            Log.d("PERMISSION", "Permission missing, showing dialog")
            showPermissionNeededDialog()
        } else {
            Log.d("PERMISSION", "Permission already granted")
            updateUIForGrantedPermissions()
        }
    }

    private fun showPermissionNeededDialog() {
        try {
            Log.d("PERMISSION", "Attempting to show dialog")
            if(isAdded && !isDetached) {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(R.string.permission_required_title)
                    setMessage(R.string.permission_rationale)
                    setPositiveButton(R.string.grant_access) { _, _ ->
                        Log.d("PERMISSION", "User clicked grant access")
                        openUsageSettings()
                    }
                    setNegativeButton(R.string.later) { _, _ ->
                        Log.d("PERMISSION", "User deferred permission")
                        binding.permissionStatus.visibility = View.VISIBLE
                    }
                    setCancelable(false)
                    show()
                }.show()
            }
            Log.d("PERMISSION", "Dialog shown successfully")
        } catch (e: Exception) {
            Log.e("PERMISSION", "Failed to show dialog", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update UI state when returning from settings
        checkPermissions()
    }

    private fun setupButtonListeners() {
        binding.btnAppList.setOnClickListener {
            if (hasUsagePermission()) {
                startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                    // Add clear top to prevent multiple instances
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } else {
                showPermissionNeededDialog()
            }
        }

        binding.btnMonitoredApps.setOnClickListener {
            if (hasUsagePermission()) {
                startActivity(Intent(requireContext(), MonitoredAppsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            } else {
                showPermissionNeededDialog()
            }
        }
    }

    private fun checkPermissions() {
        if (!hasUsagePermission()) {
            showPermissionRationale()
        } else {
            updateUIForGrantedPermissions()
        }
    }

    private fun hasUsagePermission(): Boolean {
        return try {
            val context = context ?: return false.also {
                Log.e("PERMISSION", "Null context in permission check")
            }

            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false.also {
                    Log.e("PERMISSION", "Failed to get AppOpsManager")
                }

            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }

            Log.d("PERMISSION", "Permission check result: $mode")
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e("PERMISSION", "Permission check failed", e)
            false
        }
    }

    private fun showPermissionRationale() {
        binding.permissionStatus.apply {
            text = getString(R.string.permission_needed)
            visibility = View.VISIBLE
        }

        binding.btnAppList.isEnabled = false
        binding.btnMonitoredApps.isEnabled = false
    }

    private fun updateUIForGrantedPermissions() {
        binding.permissionStatus.visibility = View.GONE
        binding.btnAppList.isEnabled = true
        binding.btnMonitoredApps.isEnabled = true
    }

    private fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Log.e("PERMISSION", "No activity to handle settings intent")
            // Show error message to user
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            updateUIForGrantedPermissions()
        } else {
            showPermissionRationale()
        }
    }


}