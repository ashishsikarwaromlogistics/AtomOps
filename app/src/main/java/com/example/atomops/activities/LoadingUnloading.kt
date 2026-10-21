package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.CustomProgress2
import com.example.atomops.R
import com.example.atomops.adapters.TripAdapter
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
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
    private var query = ""

    // Created once, reused everywhere
    private val api: ServiceInterface by lazy {
        ApiClient.getRetrofitclient().create(ServiceInterface::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= DataBindingUtil. setContentView(this,R.layout.activity_loading_unloading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupToolbar()
        setupTabs()
        setupList()
        setupSearch()
        loadDummyData()   // replace with your API call
        if(Utils.haveInternet(this))
        LoadData() else Utils.showDialog(
            this,
            "error",
            "NO Intenet Connection",R.drawable.ic_baseline_error_24
        )
        binding.ivSearch.setOnClickListener {
            if(binding.etSearch.text.toString().length>10)
            fliterdata(binding.etSearch.text.toString())
            else{
                Utils.showDialog(this,"error","please enter valid trip id",R.drawable.ic_baseline_error_24)
            }
        }
    }
    private fun setupToolbar() {
        cp= CustomProgress2(this)
        binding.ivBack.setOnClickListener { finish() }
        binding.ivSearchTop.setOnClickListener {
            binding.etSearch.requestFocus()
        }
        binding.btnFilter.setOnClickListener {
            Toast.makeText(this, "Filter clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        listOf("All Trips", "Loading", "Unloading", "Completed").forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.text.toString()
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupList() {
        adapter = TripAdapter(
            onItemClick = { trip ->
                 startActivity(Intent(this, TripsDetails::class.java).putExtra("tripid",trip.tripNo))
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

    private fun setupSearch() {
       /* binding.etSearch.doAfterTextChanged {
            query = it?.toString()?.trim().orEmpty()
            applyFilters()
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters(); true
            } else false
        }*/
    }

    private fun applyFilters() {
        val filtered = allTrips.filter { trip ->
            val tabOk = selectedTab == "All Trips" || trip.status.equals(selectedTab, true)
            val searchOk = query.isEmpty() || trip.tripNo.contains(query, true)
            tabOk && searchOk
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility =
            if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }


    private fun LoadData(){
        cp.show()

        lifecycleScope.launch {
            try{
               val resp= api.getTrips(
                   "Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),400021,"CREATED",1001,1,20)
               if (resp.isSuccessful == true && resp.body()?.status == "SUCCESS") {
                   cp.dismiss()
                   val data = resp.body()?.data
                   if (data != null) {
                       // Save tokens securely, then navigate
                       for(i in data.items)
                           allTrips.add(i)
                       applyFilters()
                   } else {
                       showError("Login failed")
                   }
               } else {
                   cp.dismiss()
                   when (resp.code()) {
                       401 -> showError("Invalid user ID or password")
                       403 -> showError("Access denied")
                       in 500..599 -> showError("Server error, try again later")
                       else -> showError("Login failed")
                   }
               }

           }catch (e: Exception){
               cp.dismiss()
               Utils.gettrycatcherror(this@LoadingUnloading,e)
           }
        }

      }
    private fun fliterdata(q :String){
        cp.show()
        lifecycleScope.launch {
          try{ val resp= api.filtertrips(
              "Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),400021,"PLANNED",1001,1,20,q)
              if (resp?.isSuccessful == true && resp.body()?.status == "SUCCESS") {
                  cp.dismiss()
                  allTrips.clear()
                  val data = resp.body()?.data
                  if (data != null) {
                      // Save tokens securely, then navigate
                      if(data.items.size==0){
                          Utils.showDialog(this@LoadingUnloading,"error",resp.body()?.message?:"No data found",R.drawable.ic_baseline_error_24)
                      }
                      for(i in data.items)
                          allTrips.add(i)
                        applyFilters()
                  } else {
                      showError("Login failed")
                  }
              }
              else {
                  cp.dismiss()
                  when (resp?.code()) {
                      401 -> showError("Invalid user ID or password")
                      403 -> showError("Access denied")
                      in 500..599 -> showError("Server error, try again later")
                      else -> showError("Login failed")
                  }
              }}
          catch (e:Exception){
              Utils.gettrycatcherror(this@LoadingUnloading,e)
          }

        }
    }
    private fun loadDummyData() {

        repeat(3) {

        }
        applyFilters()
    }
    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}