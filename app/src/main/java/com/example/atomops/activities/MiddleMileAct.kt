package com.example.atomops.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.R
import com.example.atomops.adapters.StatAdapter
import com.example.atomops.adapters.TripAdapter
import com.example.atomops.adapters.TripAdapter1
import com.example.atomops.databinding.ActivityMiddleMileBinding
import com.example.atomops.models.dummy.Trip
import com.example.atomops.models.dummy.TripStat
import com.example.atomops.models.dummy.TripStatus

class MiddleMileAct : AppCompatActivity() {

    private lateinit var binding: ActivityMiddleMileBinding

    private val statAdapter = StatAdapter { stat -> onStatClicked(stat) }
    private val tripAdapter = TripAdapter1(
        onMenuClick = { /* show popup menu */ },
        onItemClick = { /* open trip details */ }
    )

    private var allTrips: List<Trip> = emptyList()
    private var selectedStatus: TripStatus? = null   // null = all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_middle_mile)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.updatePadding(top = bars.top + 12.dp)
            binding.rvTrips.updatePadding(bottom = bars.bottom + 16.dp)
            insets
        }

        setupStats()
        setupTrips()
        setupSearch()
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupStats() {
        binding.rvStats.layoutManager = GridLayoutManager(this, 3)
        binding.rvStats.adapter = statAdapter
        statAdapter.submitList(
            listOf(
                TripStat("Total Trips", 250, null),
                TripStat("Created", 15, TripStatus.CREATED),
                TripStat("Dispatch", 5, TripStatus.DISPATCH),
                TripStat("Gate Out", 150, TripStatus.GATE_OUT),
                TripStat("Gate In", 25, TripStatus.GATE_IN),
                TripStat("Completed", 50, TripStatus.COMPLETED),
                TripStat("Cancelled", 5, TripStatus.CANCELLED)
            )
        )
    }

    private fun setupTrips() {
        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = tripAdapter

        allTrips = listOf(
            sampleTrip("TRIP123456725", TripStatus.CREATED),
            sampleTrip("TRIP123456726", TripStatus.GATE_OUT),
            sampleTrip("TRIP123456727", TripStatus.DISPATCH),
            sampleTrip("TRIP123456728", TripStatus.GATE_IN),
            sampleTrip("TRIP123456729", TripStatus.COMPLETED),
            sampleTrip("TRIP123456730", TripStatus.CANCELLED)
        )
        applyFilters()
    }

    private fun sampleTrip(id: String, status: TripStatus) = Trip(
        id = id,
        createdAt = "14-01-2025, 14:30",
        status = status,
        stops = listOf("MUM-MHBI-006", "PUN-MHBI-007", "DEL-MHBI-008"),
        serviceTime = "21:30",
        dock = "2A",
        manifestCount = 2,
        vehicleNo = "DL14CC1589",
        ownership = "Owned",
        remainingKg = 1400
    )

    private fun onStatClicked(stat: TripStat) {
        selectedStatus = stat.status
        statAdapter.selected = stat.status
        applyFilters()
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
        tripAdapter.submitList(
            allTrips.filter {
                (selectedStatus == null || it.status == selectedStatus) &&
                        (q.isEmpty() || it.id.contains(q, ignoreCase = true))
            }
        )
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}