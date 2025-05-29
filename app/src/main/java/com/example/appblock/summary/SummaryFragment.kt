package com.example.appblock.summary

import com.example.appblock.R
//import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import android.widget.LinearLayout
//import android.widget.ImageView
//import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import java.util.*
import java.util.concurrent.TimeUnit

class SummaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private lateinit var timePeriodSpinner: Spinner
    private val appUsageList = mutableListOf<AppUsageInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timePeriodSpinner = view.findViewById(R.id.spinnerTimePeriod)
        recyclerView = view.findViewById(R.id.recyclerViewAppUsage)
        adapter = AppUsageAdapter(appUsageList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        setupTimePeriodSpinner()
        checkUsageStatsPermission()
    }

    private fun setupTimePeriodSpinner() {
        val timePeriods = arrayOf("Daily", "Weekly", "Monthly")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timePeriods)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timePeriodSpinner.adapter = spinnerAdapter

        timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadAppUsageData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun checkUsageStatsPermission() {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isEmpty()) {
            // Permission not granted, redirect to settings
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            loadAppUsageData()
        }
    }

    private fun getTimeRange(position: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        when (position) {
            0 -> { // Daily
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            1 -> { // Weekly
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            2 -> { // Monthly
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        return Pair(calendar.timeInMillis, endTime)
    }

    private fun loadAppUsageData() {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = requireContext().packageManager

        val selectedPosition = timePeriodSpinner.selectedItemPosition
        val (startTime, endTime) = getTimeRange(selectedPosition)

        val interval = when (selectedPosition) {
            0 -> UsageStatsManager.INTERVAL_DAILY
            1 -> UsageStatsManager.INTERVAL_WEEKLY
            2 -> UsageStatsManager.INTERVAL_MONTHLY
            else -> UsageStatsManager.INTERVAL_DAILY
        }

        val usageStatsList = usageStatsManager.queryUsageStats(
            interval,
            startTime,
            endTime
        )

        appUsageList.clear()

        for (usageStats in usageStatsList) {
            if (usageStats.totalTimeInForeground > 0) {
                try {
                    val appInfo = packageManager.getApplicationInfo(usageStats.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon = packageManager.getApplicationIcon(usageStats.packageName)

                    val usageTime = formatUsageTime(usageStats.totalTimeInForeground)

                    appUsageList.add(
                        AppUsageInfo(
                            appName = appName,
                            packageName = usageStats.packageName,
                            usageTime = usageTime,
                            usageTimeMs = usageStats.totalTimeInForeground,
                            appIcon = appIcon
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might be uninstalled, skip
                }
            }
        }

        // Sort by usage time (descending)
        appUsageList.sortByDescending { it.usageTimeMs }
        adapter.notifyDataSetChanged()
    }

    private fun formatUsageTime(timeInMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMs) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppUsageData()
    }
}