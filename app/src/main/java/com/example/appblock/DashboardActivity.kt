package com.example.appblock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appblock.databinding.ActivityDashboardBinding
import com.example.appblock.summary.SummaryFragment
import com.example.appblock.tasks.TaskFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // On first launch show the LockFragment
        if (savedInstanceState == null) {
            showLockFragment()
        }

        // Inflate your menu and wire the item clicks
        binding.bottomNav.apply {
            // if you haven't set menu in XML, uncomment next line:
            // inflateMenu(R.menu.bottom_nav_menu)

            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.lock_menu    -> { showLockFragment();      true }
                    R.id.summary_menu -> { showSummaryFragment();   true }
                    R.id.task_menu    -> { showTasksFragment();     true }
                    else              -> false
                }
            }
        }
    }

    private fun showLockFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LockFragment())
            .commit()
    }

    private fun showSummaryFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SummaryFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun showTasksFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TaskFragment())
            .addToBackStack(null)
            .commit()
    }
}
