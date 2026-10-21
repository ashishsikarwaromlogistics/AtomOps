package com.example.atomops.activities

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.adapters.PrqAdapter
import com.example.atomops.models.DummyPrqData
import com.example.atomops.models.PrqRequest
import com.example.atomops.models.RequestStatus
import com.example.atomops.models.RequestType
import com.google.android.material.tabs.TabLayout

class PrqAct : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tabPickup: TextView
    private lateinit var tabDrop: TextView
    private lateinit var tabStatus: TabLayout

    private lateinit var adapter: PrqAdapter

    // Dummy data (replace with API response later)
    private val pickupList = DummyPrqData.pickupRequests()
    private val dropList = DummyPrqData.dropRequests()

    // Current filter state
    private var currentType = RequestType.PICKUP
    private var selectedStatus: RequestStatus? = null   // null = All Requests
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prq)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupList()
        setupTopBar()
        setupTypeToggle()
        setupStatusTabs()
        setupSearch()

        applyFilters()
    }

    private fun initViews() {
        rvRequests = findViewById(R.id.rvRequests)
        etSearch = findViewById(R.id.etSearch)
        tabPickup = findViewById(R.id.tabPickup)
        tabDrop = findViewById(R.id.tabDrop)
        tabStatus = findViewById(R.id.tabStatus)
    }

    private fun setupList() {
        adapter = PrqAdapter(
            onItemClick = { item ->
                Toast.makeText(this, "Opened ${item.id}", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { anchor, item -> showMoreMenu(anchor, item) }
        )
        rvRequests.adapter = adapter
    }

    private fun setupTopBar() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        // Toolbar search icon just focuses the search box
        findViewById<ImageView>(R.id.ivSearch).setOnClickListener {
            etSearch.requestFocus()
        }
        findViewById<ImageView>(R.id.ivFilter).setOnClickListener {
            Toast.makeText(this, "Filter clicked", Toast.LENGTH_SHORT).show()
        }
        // Chevron scrolls the status tabs to reveal hidden ones
        findViewById<ImageView>(R.id.ivTabsMore).setOnClickListener {
            tabStatus.smoothScrollBy(250, 0)
        }
    }

    // ---------- Pickup / Drop toggle ----------
    private fun setupTypeToggle() {
        tabPickup.setOnClickListener { selectType(RequestType.PICKUP) }
        tabDrop.setOnClickListener { selectType(RequestType.DROP) }
        updateToggleUi()
    }

    private fun selectType(type: RequestType) {
        if (type == currentType) return
        currentType = type
        updateToggleUi()
        applyFilters()
    }

    private fun updateToggleUi() {
        val isPickup = currentType == RequestType.PICKUP
        styleToggle(tabPickup, selected = isPickup)
        styleToggle(tabDrop, selected = !isPickup)
    }

    private fun styleToggle(tab: TextView, selected: Boolean) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected)
            tab.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            tab.background = null
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }

    // ---------- Status tabs: All / Created / Out for Pickup ----------
    private fun setupStatusTabs() {
        tabStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

    // ---------- Search ----------
    private fun setupSearch() {
        etSearch.doAfterTextChanged {
            searchQuery = it?.toString()?.trim().orEmpty()
            applyFilters()
        }
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus()
                true
            } else false
        }
    }

    // ---------- Combine type + status + search ----------
    private fun applyFilters() {
        val source = if (currentType == RequestType.PICKUP) pickupList else dropList

        val result = source.filter { item ->
            val statusOk = selectedStatus == null || item.status == selectedStatus
            val searchOk = searchQuery.isEmpty() ||
                    item.customer.contains(searchQuery, ignoreCase = true) ||
                    item.id.contains(searchQuery, ignoreCase = true)
            statusOk && searchOk
        }
        adapter.submitList(result)
    }

    // ---------- ⋮ menu ----------
    private fun showMoreMenu(anchor: android.view.View, item: PrqRequest) {
        PopupMenu(this, anchor).apply {
            menu.add("View Details")
            menu.add("Call Customer")
            menu.add("Cancel Request")
            setOnMenuItemClickListener { menuItem ->
                Toast.makeText(
                    this@PrqAct,
                    "${menuItem.title}: ${item.id}",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            show()
        }
    }
}
