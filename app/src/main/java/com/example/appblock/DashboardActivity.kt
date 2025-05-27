// File: DashboardActivity.kt
package com.example.appblock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appblock.databinding.ActivityDashboardBinding
import com.example.appblock.summary.SummaryFragment
import com.example.appblock.tasks.TaskFragment

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LockFragment())
                .commit()
        }
    }

    fun showSummaryFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SummaryFragment())
            .addToBackStack(null)
            .commit()
    }

    fun showTasksFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TaskFragment())
            .addToBackStack(null)
            .commit()
    }
}
