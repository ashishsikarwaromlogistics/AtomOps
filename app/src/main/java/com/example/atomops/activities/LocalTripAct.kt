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
          startActivity(Intent(this,LocalLoading::class.java).putExtra("tripId",it.trip_no))
    }) }
    private var allTrips: List<Item> = emptyList()
    private var selectedTab = "All Trips"
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

        listOf("All Trips", "Loading", "Unloading", "Completed").forEach {
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
                    Constants.COMP_CODE,""), loader = "29658")
                if(res.code()==200){
                    allTrips = res.body()!!.data?.items!!
                    updateSummary()
                    applyFilter()
                }
                else {
                    Utils.handleHttpError(this@LocalTripAct,res.code())
                }

            } catch (e: Exception) {
                Toast.makeText(this@LocalTripAct, e.localizedMessage ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                DismissDialog()
            }
        }
    }

    private fun updateSummary() {
        fun count(s: String) = "%02d".format(allTrips.count { it.trip_status.equals(s, true) })
        binding.tvTotal.text = "%02d".format(allTrips.size)
        binding.tvLoading.text = count("Loading")
        binding.tvUnloading.text = count("Unloading")
        binding.tvCompleted.text = count("Completed")
    }

    private fun applyFilter() {
        val q = binding.etSearch.text.toString().trim()
        tripAdapter.submitList(
            allTrips.filter {
                (selectedTab == "All Trips" || it.trip_status.equals(selectedTab, true)) &&
                        it.trip_no.contains(q, ignoreCase = true)
            }
        )
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}