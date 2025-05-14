package com.example.appblock.summary

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblock.AppInfo
import com.example.appblock.ExcludedApps
import com.example.appblock.databinding.FragmentSummaryBinding  // This will be generated

class SummaryFragment : Fragment() {
    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        // Set adapter with installed apps
        binding.recyclerView.adapter = SummaryAdapter(getInstalledApps())
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = requireActivity().packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            pm.queryIntentActivities(intent, 0).mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                if (!ExcludedApps.isExcluded(packageName)) {
                    AppInfo(
                        name = info.loadLabel(pm).toString(),
                        packageName = packageName,
                        icon = info.loadIcon(pm),
                        isBlocked = false,
                        blockDelay = 0
                    )
                } else null
            }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
