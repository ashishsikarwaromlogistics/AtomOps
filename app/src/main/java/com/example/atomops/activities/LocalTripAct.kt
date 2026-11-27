package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.AppProgressDialog
import com.example.atomops.R
import com.example.atomops.databinding.ActivityLocalTripBinding
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.adapters.LocalTripAdapter
import com.example.atomops.models.localtrips.Item
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayout

class LocalTripAct : AppCompatActivity() {
    lateinit var binding: ActivityLocalTripBinding
    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }

    private val tripAdapter by lazy { LocalTripAdapter(onItemClick = {
        //  startActivity(Intent(this,LocalLoading::class.java).putExtra("tripId",it.trip_no))

        if(it.trip_status.equals("CREATED",true)){
            startActivity(Intent(this,LocalLoading::class.java).putExtra("tripId",it.trip_no))
        }
        else if(it.trip_status.equals("GATE_IN",true) || it.trip_status.equals("CLOUSER_PENDING",true))
            startActivity(Intent(this,LocalUnLoading::class.java).putExtra("tripId",it.trip_no))
        else {
            Utils.showDialog(this, "error", "Invalid Trip Status "+it.trip_status, R.drawable.ic_baseline_error_24)
        }
    }) }
    private var allTrips: List<Item> = emptyList()
    private var selectedTab = "All Trips"

    // Tab title -> trip_status values shown in that tab (empty list = all trips)
    private val tabStatusMap = linkedMapOf(
        "All Trips" to emptyList(),
        "Loading"   to listOf("CREATED"),
        "Unloading" to listOf("GATE_IN", "CLOUSER_PENDING"),
        "Completed" to listOf("COMPLETED"),
        "Cancelled" to listOf("CANCELLED")
    )
    // TODO: replace "YOUR_TOKEN" with your stored access token (SharedPreferences etc.)
    private val serviceinterface by lazy { ApiClient.getRetrofitclient().create(ServiceInterface::class.java)  }

    private fun ShowDailog() { if (!appprogress.isShowing) appprogress.show() }
    private fun DismissDialog() { if (appprogress.isShowing) appprogress.dismiss() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_local_trip)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            // let the gradient header draw behind the status bar
            binding.header.setPadding(
                binding.header.paddingLeft, bars.top + 12.dp(),
                binding.header.paddingRight, binding.header.paddingBottom
            )
            insets
        }

        setupUi()
        loadTrips()
    }

    private fun setupUi() {
        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = tripAdapter

        binding.ivBack.setOnClickListener { finish() }
        binding.ivFilter.setOnClickListener {
            Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show()
        }

        tabStatusMap.keys.forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.text.toString(); applyFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.etSearch.doAfterTextChanged { applyFilter() }
    }

    private fun loadTrips() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val res = serviceinterface.getTrips(auth = "Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""), companyCode = AtomOpsApp.getPreferences(
                    Constants.COMP_CODE,""), loader = AtomOpsApp.getPreferences(Constants.LOADER,""))
                /* if(res.code()==200){
                     allTrips = res.body()!!.data?.items!!
                     updateSummary()
                     applyFilter()
                 }
                 else {
                     Utils.handleHttpError(this@LocalTripAct,res.code())
                 }*/
                if (res.isSuccessful) {
                    val items = res.body()?.data?.items
                    if (items != null) {
                        allTrips = items
                        updateSummary()
                        applyFilter()
                    } else {
                        allTrips = emptyList()
                        updateSummary()          // counts show 00, not old/placeholder values
                        applyFilter()
                        Utils.showDialog(this@LocalTripAct, "error", "No data found", R.drawable.ic_baseline_error_24)
                    }
                } else {
                    try{
                        Utils.showDialog(this@LocalTripAct, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LocalTripAct,e)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@LocalTripAct, e.localizedMessage ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                DismissDialog()
            }
        }
    }

    // Summary card counts (always from the full list, not the filtered one)
    private fun updateSummary() {
        fun count(tab: String): String {
            val statuses = tabStatusMap[tab].orEmpty()
            return "%02d".format(allTrips.count { trip -> statuses.any { trip.trip_status.equals(it, true) } })
        }
        binding.tvTotal.text     = "%02d".format(allTrips.size)   // all trips
        binding.tvLoading.text   = count("Loading")      // CREATED
        binding.tvUnloading.text = count("Unloading")    // GATE_IN + CLOUSER_PENDING
        binding.tvCompleted.text = count("Completed")    // COMPLETED
    }

    private fun applyFilter() {
        val q = binding.etSearch.text.toString().trim()
        val statuses = tabStatusMap[selectedTab].orEmpty()
        tripAdapter.submitList(
            allTrips.filter {
                (statuses.isEmpty() || statuses.any { s -> it.trip_status.equals(s, true) }) &&
                        it.trip_no.contains(q, ignoreCase = true)
            }
        )
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}