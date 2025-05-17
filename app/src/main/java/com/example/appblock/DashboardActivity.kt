package com.example.appblock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.appblock.databinding.ActivityDashboardBinding
import com.example.appblock.summary.SummaryFragment
import com.example.appblock.tasks.TaskFragment

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load LockFragment immediately using supportFragmentManager
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LockFragment())
            .commit() // Changed to regular commit

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.lock_menu -> replaceFragment(LockFragment())
                R.id.summary_menu -> replaceFragment(SummaryFragment())
                R.id.task_menu -> replaceFragment(TaskFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Add this line
            .commit()
    }
}