package com.example.atomops.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.adapters.LocalLoadBoxAdapter
import com.example.atomops.database.daos.LocalLoadbarcodeDao
import com.example.atomops.databinding.ActivityLocalLoadBoxDetailsBinding
import com.example.atomops.models.localloadbox.LoadBoxItem
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalLoadBoxDetails : AppCompatActivity() {



    private lateinit var binding: ActivityLocalLoadBoxDetailsBinding

    private val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }
    private fun ShowDailog() { if (!appprogress.isShowing) appprogress.show() }
    private fun DismissDialog() { if (appprogress.isShowing) appprogress.dismiss() }
    private val serviceinterface by lazy { ApiClient.getRetrofitclient().create(ServiceInterface::class.java) }

    private val allItems = mutableListOf<LoadBoxItem>()
    private var currentFilter = Filter.ALL
    private var query = ""
    private var excessBoxes = 0
    private var tripId = ""
    private var docketNo = ""

    private val adapter = LocalLoadBoxAdapter { item -> onActionClicked(item) }
    lateinit var dao: LocalLoadbarcodeDao
    private enum class Filter { ALL, SCANNED, UNSCANNED, SHORT, DAMAGE }

    // Request codes
    private val REQ_DAMAGE_IMAGE = 1001

    // Temp state for the currently-marked-damage box
    private var pendingDamageBoxCode: String? = null
    private var pendingDamageRemarks: String = ""
    private var pendingDamagePath: String? = null

    // Image capture
    private var photoUri: Uri? = null
    private var cameraImagePath: String? = null

    // Reused for image preview thumbnail in the damage dialog
    private lateinit var damageDialogPreview: ImageView   // set inside dialog builder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_local_load_box_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dao = (application as AtomOpsApp).database.getLocalLoadbarcode()

        tripId   = intent.getStringExtra("tripId") ?: ""
        docketNo = intent.getStringExtra("docketNo") ?: ""

        binding.tvTripId.text = tripId
        binding.rvBoxes.adapter = adapter
        binding.ivBack.setOnClickListener { finish() }

        setupSearch()
        setupFilters()
        selectFilter(Filter.ALL)

        loadData()
    }


    private fun loadData() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val rows = if (docketNo.isNotEmpty())
                    dao.getByDocketAndTrip(docketNo, tripId)
                else
                    dao.getByTrip(tripId)

                allItems.clear()
                rows.forEach { row ->
                    val code = row.barcodebox ?: return@forEach
                    allItems.add(
                        LoadBoxItem(
                            boxCode       = code,
                            docketNo      = row.docket.orEmpty(),
                            scanned       = row.scanStatus.equals("Scanned", true),
                            scannedAt     = row.scanAt,
                            isDamage      = row.isDamage.equals("Damage", true),
                            damageUrl     = row.damageURL,
                            damageRemarks = row.damageremarks,
                            isShort       = row.shortremarks.isNotBlank(),
                            shortRemarks  = row.shortremarks
                        )
                    )
                }
                refresh()
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoadBoxDetails, e)
            } finally { DismissDialog() }
        }
    }

    private fun onActionClicked(item: LoadBoxItem) {
        val idx = allItems.indexOfFirst { it.boxCode == item.boxCode }
        if (idx == -1) return

        when {
            // ---- Undo Damage ----
            item.isDamage -> {
                allItems[idx] = item.copy(
                    isDamage = false,
                    damageRemarks = "",
                    damageUrl = ""
                )
                refresh()
                lifecycleScope.launch { dao.clearDamage(item.boxCode, tripId) }
            }

            // ---- Undo Short ----
            item.isShort -> {
                allItems[idx] = item.copy(isShort = false, shortRemarks = "")
                refresh()
                lifecycleScope.launch { dao.clearShort(item.boxCode, tripId) }
            }

            // ---- Mark Damage ----
            item.scanned -> {
                showDamageDialog(item)
            }

            // ---- Mark Short ----
            else -> {
                showShortDialog(item)
            }
        }
    }

    // --- Damage dialog ---
    private fun showDamageDialog(item: LoadBoxItem) {
        // ---- Reason options ----
        val reasons = arrayOf(
            "Damaged in transit",
            "Crushed box",
            "Wet / water damage",
            "Torn packaging",
            "Broken seal",
            "Other"
        )

        // Reset temp state
        pendingDamageBoxCode = item.boxCode
        pendingDamageRemarks = ""
        pendingDamagePath    = null
        photoUri             = null
        cameraImagePath      = null

        // ---- Custom view: Spinner + "Capture Image" button + preview ImageView ----
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@LocalLoadBoxDetails,
                android.R.layout.simple_spinner_dropdown_item,
                reasons
            )
        }
        container.addView(spinner, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val btnCapture = Button(this).apply {
            text = "Capture Image"
        }
        container.addView(btnCapture, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        val preview = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        container.addView(preview, LinearLayout.LayoutParams(
            dp(120), dp(120)
        ).apply { topMargin = dp(12) })

        damageDialogPreview = preview   // so onActivityResult can update it

        // ---- Launch camera ----
        btnCapture.setOnClickListener {
            startCameraForDamage()
        }

        // ---- Show the dialog ----
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mark Damage — ${item.boxCode}")
            .setView(container)
            .setPositiveButton("Save", null)   // set listener below to allow validation
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val remarks = spinner.selectedItem?.toString().orEmpty()
                    if (remarks.isBlank()) {
                        Toast.makeText(this, "Select a damage reason", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val imagePath = pendingDamagePath
                    if (imagePath.isNullOrBlank()) {
                        Toast.makeText(this, "Please capture an image", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Update DB + UI
                    val idx = allItems.indexOfFirst { it.boxCode == item.boxCode }
                    if (idx != -1) {
                        allItems[idx] = allItems[idx].copy(
                            isDamage = true,
                            damageRemarks = remarks,
                            damageUrl = imagePath
                        )
                        refresh()
                    }
                    lifecycleScope.launch {
                        dao.markDamage(item.boxCode, tripId, remarks, imagePath)
                    }
                    dialog.dismiss()
                }
        }
        dialog.show()
    }

    private fun startCameraForDamage() {
        try {
            val dir = File(cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "damage_${System.currentTimeMillis()}.jpg")
            cameraImagePath = file.absolutePath

            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )
            photoUri = uri

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, REQ_DAMAGE_IMAGE)
        } catch (e: Exception) {
            Utils.gettrycatcherror(this, e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DAMAGE_IMAGE && resultCode == RESULT_OK) {
            val path = cameraImagePath
            if (!path.isNullOrBlank() && File(path).exists()) {
                pendingDamagePath = path
                try {
                    damageDialogPreview.visibility = View.VISIBLE
                    damageDialogPreview.setImageURI(Uri.fromFile(File(path)))
                } catch (_: Exception) { }
            }
        }
    }

    // --- Short dialog ---
    private fun showShortDialog(item: LoadBoxItem) {
        val input = EditText(this).apply {
            hint = "Short remarks (e.g. Lost, Missing)"
            setText(item.shortRemarks)
            setSelection(text.length)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mark Short — ${item.boxCode}")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val remarks = input.text.toString().trim()
                if (remarks.isEmpty()) {
                    Toast.makeText(this, "Enter a remark", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val idx = allItems.indexOfFirst { it.boxCode == item.boxCode }
                if (idx != -1) {
                    allItems[idx] = allItems[idx].copy(isShort = true, shortRemarks = remarks)
                    refresh()
                }
                lifecycleScope.launch {
                    dao.markShort(item.boxCode, tripId, remarks)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    // ---------------- DATA (from local DB only) ----------------





    // ---------------- UI ----------------
    private fun refresh() {
        applyFilter()
        updateSummary()
    }

    private fun applyFilter() {
        val list = allItems
            .filter {
                when (currentFilter) {
                    Filter.ALL       -> true
                    Filter.SCANNED   -> it.scanned
                    Filter.UNSCANNED -> !it.scanned
                    Filter.SHORT     -> it.isShort
                    Filter.DAMAGE    -> it.isDamage
                }
            }
            .filter { query.isBlank() || it.boxCode.contains(query, ignoreCase = true) }
        adapter.submitList(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateSummary() {
        val total = allItems.size
        binding.tvLoadedCount.text = allItems.count { it.scanned }.toString()
        binding.tvTotalCount.text  = "/$total"
        binding.progressTotal.max      = if (total == 0) 1 else total
        binding.progressTotal.progress = allItems.count { it.scanned }
        binding.tvShort.text  = allItems.count { it.isShort  }.toString()
        binding.tvDamage.text = allItems.count { it.isDamage }.toString()
        binding.tvExcess.text = excessBoxes.toString()
    }



    private fun setupFilters() {
        binding.tvFilterAll.setOnClickListener       { selectFilter(Filter.ALL) }
        binding.tvFilterScanned.setOnClickListener   { selectFilter(Filter.SCANNED) }
        binding.tvFilterUnscanned.setOnClickListener { selectFilter(Filter.UNSCANNED) }
        binding.tvFilterShort.setOnClickListener     { selectFilter(Filter.SHORT) }
        binding.tvFilterDamage.setOnClickListener    { selectFilter(Filter.DAMAGE) }
    }

    private fun selectFilter(f: Filter) {
        currentFilter = f
        val chips = mapOf(
            Filter.ALL       to binding.tvFilterAll,
            Filter.SCANNED   to binding.tvFilterScanned,
            Filter.UNSCANNED to binding.tvFilterUnscanned,
            Filter.SHORT     to binding.tvFilterShort,
            Filter.DAMAGE    to binding.tvFilterDamage
        )
        chips.forEach { (filter, tv) ->
            val selected = filter == f
            tv.setBackgroundResource(if (selected) R.drawable.bg_filter_selected else R.drawable.bg_filter_unselected)
            tv.setTextColor(if (selected) 0xFF0B3A6F.toInt() else 0xFF374151.toInt())
        }
        applyFilter()
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val showing = binding.etSearch.visibility == View.VISIBLE
            if (showing) {
                binding.etSearch.setText("")
                binding.etSearch.visibility = View.GONE
                binding.tvTripId.visibility = View.VISIBLE
            } else {
                binding.tvTripId.visibility = View.GONE
                binding.etSearch.visibility = View.VISIBLE
                binding.etSearch.requestFocus()
            }
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                applyFilter()
            }
        })
    }
}