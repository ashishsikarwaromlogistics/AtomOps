package com.example.atomops.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.R
import com.example.atomops.adapters.GateTripAdapter
import com.example.atomops.databinding.ActivityGateOperationBinding
import com.example.atomops.models.dummy.GateMode
import com.example.atomops.models.dummy.GateTrip
import com.example.atomops.models.dummy.TripStatus
import com.google.android.material.tabs.TabLayout

class GateOperationAct : AppCompatActivity() {

    private lateinit var binding: ActivityGateOperationBinding

    private val adapter = GateTripAdapter(
        onActionClick = { /* mark gate-in / gate-out */ },
        onMenuClick = { /* popup menu */ },
        onItemClick = { /* open details */ }
    )

    private var allTrips: List<GateTrip> = emptyList()
    private var mode = GateMode.GATE_IN
    private var showDone = false          // false = Upcoming, true = Checked-In/Out

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_gate_operation)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.updatePadding(top = bars.top + 12.dp)
            binding.rvTrips.updatePadding(bottom = bars.bottom + 16.dp)
            insets
        }

        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = adapter
        allTrips = dummyTrips()

        setupToggle()
        setupTabs()
        setupSearch()
        binding.ivBack.setOnClickListener { finish() }
        applyFilters()
    }

    private fun setupToggle() {
        binding.tabGateIn.setOnClickListener { selectMode(GateMode.GATE_IN) }
        binding.tabGateOut.setOnClickListener { selectMode(GateMode.GATE_OUT) }
        selectMode(GateMode.GATE_IN)
    }

    private fun selectMode(newMode: GateMode) {
        mode = newMode
        val inSel = newMode == GateMode.GATE_IN
        styleToggle(binding.tabGateIn, inSel)
        styleToggle(binding.tabGateOut, !inSel)

        // 2nd tab label changes with mode
        binding.tabLayout.getTabAt(1)?.text = if (inSel) "Checked-In" else "Checked-Out"
        applyFilters()
    }

    private fun styleToggle(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(if (selected) R.drawable.bg_toggle_selected else 0)
        val color = getColor(if (selected) android.R.color.white else R.color.text_primary)
        tv.setTextColor(color)
        TextViewCompat.setCompoundDrawableTintList(tv, ColorStateList.valueOf(color))
    }

    private fun setupTabs() = with(binding.tabLayout) {
        addTab(newTab().setText("Upcoming"))
        addTab(newTab().setText("Checked-In"))
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showDone = tab.position == 1
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilters()
        })
    }

    private fun applyFilters() {
        val q = binding.etSearch.text?.toString().orEmpty().trim()
        adapter.submitList(
            allTrips.filter {
                it.mode == mode &&
                        it.isDone == showDone &&
                        (q.isEmpty() || it.vehicleNo.contains(q, ignoreCase = true))
            }
        )
    }

    // ---------- Dummy data ----------
    private fun dummyTrips(): List<GateTrip> {
        fun trip(
            id: String, mode: GateMode, done: Boolean, status: TripStatus,
            vehicle: String, driver: String, eta: String
        ) = GateTrip(
            id = id,
            createdAt = "14-01-2025, 14:30",
            status = status,
            stops = listOf("MUM-MHBI-006", "PUN-MHBI-007", "DEL-MHBI-008"),
            serviceName = "Service",
            manifestId = "MAN-1234-12",
            driverName = driver,
            vehicleNo = vehicle,
            volWt = 500,
            actWt = 700,
            eta = eta,
            mode = mode,
            isDone = done
        )

        return listOf(
            // Gate-In: upcoming
            trip("TRIP123456725", GateMode.GATE_IN, false, TripStatus.GATE_OUT, "DL14CC1589", "Kamal Singh", "12 Hours"),
            trip("TRIP123456726", GateMode.GATE_IN, false, TripStatus.GATE_OUT, "MH12AB4521", "Rohit Verma", "8 Hours"),
            trip("TRIP123456727", GateMode.GATE_IN, false, TripStatus.GATE_OUT, "HR55XY9087", "Amit Kumar", "3 Hours"),
            // Gate-In: checked-in
            trip("TRIP123456728", GateMode.GATE_IN, true, TripStatus.GATE_IN, "UP16CD3345", "Sunil Yadav", "Arrived"),
            trip("TRIP123456729", GateMode.GATE_IN, true, TripStatus.GATE_IN, "RJ14EF7712", "Manoj Sharma", "Arrived"),
            // Gate-Out: upcoming
            trip("TRIP123456730", GateMode.GATE_OUT, false, TripStatus.DISPATCH, "DL01GH2210", "Pawan Kumar", "2 Hours"),
            trip("TRIP123456731", GateMode.GATE_OUT, false, TripStatus.DISPATCH, "PB10JK6643", "Gurpreet Singh", "5 Hours"),
            // Gate-Out: checked-out
            trip("TRIP123456732", GateMode.GATE_OUT, true, TripStatus.COMPLETED, "GJ05LM1198", "Ravi Patel", "Departed")
        )
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}