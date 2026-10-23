package com.example.atomops.activities

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.R
import com.example.atomops.adapters.DockAdapter
import com.example.atomops.adapters.DockStatAdapter
import com.example.atomops.databinding.ActivityDockMgmtBinding
import com.example.atomops.models.dummy.Dock
import com.example.atomops.models.dummy.DockDirection
import com.example.atomops.models.dummy.DockSlot
import com.example.atomops.models.dummy.DockStat
import com.example.atomops.models.dummy.SlotStatus

class DockMgmtAct : AppCompatActivity() {

    private lateinit var binding: ActivityDockMgmtBinding

    private val statAdapter = DockStatAdapter()
    private val dockAdapter = DockAdapter(
        onSlotClick = { _, _ -> /* open slot details */ },
        onDockClick = { /* open dock details */ }
    )

    private var allDocks: List<Dock> = emptyList()
    private var slotFilter: SlotStatus? = null     // null = All

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dock_mgmt)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.updatePadding(top = bars.top + 12.dp)
            binding.scroll.updatePadding(bottom = bars.bottom)
            insets
        }

        setupStats()
        setupDocks()
        setupToggle()
        setupChips()
        binding.ivBack.setOnClickListener { finish() }
        binding.cardLive.setOnClickListener { /* open live dock status */ }
    }

    private fun setupStats() {
        binding.rvStats.layoutManager = GridLayoutManager(this, 3)
        binding.rvStats.adapter = statAdapter
        statAdapter.submitList(
            listOf(
                DockStat("Total Docks", 24, R.color.dot_gate_out),
                DockStat("Available Docks", 4, R.color.dot_gate_in),
                DockStat("Queued Vehicles", 1, R.color.dot_completed)
            )
        )
    }

    private fun setupDocks() {
        binding.rvDocks.layoutManager = LinearLayoutManager(this)
        binding.rvDocks.adapter = dockAdapter
        allDocks = dummyDocks()
        applyFilters()
    }

    private fun setupToggle() {
        fun select(dock: Boolean) {
            style(binding.tabDock, dock)
            style(binding.tabQueue, !dock)
            // TODO: show Queue Overview list when it is designed
        }
        binding.tabDock.setOnClickListener { select(true) }
        binding.tabQueue.setOnClickListener { select(false) }
    }

    private fun style(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(if (selected) R.drawable.bg_toggle_selected else 0)
        tv.setTextColor(getColor(if (selected) android.R.color.white else R.color.text_primary))
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            slotFilter = when (ids.firstOrNull()) {
                R.id.chipLoading -> SlotStatus.LOADING
                R.id.chipUnloading -> SlotStatus.UNLOADING
                R.id.chipAvailable -> SlotStatus.AVAILABLE
                else -> null
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val f = slotFilter
        dockAdapter.submitList(
            if (f == null) allDocks
            else allDocks.filter { dock -> dock.slots.any { it.status == f } }
        )
    }

    // ---------- Dummy data ----------
    private fun dummyDocks(): List<Dock> {
        fun slot(letter: String, status: SlotStatus, vehicle: String? = null, progress: Int = 0) =
            DockSlot("$letter. Dock Slot", status, vehicle, progress)

        return listOf(
            Dock(
                "D01", "Dock 01", DockDirection.OUTBOUND,
                listOf(
                    slot("A", SlotStatus.LOADING, "MH12AB4521", 65),
                    slot("B", SlotStatus.LOADING, "DL14CC1589", 20),
                    slot("C", SlotStatus.AVAILABLE)
                )
            ),
            Dock(
                "D02", "Dock 02", DockDirection.OUTBOUND,
                listOf(
                    slot("A", SlotStatus.AVAILABLE),
                    slot("B", SlotStatus.INACTIVE),
                    slot("C", SlotStatus.INACTIVE)
                )
            ),
            Dock(
                "D03", "Dock 03", DockDirection.INBOUND,
                listOf(
                    slot("A", SlotStatus.UNLOADING, "HR55XY9087", 40),
                    slot("B", SlotStatus.AVAILABLE),
                    slot("C", SlotStatus.INACTIVE)
                )
            ),
            Dock(
                "D04", "Dock 04", DockDirection.INBOUND,
                listOf(
                    slot("A", SlotStatus.UNLOADING, "UP16CD3345", 80),
                    slot("B", SlotStatus.UNLOADING, "RJ14EF7712", 10),
                    slot("C", SlotStatus.UNLOADING, "GJ05LM1198", 55)
                )
            ),
            Dock(
                "D05", "Dock 05", DockDirection.OUTBOUND,
                listOf(
                    slot("A", SlotStatus.AVAILABLE),
                    slot("B", SlotStatus.AVAILABLE),
                    slot("C", SlotStatus.AVAILABLE)
                )
            ),
            Dock(
                "D06", "Dock 06", DockDirection.INBOUND,
                listOf(
                    slot("A", SlotStatus.AVAILABLE),
                    slot("B", SlotStatus.UNLOADING, "PB10JK6643", 30),
                    slot("C", SlotStatus.AVAILABLE)
                )
            )
        )
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}