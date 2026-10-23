package com.example.atomops.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.R
import com.example.atomops.adapters.RequestAdapter
import com.example.atomops.databinding.ActivityReqMngmtBinding
import com.example.atomops.models.dummy.PickupRequest
import com.example.atomops.models.dummy.RequestStatus
import com.google.android.material.tabs.TabLayout

class ReqMngmtAct : AppCompatActivity() {

    private lateinit var binding: ActivityReqMngmtBinding
    private val adapter = RequestAdapter(
        onMenuClick = { /* show popup menu */ },
        onItemClick = { /* open details */ }
    )

    private var allRequests: List<PickupRequest> = emptyList()
    private var selectedStatus: RequestStatus? = null   // null = All

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_req_mngmt)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.updatePadding(top = bars.top + 12.dp)   // teal bar draws under status bar
            binding.rvRequests.updatePadding(bottom = bars.bottom + 16.dp)
            insets
        }

        setupTabs()
        setupToggle()
        setupList()
        setupSearch()
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupList() {
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter

        allRequests = listOf(
            sample("PRQ87S8785255", RequestStatus.CREATED),
            sample("PRQ87S8785256", RequestStatus.OUT_FOR_PICKUP),
            sample("PRQ87S8785257", RequestStatus.OUT_FOR_PICKUP)
        )
        applyFilters()
    }

    private fun sample(id: String, status: RequestStatus) = PickupRequest(
        id = id,
        createdAt = "14-01-2025, 14:30",
        status = status,
        customer = "CUS789656 - Creative Wealth Management Pvt. Ltd.",
        location = "110064 (Mayapuri, Delhi)",
        phone = "8291658458",
        pickupSlot = "12-03-2026  |  09:00 - 11:00",
        loadType = "Part Truck Load (PTL)"
    )

    private fun setupTabs() = with(binding.tabLayout) {
        addTab(newTab().setText("All Requests"))
        addTab(newTab().setText("Created"))
        addTab(newTab().setText("Out for Pickup"))
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedStatus = when (tab.position) {
                    1 -> RequestStatus.CREATED
                    2 -> RequestStatus.OUT_FOR_PICKUP
                    else -> null
                }
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupToggle() {
        fun select(pickup: Boolean) {
            binding.tabPickup.setBackgroundResource(if (pickup) R.drawable.bg_toggle_selected else 0)
            binding.tabDrop.setBackgroundResource(if (!pickup) R.drawable.bg_toggle_selected else 0)
            binding.tabPickup.setTextColor(getColor(if (pickup) android.R.color.white else R.color.text_primary))
            binding.tabDrop.setTextColor(getColor(if (!pickup) android.R.color.white else R.color.text_primary))
            // TODO: load pickup / drop data here
        }
        binding.tabPickup.setOnClickListener { select(true) }
        binding.tabDrop.setOnClickListener { select(false) }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { applyFilters() }
    }

    private fun applyFilters() {
        val q = binding.etSearch.text?.toString().orEmpty().trim()
        adapter.submitList(
            allRequests.filter {
                (selectedStatus == null || it.status == selectedStatus) &&
                        (q.isEmpty() || it.customer.contains(q, ignoreCase = true))
            }
        )
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}