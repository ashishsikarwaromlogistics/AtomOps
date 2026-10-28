package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.CustomProgress2
import com.example.atomops.R
import com.example.atomops.adapters.TripAdapter
import com.example.atomops.databinding.ActivityLoadingUnloadingBinding
import com.example.atomops.models.trips.Item
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class LoadingUnloading : AppCompatActivity() {
    lateinit var binding: ActivityLoadingUnloadingBinding
    lateinit var cp: CustomProgress2
    private lateinit var adapter: TripAdapter

    private val allTrips = mutableListOf<Item>()
    private var selectedTab = "All Trips"
    private var selectedStatus = "CREATED"
    private var query = ""
    private var currentstatus="ALL"

    // Tab title -> status value sent to the API (adjust to what your backend expects)
    private val tabStatusMap = linkedMapOf(
        "All Trips" to "CREATED",
        "Loading" to "CREATED",
        "Unloading" to "GATE_IN",
        "Completed" to "COMPLETED"
    )

    // Created once, reused everywhere
    private val api: ServiceInterface by lazy {
        ApiClient.getRetrofitclient().create(ServiceInterface::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_loading_unloading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupToolbar()
        setupTabs()
        setupList()

        // Initial load (first tab). The tab listener is added after the first tab
        // is selected, so it does not fire a duplicate call.
        loadIfOnline(selectedStatus)

        binding.ivSearch.setOnClickListener {
            val text = binding.etSearch.text.toString()
            if (text.length > 10) {
                if (Utils.haveInternet(this)) {
                    fliterdata(text)
                } else {
                    showNoInternet()
                }
            } else {
                Utils.showDialog(
                    this, "error", "please enter valid trip id",
                    R.drawable.ic_baseline_error_24
                )
            }
        }
    }

    private fun setupToolbar() {
        cp = CustomProgress2(this)
        binding.ivBack.setOnClickListener { finish() }
        binding.ivSearchTop.setOnClickListener {
            binding.etSearch.requestFocus()
        }
        binding.btnFilter.setOnClickListener {
            Toast.makeText(this, "Filter clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabStatusMap.keys.forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.text.toString()
                selectedStatus = tabStatusMap[selectedTab] ?: "CREATED"

                // reset search when switching tabs
                binding.etSearch.text?.clear()
                query = ""

                // API hit on tab select
                loadIfOnline(selectedStatus)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                // tap the same tab again to refresh
                onTabSelected(tab)
            }
        })
    }

    private fun setupList() {
        adapter = TripAdapter(
            onItemClick = { trip ->
                startActivity(
                    Intent(this, TripsDetails::class.java).putExtra("currentstatus",currentstatus).putExtra("tripid", trip.tripNo)
                )
                Toast.makeText(this, trip.tripNo, Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { trip, anchor ->
                PopupMenu(this, anchor).apply {
                    menu.add("View Details")
                    menu.add("Cancel Trip")
                    setOnMenuItemClickListener {
                        Toast.makeText(
                            this@LoadingUnloading,
                            "${it.title}: ${trip.tripNo}",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    show()
                }
            }
        )
        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = adapter
    }

    // Server already returns trips for the selected status,
    // so only the local search text is applied here.
    private fun applyFilters() {
        val filtered = allTrips.filter { trip ->
            query.isEmpty() || trip.tripNo.contains(query, true)
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadIfOnline(status: String) {
        if (Utils.haveInternet(this)) {
            currentstatus=status
            LoadData(status)
        } else {
            showNoInternet()
        }
    }

    private fun showNoInternet() {
        Utils.showDialog(
            this, "error", "NO Internet Connection",
            R.drawable.ic_baseline_error_24
        )
    }

    private fun LoadData(status: String) {
        cp.show()

        lifecycleScope.launch {
            try {
                val resp = api.getTrips(
                    "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    400021,
                    status,
                    AtomOpsApp.getPreferences(Constants.BCODE, "").toInt(),
                    1,
                    20
                )
                cp.dismiss()
                if (resp.isSuccessful && resp.body()?.status == "SUCCESS") {
                    allTrips.clear()
                    val data = resp.body()?.data
                    if (data != null) {
                        allTrips.addAll(data.items)
                        applyFilters()
                    } else {
                        showError("No data received")
                    }
                } else {
                    handleError(resp.code())
                }
            } catch (e: Exception) {
                cp.dismiss()
                Utils.gettrycatcherror(this@LoadingUnloading, e)
            }
        }
    }

    private fun fliterdata(q: String) {
        cp.show()
        lifecycleScope.launch {
            try {
                val resp = api.filtertrips(
                    "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    400021,
                    selectedStatus,
                    AtomOpsApp.getPreferences(Constants.BCODE, "").toInt(),
                    1,
                    20,
                    q
                )
                cp.dismiss()
                if (resp.isSuccessful && resp.body()?.status == "SUCCESS") {
                    allTrips.clear()
                    val data = resp.body()?.data
                    if (data != null) {
                        if (data.items.isEmpty()) {
                            Utils.showDialog(
                                this@LoadingUnloading,
                                "error",
                                resp.body()?.message ?: "No data found",
                                R.drawable.ic_baseline_error_24
                            )
                        }
                        allTrips.addAll(data.items)
                        applyFilters()
                    } else {
                        showError("No data received")
                    }
                } else {
                    handleError(resp.code())
                }
            } catch (e: Exception) {
                cp.dismiss()
                Utils.gettrycatcherror(this@LoadingUnloading, e)
            }
        }
    }

    private fun handleError(code: Int) {
        when (code) {
            401 -> showError("Session expired, please login again")
            403 -> showError("Access denied")
            in 500..599 -> showError("Server error, try again later")
            else -> showError("Something went wrong")
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}