package com.example.appblock.lock

import com.example.appblock.MainActivity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
// import com.example.appblock.BlockedAppsActivity
import com.example.appblock.MonitoredAppsActivity
import com.example.appblock.R

class LockFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lock, container, false)

        view.findViewById<Button>(R.id.btn_app_list).setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }

        view.findViewById<Button>(R.id.btn_monitored_apps).setOnClickListener {
            startActivity(Intent(requireContext(), MonitoredAppsActivity::class.java))
        }

        return view
    }
}
