package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.AppProgressDialog
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoadingUnloading : AppCompatActivity() {
    lateinit var binding: ActivityLoadingUnloadingBinding
    lateinit var cp: CustomProgress2
    private lateinit var adapter: TripAdapter

    private val allTrips = mutableListOf<Item>()
    private var selectedTab = "All Trips"
    private var selectedStatus = ""
    private var query = ""
    private var currentstatus="ALL"

    // ---- pagination ----
    private val PAGE_SIZE = 20           // set to 5 temporarily to test paging with few trips
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var totalTrips = 0           // "total" from the API response
    private var searchText = ""          // "" = normal list, otherwise filter API with this text
    private var loadJob: Job? = null     // cancelled when a new page-1 load starts (tab switch / search)
    private var requestId = 0            // only the latest request may update the list / loaders
    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }
    private fun ShowDailog(){
        if(!appprogress.isShowing){
            appprogress.show()
        }
    }
    private fun DismissDialog(){
        if(appprogress.isShowing){
            appprogress.dismiss()
        }
    }

    // Tab title -> status value sent to the API (adjust to what your backend expects)
    private val tabStatusMap = linkedMapOf(
        "All Trips" to "",
        "Loading" to "CREATED",
        "Unloading" to "GATE_IN",
        "PLANNED" to "PLANNED",
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
        binding.ivBack.setOnClickListener { finish() }
        // Initial load (first tab). The tab listener is added after the first tab
        // is selected, so it does not fire a duplicate call.
        loadIfOnline(selectedStatus)

        binding.ivSearch.setOnClickListener {
            val text = binding.etSearch.text.toString()
            if (text.length > 10) {
                if (Utils.haveInternet(this)) {
                    searchText = text
                    fliterdata(text, 1)
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


        binding.etSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    loadIfOnline(selectedStatus)
                }
            }
        })


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
                if(trip.status.equals("CANCELLED")){
                    Utils.showDialog(this@LoadingUnloading,"error","This is Cancelled Trip",R.drawable.ic_baseline_error_24)
                }
                else if(trip.status.equals("COMPLETED")){
                    Utils.showDialog(this@LoadingUnloading,"error","Already Completed Trip",R.drawable.ic_baseline_error_24)

                }
                else if(trip.status.equals("PLANNED")){
                    startActivity(
                        Intent(this, TripsDetails::class.java).putExtra("currentstatus","PLANNED").putExtra("tripid", trip.tripNo)
                    )
                }

                else if(trip.status.equals("GATE_IN")){
                    startActivity(
                        Intent(this, TripsDetails::class.java).putExtra("currentstatus","GATE_IN").putExtra("tripid", trip.tripNo)
                    )
                }
                else
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
        val lm = LinearLayoutManager(this)
        binding.rvTrips.layoutManager = lm
        binding.rvTrips.adapter = adapter

        // Load the next page when the user is 5 rows from the bottom
        binding.rvTrips.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoading || isLastPage) return
                val lastVisible = lm.findLastVisibleItemPosition()
                if (lastVisible >= lm.itemCount - 5 || !rv.canScrollVertically(1)) loadNextPage()
            }
        })
    }

    private fun loadNextPage() {
        if (!Utils.haveInternet(this)) return
        if (searchText.isEmpty()) LoadData(selectedStatus, currentPage + 1)
        else fliterdata(searchText, currentPage + 1)
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
            searchText = ""
            LoadData(status, 1)
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

    /** page 1 = fresh list (tab / pull), page > 1 = append (scroll). */
    private fun LoadData(status: String, page: Int = 1) {
        if (page > 1 && (isLoading || isLastPage)) return
        startLoading(page)
        val myId = ++requestId

        loadJob = lifecycleScope.launch {
            try {
                val auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, "")
                val comp = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toIntOrNull() ?: 0
                val branch = AtomOpsApp.getPreferences(Constants.BCODE, "").toIntOrNull() ?: 0

                Log.d("Pagination", "LoadData status='$status' page=$page size=$PAGE_SIZE")
                val resp = if (status.equals("")) api.getAllTrips(auth, comp, branch, page, PAGE_SIZE)
                else api.getTrips(auth, comp, status, branch, page, PAGE_SIZE)

                if (myId != requestId) return@launch   // a newer request (tab/search) replaced this one
                Log.d("Pagination", "response page=$page code=${resp.code()}")

                if (resp.isSuccessful) {
                    val data = resp.body()?.data
                    if (data != null) {
                        onPageLoaded(page, data.items, data.total)
                    } else if (page == 1) {
                        onPageLoaded(page, emptyList(), 0)
                        Utils.showDialog(this@LoadingUnloading, "error", "No data found", R.drawable.ic_baseline_error_24)
                    }
                } else {
                    try{
                        Utils.showDialog(this@LoadingUnloading, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LoadingUnloading,e)
                    }
                }

            } catch (e: CancellationException) {
                throw e                          // tab switched: old request dropped, no error
            } catch (e: Exception) {
                Toast.makeText(this@LoadingUnloading, e.localizedMessage ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                if (myId == requestId) stopLoading()   // a cancelled old request must not hide the new loader
            }
        }
    }

    private fun fliterdata(q: String, page: Int = 1) {
        if (page > 1 && (isLoading || isLastPage)) return
        startLoading(page)
        val myId = ++requestId

        loadJob = lifecycleScope.launch {
            try {
                val auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, "")
                val comp = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toIntOrNull() ?: 0
                val branch = AtomOpsApp.getPreferences(Constants.BCODE, "").toIntOrNull() ?: 0

                Log.d("Pagination", "fliterdata q='$q' status='$selectedStatus' page=$page size=$PAGE_SIZE")
                val resp = if (selectedStatus.equals("")) api.Allfiltertrips(auth, comp, branch, page, PAGE_SIZE, q)
                else api.filtertrips(auth, comp, selectedStatus, branch, page, PAGE_SIZE, q)

                if (myId != requestId) return@launch   // a newer request (tab/search) replaced this one
                Log.d("Pagination", "response page=$page code=${resp.code()}")

                if (resp.isSuccessful) {
                    val data = resp.body()?.data
                    if (data != null) {
                        if (page == 1 && data.items.isEmpty()) {
                            Utils.showDialog(
                                this@LoadingUnloading, "error",
                                resp.body()?.message?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: "No data found",
                                R.drawable.ic_baseline_error_24
                            )
                        }
                        onPageLoaded(page, data.items, data.total)
                    } else if (page == 1) {
                        onPageLoaded(page, emptyList(), 0)
                        Utils.showDialog(this@LoadingUnloading, "error", "No data found", R.drawable.ic_baseline_error_24)
                    }
                } else {
                    try{
                        Utils.showDialog(this@LoadingUnloading, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LoadingUnloading,e)
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@LoadingUnloading, e.localizedMessage ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                if (myId == requestId) stopLoading()   // a cancelled old request must not hide the new loader
            }
        }
    }

    // ---- pagination helpers ----
    private fun startLoading(page: Int) {
        if (page == 1) {
            loadJob?.cancel()            // drop an older request (e.g. previous tab)
            currentPage = 1
            isLastPage = false
            cp.show()                    // full-screen loader for the first page
        } else {
            binding.pbLoadMore.visibility = View.VISIBLE   // small loader at the bottom
        }
        isLoading = true
    }

    private fun stopLoading() {
        isLoading = false
        cp.dismiss()
        binding.pbLoadMore.visibility = View.GONE
    }

    private fun onPageLoaded(page: Int, items: List<Item>, total: Int) {
        if (page == 1) allTrips.clear()
        // skip duplicates in case the server list shifted between pages
        val existing = allTrips.mapTo(HashSet()) { it.tripNo }
        allTrips.addAll(items.filter { it.tripNo !in existing })
        currentPage = page
        totalTrips = total
        // e.g. total = 13, size = 20 -> page 1 already has everything -> no more calls
        //      total = 45, size = 20 -> pages 1, 2, 3 are loaded, then it stops
        isLastPage = items.isEmpty() || page * PAGE_SIZE >= total
        applyFilters()
        Log.d("Pagination", "loaded page=$page got=${items.size} total=$total shown=${allTrips.size} lastPage=$isLastPage")

        // If the first page doesn't fill the screen the user can't scroll, so the
        // scroll listener never fires -> load the next page automatically.
        binding.rvTrips.post {
            if (!isLoading && !isLastPage && !binding.rvTrips.canScrollVertically(1)) loadNextPage()
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