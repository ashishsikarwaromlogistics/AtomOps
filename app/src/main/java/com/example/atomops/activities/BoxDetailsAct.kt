package com.example.atomops.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
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

    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }

    private fun ShowDailog() {
        if (!appprogress.isShowing) {
            appprogress.show()
        }
    }

    private fun DismissDialog() {
        if (appprogress.isShowing) {
            appprogress.dismiss()
        }
    }
    var tripId=""
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
        tripId = intent.getStringExtra("tripId") ?: ""
        binding.tvTripId.text=tripId
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

    /**
     * Scanned box  -> open Damage sheet, or undo Damage if already marked.
     * Unscanned box -> mark Short, or undo Short if already marked.
     */
    private fun onActionClick(box: BoxItem) {
        // Already damaged -> undo
        if (box.damage == BoxStatus.DAMAGE) {
            updateStatus(box.box, BoxStatus.NONE, "DAMAGE")
            return
        }

        // Already short -> undo
        if (box.isshort == BoxStatus.SHORT) {
            updateStatus(box.box, BoxStatus.NONE, "SHORT")
            return
        }

        // Scanned and no damage yet -> open the damage bottom sheet
        if (box.isScan) {
            BoxIssueBottomSheet.newInstance(box.box, BoxStatus.DAMAGE)
                .show(supportFragmentManager, BoxIssueBottomSheet.TAG)
            return
        }

        // Unscanned, not short, not damaged -> mark short
        updateStatus(box.box, BoxStatus.SHORT, "")
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
            val photoId = bundle.getString(BoxIssueBottomSheet.KEY_PHOTO_ID).orEmpty()

            updateStatus(boxId, type, remark, reason, photoId)
        }
    }

    /**
     * Toggle style update used by direct taps (Mark Short / Undo Short / Undo Damage).
     * Updates the UI synchronously and persists in the background.
     *
     * @param tagname "DAMAGE" -> writes to damage column, anything else -> isshort column
     */
    private fun updateStatus(boxId: String, newStatus: String, tagname: String) {
        val index = allBoxes.indexOfFirst { it.box == boxId }
        if (index == -1) return

        val isShortAction = tagname.isEmpty() || tagname == "SHORT"

        // 1) Reflect immediately in the in-memory model + UI (main thread)
        allBoxes[index] = if (isShortAction) {
            allBoxes[index].copy(isshort = newStatus)
        } else {
            allBoxes[index].copy(damage = newStatus, damageurl = "")   // undo damage -> photo removed too
        }
        refresh()

        // 2) Persist in the background; a failure doesn't block the UI
        lifecycleScope.launch {
            try {
                if (isShortAction) barcodedao.updateShortStatus(boxId, newStatus,newStatus)
                else barcodedao.updateDamageWithPhoto(boxId, newStatus, "","")
            } catch (e: Exception) {
                Toast.makeText(this@BoxDetailsAct, e.message ?: "Save failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Damage confirmed from the bottom sheet with reason + remark + photo.
     * Writes to the DAMAGE column and saves the uploaded photo id in damageurl.
     */
    private fun updateStatus(boxId: String, newStatus: String, remark: String, reason: String, photoId: String) {
        val index = allBoxes.indexOfFirst { it.box == boxId }
        if (index == -1) return

        // 1) Update the in-memory model + UI immediately
        allBoxes[index] = allBoxes[index].copy(damage = newStatus, damageurl = photoId)
        refresh()

        // 2) Persist in the background
        lifecycleScope.launch {
            try {
                barcodedao.updateDamageWithPhoto(boxId, newStatus, photoId,remark)
                // reason / remark: add columns + DAO method if they must be saved too
            } catch (e: Exception) {
                Toast.makeText(this@BoxDetailsAct, e.message ?: "Save failed", Toast.LENGTH_SHORT)
                    .show()
            }
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
            Filter.SHORT -> allBoxes.filter { it.isshort == BoxStatus.SHORT }
        }
        adapter.submitList(filtered.toList())
    }

    private fun updateSummary() {
        val total = allBoxes.size
        val scanned = allBoxes.count { it.isScan }

        binding.tvScannedCount.text = scanned.toString()
        binding.tvTotalCount.text = "/$total"
        binding.progressLoading.max = if (total == 0) 1 else total
        binding.progressLoading.progress = scanned

        binding.tvShortCount.text = allBoxes.count { it.isshort == BoxStatus.SHORT }.toString()
        binding.tvExcessCount.text = allBoxes.count { it.status == BoxStatus.EXCESS }.toString()
        binding.tvDamageCount.text = allBoxes.count { it.damage == BoxStatus.DAMAGE }.toString()
    }
}