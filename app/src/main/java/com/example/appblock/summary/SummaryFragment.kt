package com.example.appblock.summary

import com.example.appblock.R
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.util.concurrent.TimeUnit

class SummaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private lateinit var timeSpinner: Spinner
    private val appUsageList = mutableListOf<AppUsageInfo>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timeSpinner = view.findViewById(R.id.spinnerTimePeriod)
        recyclerView = view.findViewById(R.id.recyclerViewAppUsage)
        adapter = AppUsageAdapter(appUsageList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        setupTimeSpinner()
        checkUsagePermission()
    }

    private fun setupTimeSpinner() {
        val options = arrayOf("Daily", "Weekly", "Monthly")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        timeSpinner.adapter = spinnerAdapter

        timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadAppUsageData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun checkUsagePermission() {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = Calendar.getInstance()
        val end = now.timeInMillis
        now.add(Calendar.DAY_OF_YEAR, -1)
        val start = now.timeInMillis

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

        if (stats.isNullOrEmpty()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadAppUsageData()
        }
    }

    private fun getTimeRange(type: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis

        when (type) {
            0 -> calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            1 -> calendar.apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            2 -> calendar.apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        }

        return calendar.timeInMillis to end
    }

    private fun loadAppUsageData() {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = requireContext().packageManager
        val position = timeSpinner.selectedItemPosition
        val (start, end) = getTimeRange(position)

        val interval = when (position) {
            0 -> UsageStatsManager.INTERVAL_DAILY
            1 -> UsageStatsManager.INTERVAL_WEEKLY
            2 -> UsageStatsManager.INTERVAL_MONTHLY
            else -> UsageStatsManager.INTERVAL_DAILY
        }

        val stats = usageStatsManager.queryUsageStats(interval, start, end)
        appUsageList.clear()

        stats?.forEach { stat ->
            if (stat.totalTimeInForeground > 0) {
                try {
                    val info = pm.getApplicationInfo(stat.packageName, 0)
                    val name = pm.getApplicationLabel(info).toString()
                    val icon = pm.getApplicationIcon(stat.packageName)
                    val usageStr = formatUsageTime(stat.totalTimeInForeground)

                    appUsageList.add(AppUsageInfo(name, stat.packageName, usageStr, stat.totalTimeInForeground, icon))
                } catch (e: PackageManager.NameNotFoundException) {
                    // App uninstalled or hidden; skip it
                }
            }
        }

        appUsageList.sortByDescending { it.usageTimeMs }
        adapter.notifyDataSetChanged()
    }

    private fun formatUsageTime(ms: Long): String {
        val hrs = TimeUnit.MILLISECONDS.toHours(ms)
        val min = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return when {
            hrs > 0 -> "${hrs}h ${min}m"
            min > 0 -> "${min}m ${sec}s"
            else -> "${sec}s"
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppUsageData()
    }
}
