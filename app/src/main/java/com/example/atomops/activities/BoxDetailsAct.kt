package com.example.atomops.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.AtomOpsApp
import com.example.atomops.R
import com.example.atomops.adapters.BoxAdapter
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.databinding.ActivityBoxDetailsBinding
import com.example.atomops.dialogs.BoxIssueBottomSheet
import kotlinx.coroutines.launch

class BoxDetailsAct : AppCompatActivity() {

    private enum class Filter { ALL, SCANNED, UNSCANNED, SHORT }

    lateinit var binding: ActivityBoxDetailsBinding
    private lateinit var adapter: BoxAdapter

    private val allBoxes = mutableListOf<BoxItem>()
    private var currentFilter = Filter.ALL
    private lateinit var docketno: String
    private lateinit var barcodedao: BarcodeDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_box_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        barcodedao = (application as AtomOpsApp).database.getBarcodeDao()
        docketno = intent.getStringExtra("docketno") ?: ""

        setupToolbar()
        setupRecycler()
        setupFilters()
        setupResultListener()
        loadBoxes()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
        // binding.ivSearch.setOnClickListener { /* open search */ }
    }

    private fun setupRecycler() {
        adapter = BoxAdapter { box -> onActionClick(box) }
        binding.rvBoxes.layoutManager = LinearLayoutManager(this)
        binding.rvBoxes.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipScanned -> Filter.SCANNED
                R.id.chipUnscanned -> Filter.UNSCANNED
                R.id.chipShort -> Filter.SHORT
                else -> Filter.ALL
            }
            applyFilter()
        }
    }

    private fun loadBoxes() {
        lifecycleScope.launch {
            val dbBoxes = barcodedao.getAllBox(docketno)

            allBoxes.clear()
            // If getAllBox() already returns List<BoxItem>, just use: allBoxes.addAll(dbBoxes)
            allBoxes.addAll(dbBoxes.map { i ->
                BoxItem(
                    docketno = i.docketno,
                    box = i.box,
                    timeScan = i.timeScan,
                    status = i.status,
                    voided = i.voided,
                    manifest = i.manifest,
                    isshort = i.isshort,
                    damage = i.damage,
                    damageurl = i.damageurl,
                    tripId = i.tripId,
                    isScan = i.isScan
                )
            })

            allBoxes.firstOrNull()?.let { binding.tvTripId.text = it.tripId.toString() }

            refresh()   // must run AFTER the data is loaded
        }
    }

    /** Scanned box -> Damage sheet, unscanned box -> Short sheet, already marked -> undo. */
    private fun onActionClick(box: BoxItem) {
        if (box.status != BoxStatus.NONE) {
            updateStatus(box.box, BoxStatus.NONE)   // undo
            return
        }
        val type = if (box.isScan) BoxStatus.DAMAGE else BoxStatus.SHORT
        BoxIssueBottomSheet.newInstance(box.box, type)
            .show(supportFragmentManager, BoxIssueBottomSheet.TAG)

    }

    /** Receives the result when the user taps Confirm in the bottom sheet. */
    private fun setupResultListener() {
        supportFragmentManager.setFragmentResultListener(
            BoxIssueBottomSheet.REQUEST_KEY, this
        ) { _, bundle ->
            val boxId = bundle.getString(BoxIssueBottomSheet.KEY_BOX_ID).orEmpty()
            val type = bundle.getString(BoxIssueBottomSheet.KEY_TYPE).orEmpty()
            val reason = bundle.getString(BoxIssueBottomSheet.KEY_REASON).orEmpty()
            val remark = bundle.getString(BoxIssueBottomSheet.KEY_REMARK).orEmpty()

            updateStatus(boxId, type)

            // TODO: save reason + remark (and damage photo url) to your database, e.g.
            // lifecycleScope.launch { barcodedao.updateStatus(docketno, boxId, type) }
        }
    }

    private fun updateStatus(boxId: String, newStatus: String) {
        val index = allBoxes.indexOfFirst { it.box == boxId }
        if (index != -1) {
            lifecycleScope.launch {
                barcodedao.updateShortStatus(boxId,newStatus)
            }

            allBoxes[index] = allBoxes[index].copy(status = newStatus)

            refresh()
        }
    }

    private fun refresh() {
        updateSummary()
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> allBoxes
            Filter.SCANNED -> allBoxes.filter { it.isScan }
            Filter.UNSCANNED -> allBoxes.filter { !it.isScan }
            Filter.SHORT -> allBoxes.filter { it.status == BoxStatus.SHORT }
        }
        adapter.submitList(filtered.toList())
    }

    private fun updateSummary() {
        val total = allBoxes.size
        val scanned = allBoxes.count { it.isScan }

        binding.tvScannedCount.text = scanned.toString()
        binding.tvTotalCount.text = "/$total"
        binding.progressLoading.max = total
        binding.progressLoading.progress = scanned

        binding.tvShortCount.text = allBoxes.count { it.status == BoxStatus.SHORT }.toString()
        binding.tvExcessCount.text = allBoxes.count { it.status == BoxStatus.EXCESS }.toString()
        binding.tvDamageCount.text = allBoxes.count { it.status == BoxStatus.DAMAGE }.toString()
    }
}
