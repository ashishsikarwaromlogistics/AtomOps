package com.example.atomops.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.room.ColumnInfo
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.UtilsK
import com.example.atomops.adapters.ScanAdapter
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.database.daos.TripDetailsDao
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.databinding.ActivityLoadingBinding
import com.example.atomops.dialogs.AddSealBottomSheet
import com.example.atomops.models.DocketBox
import com.example.atomops.models.bulkids.BulkIdGenMod
import com.example.atomops.models.bulkids.Item
import com.example.atomops.models.closem.CloseMod
import com.example.atomops.models.dispatch.Checklist
import com.example.atomops.models.dispatch.DispatchMod
import com.example.atomops.models.findbox.FIndBoxMod
import com.example.atomops.models.loadphoto.LoadImageMod
import com.example.atomops.models.loadtrip.LoadTripMod
import com.example.atomops.models.loadtrip.ShortBoxe
import com.example.atomops.models.seal.SealMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

class LoadingAct : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityLoadingBinding
    lateinit var scanapdater: ScanAdapter
    lateinit var tripDetailsDao: TripDetailsDao
    lateinit var barcodedao: BarcodeDao

    private var docketlisttable: List<TripDetailsTable> = emptyList()
    private var docketlist: List<DocketBox> = emptyList()
    private lateinit var textToSpeech: TextToSpeech
    lateinit var serial: String
    lateinit var tripId: String
    private var boxes: MutableList<String> = mutableListOf()
    var currentManifest: String = ""
    var manifestList: List<String> = mutableListOf()
    var selectedPosition = 0

    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }

    private fun ShowDailog() {
        if (!appprogress.isShowing) appprogress.show()
    }

    private fun DismissDialog() {
        if (appprogress.isShowing) appprogress.dismiss()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) ShowDailog() else DismissDialog()
    }

    private fun showError(msg: String) {
        Utils.showDialog(this, "error", msg, R.drawable.ic_baseline_error_24)
    }

    // "message" is Any? in the models, so a missing value can appear as the text "null"
    private fun bodyMessage(msg: Any?): String =
        msg?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: GENERIC_ERROR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tripId = intent.getStringExtra("tripid").toString()
        binding.tvTripId.setText(tripId)
        serial = Utils.getSerial(this)
        textToSpeech = TextToSpeech(this, this)
        tripDetailsDao = (application as AtomOpsApp).database.tripDetailDao()
        barcodedao = (application as AtomOpsApp).database.getBarcodeDao()
        binding.ivBack.setOnClickListener { finish() }
        setupManifestTabs()

        binding.rvDockets.setHasFixedSize(false)
        binding.rvDockets.layoutManager = LinearLayoutManager(this)
        (binding.rvDockets.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, MyProfile::class.java))
        }
        binding.ivSearch.setOnClickListener { toggleSearch() }

        binding.etSearch.doAfterTextChanged {
            if (::scanapdater.isInitialized) {
                scanapdater.setQuery(it?.toString().orEmpty())
            }
        }

        clearBarcodeEdittext()

        binding.save.setOnClickListener {
            getManifestProgress()
             }

        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                var value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
                  //  value= Utils.revertTransform(value)
                    Log.d("ashishsikarwar",value)
                    if (boxes.contains(value)) {
                        speak("Duplicate Barcode")
                        showError("box already scanned")
                    } else {
                        validatebarcode(value)
                    }
                }
                clearBarcodeEdittext()
                true
            } else {
                clearBarcodeEdittext()
                false
            }
        }
    }

    // ------------------------------------------------------------------
    // Bulk event id generation
    // ------------------------------------------------------------------
    private fun Generateevenidbulk() {
        if (!Utils.haveInternet(this)) return

        ShowDailog()
        lifecycleScope.launch {
            try {
                val items: MutableList<Item> = mutableListOf()
                for (i in barcodedao.getByTrip(tripId)) {
                    if (i.isScan) {
                        items.add(
                            Item(
                                boxCode = i.box,
                                branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                                clientRef = i.clientRef,
                                companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                                deviceId = serial,
                                eventType = "OUT_SCAN",
                                expectedDocketNo = i.docketno,
                                locationCode = "",
                                scanStage = "LOAD",
                                scannedAt = Utils.getCurrentTimestamp(),
                                scannedBy = AtomOpsApp.getPreferences(Constants.EMP_ID, "")
                            )
                        )
                    }
                }

                val mod = BulkIdGenMod(
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toInt(),
                    items = items
                )

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .bulkScan("Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""), mod)

                DismissDialog()

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.status == "SUCCESS") {
                        var errors = ""
                        for (r in body.data?.results.orEmpty()) {
                            if (r.outcome == "APPLIED" || r.outcome == "APPLIED_IDEMPOTENT") {
                                barcodedao.updateeventid(r.boxCode, r.publicEventId.toString())
                            } else {
                                errors += r.boxCode + " " + r.outcome + " " + r.boxStatus.toString() + "\n"
                            }
                        }

                        if (errors.isNotEmpty()) {
                            showError(errors)
                        } else {
                            val listofboxes = barcodedao.getPendingBoxes(currentManifest)
                            if (listofboxes.isEmpty()) {
                                Utils.showDialog(
                                    this@LoadingAct, "Alert", "Do you want save this manifest",
                                    R.drawable.green_check, {
                                        if (Utils.haveInternet(this@LoadingAct)) saveData()
                                    }
                                )
                            } else {
                                Utils.showDialog(
                                    this@LoadingAct, "please scan Remaining box Status",
                                    listofboxes.toString(), R.drawable.ic_baseline_error_24
                                )
                            }
                        }
                    } else {
                        showError(bodyMessage(body?.message))
                    }
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    // ------------------------------------------------------------------
    // Barcode validation / speech
    // ------------------------------------------------------------------
    private fun validatebarcode(barcodevalue: String) {
        if (barcodevalue.length < 6) {
            showInvalid()
            return
        }
        lifecycleScope.launch {
            val count = barcodedao.updateStatus(barcodevalue, manfiest = currentManifest, true)
            if (count > 0) {
                boxes.add(barcodevalue)
                speak("Scan")
                refreshFromDb(currentManifest)   // update the docket row, not just the counter
            } else {
                speak("Invalid Barcode")
                showError("Invalid Barcode for manifest $currentManifest")
            }
        }
    }

    private fun showInvalid() {
        Utils.showDialog2(
            this, "", "Invalid Barcode", R.drawable.ic_baseline_error_24,
            { clearBarcodeEdittext() },
            { clearBarcodeEdittext() }
        )
    }

    private fun clearBarcodeEdittext() {
        binding.barcodeText.setText("")
        binding.barcodeText.requestFocus()
    }

    private fun showUpdateDialog(oldBox: String) {
        val input = EditText(this).apply {
            setText(oldBox)
            setSelection(text.length)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Update box")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newBox = input.text.toString().trim()
                val valid = newBox.length >= 6 &&
                        docketlist.any { it.docketNo == newBox.dropLast(4) }
                if (!valid) {
                    Toast.makeText(this, "Invalid Barcode", Toast.LENGTH_SHORT).show()
                } else if (newBox != oldBox) {
                    lifecycleScope.launch {
                        // TODO: update DB with the right DAO method, then update the adapter
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US)
        }
    }

    private fun speak(text: String) {
        try {
            textToSpeech.speak(text.lowercase(Locale.getDefault()), TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Manifest tabs
    // ------------------------------------------------------------------
    fun selectTab(position: Int) {
        selectedPosition = position

        for (i in 0 until binding.llManifestTabs.childCount) {
            val tab = binding.llManifestTabs.getChildAt(i) as TextView

            if (i == selectedPosition) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(android.graphics.Color.WHITE)
            } else {
                tab.background = null
                tab.setTextColor(ContextCompat.getColor(tab.context, R.color.text_primary))
            }
        }
    }

    private fun setupManifestTabs() {
        lifecycleScope.launch {
            manifestList = tripDetailsDao.getByTripandmanifiest(tripId, AtomOpsApp.getPreferences(
                Constants.BCODE,""))
            binding.llManifestTabs.removeAllViews()
            selectedPosition = 0

            manifestList.forEachIndexed { index, manifest ->
                val textView = TextView(binding.root.context).apply {
                    text = manifest
                    gravity = android.view.Gravity.CENTER
                    textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD)

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._33sdp),
                        1f
                    ).apply {
                        marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                    }

                    setPadding(
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                        0,
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                        0
                    )

                    setOnClickListener {
                        selectTab(index)
                        onManifestSelected(manifest, index)
                    }
                }
                binding.llManifestTabs.addView(textView)
            }

            if (manifestList.isNotEmpty()) {
                selectTab(0)
                // called once, for the first tab only
                onManifestSelected(manifestList[0], 0)
            }
        }
    }

    // The coroutine loading the currently selected manifest. Cancelled when another tab is tapped,
    // so a slow response for an old manifest can never overwrite the new one.
    private var manifestJob: Job? = null

    private fun TripDetailsTable.toDocketBox(scanned: List<String>) = DocketBox(
        manifestNo = manifestNo,
        legSequence = legSequence,
        sourceBranch = sourceBranch,
        destinationBranch = destinationBranch,
        cnCount = cnCount,
        boxCountTotal = boxCountTotal,
        status = status,
        boxesScanned = boxesScanned,
        boxesUnloaded = boxesUnloaded,
        tripId = tripId,

        docketNo = docketNo,
        boxes = boxes,                     // was boxCountTotal (the whole MANIFEST total) -> wrong per-docket count
        weightKg = weightKg,
        serviceMode = serviceMode,
        agingHours = agingHours,
        documentStatus = documentStatus,
        missingMarkedAt = missingMarkedAt,
        missingMarkedBy = missingMarkedBy,
        fragile = fragile,
        weightCheckRequired = weightCheckRequired,
        liquidOil = liquidOil,

        scanboxelist = scanned
    )


    private fun onManifestSelected(manifest: String, position: Int) {

        currentManifest = manifest
        findShartandDamage()
        manifestJob?.cancel()
        resetManifestUi()                  // never show the previous manifest's numbers

        manifestJob = lifecycleScope.launch {
            // Only this trip's dockets, one row per docket (guards against duplicate rows in TripDetailsTable)
            docketlisttable = tripDetailsDao.getByManifest(manifest)
                .filter { it.tripId == null || it.tripId == tripId }
                .distinctBy { it.docketNo }

            // 1) Show what we already have locally straight away (also works offline)
            refreshFromDb(manifest)

            // 2) Sync boxes from the server, then redraw from the DB
            if (!Utils.haveInternet(this@LoadingAct)) return@launch
            try {
                val mod = FIndBoxMod(
                    AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toIntOrNull() ?: 0,
                    docketlisttable.map { it.docketNo }
                )
                val response = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .getAllboxes("Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""), mod)

                ensureActive()                                  // user switched tab while waiting
                if (manifest != currentManifest) return@launch

                if (!response.isSuccessful) { showError(response.errorMessage()); return@launch }
                val body = response.body()
                val data = body?.data
                if (body?.status != "SUCCESS" || data == null) { showError(bodyMessage(body?.message)); return@launch }
                if (data.dockets.isEmpty()) { showError("no data found"); return@launch }

                // One query instead of 2 queries per box. Scoped to THIS manifest, so a box left over
                // from another manifest/trip no longer blocks the insert.
                val existing = barcodedao.getByManifest(manifest).map { it.box }.toHashSet()
                for (d in data.dockets) {
                    for (b in d.boxes) {
                        if (b.boxCode in existing) continue
                       // d.boxes.get(0).currentBranch

                        if (!b.voided && (b.status.equals("PICKED") || b.status.equals("AT_HUB")) && b.currentBranch.equals(
                                AtomOpsApp.getPreferences(Constants.BCODE, ""))){
                            Log.d("ashishsikarwar",b.boxCode+"="+ Utils.transform(b.boxCode))
                            barcodedao.insert(
                                BarcodeTable(
                                    docketno = d.docketNo,
                                    box = b.boxCode,
                                    timeScan = "",
                                    status = b.status,
                                    voided = b.voided,
                                    manifest = manifest,
                                    damage = "",
                                    isshort = "",
                                    damageurl = "",
                                    tripId = tripId,
                                    isScan = false,
                                    eventid = "",
                                    clientRef = Utils.GenrateUUID(),
                                    boxloaded = false
                                )
                            )
                            existing.add(b.boxCode)
                        }
                       else continue



                        /*if (b.voided || !b.status.equals("PICKED") || !b.status.equals("AT_HUB")) continue
                           barcodedao.insert(
                            BarcodeTable(
                                docketno = d.docketNo,
                                box = b.boxCode,
                                timeScan = "",
                                status = b.status,
                                voided = b.voided,
                                manifest = manifest,
                                damage = "",
                                isshort = "",
                                damageurl = "",
                                tripId = tripId,
                                isScan = false,
                                eventid = "",
                                clientRef = Utils.GenrateUUID(),
                                boxloaded = false
                            )
                        )
                        existing.add(b.boxCode)*/
                    }
                }

                ensureActive()
                refreshFromDb(manifest)
            } catch (e: CancellationException) {
                throw e                                         // normal when switching tabs, not an error
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    /** Rebuilds the docket list + counters for [manifest] from the local DB. */
    private suspend fun refreshFromDb(manifest: String) {
        val rows = barcodedao.getByManifest(manifest)
        if (manifest != currentManifest) return

        val scannedByDocket = rows.filter { it.isScan }
            .groupBy({ it.docketno }, { it.box })

        boxes.clear()
        boxes.addAll(rows.filter { it.isScan }.map { it.box })

        docketlist = docketlisttable.map { it.toDocketBox(scannedByDocket[it.docketNo].orEmpty()) }

        // keep scroll position when the list is rebuilt
        val state = binding.rvDockets.layoutManager?.onSaveInstanceState()
        scanapdater = ScanAdapter(docketlist, onItemClick = {
          //  val context = holder.itemView.context

            val intent = Intent(this@LoadingAct, BoxDetailsAct::class.java)
            intent.putExtra("docketno", it.docketNo)
            intent.putExtra("tripId", tripId)
            startActivity(intent)
        })
        binding.rvDockets.adapter = scanapdater
        scanapdater.setQuery(binding.etSearch.text.toString())
        state?.let { binding.rvDockets.layoutManager?.onRestoreInstanceState(it) }

        binding.tvDocketCount.text = docketlist.size.toString()
        binding.tvDocketsCount.text = docketlist.size.toString()
        binding.tvBoxesCount.text = rows.size.toString()
        binding.tvscannedbarcode.text = boxes.size.toString()
        binding.tvManifestProgress.text = "/" + rows.size
    }

    private fun resetManifestUi() {
        boxes.clear()
        docketlist = emptyList()
        scanapdater = ScanAdapter(emptyList(), onItemClick = {
            val intent = Intent(this@LoadingAct, BoxDetailsAct::class.java)
            intent.putExtra("docketno", it.docketNo)
            intent.putExtra("tripId", tripId)
            startActivity(intent)
        })
        binding.rvDockets.adapter = scanapdater
        binding.tvDocketCount.text = "0"
        binding.tvDocketsCount.text = "0"
        binding.tvBoxesCount.text = "0"
        binding.tvscannedbarcode.text = "0"
        binding.tvManifestProgress.text = "/0"
    }

    // ------------------------------------------------------------------
    // Save manifest
    // ------------------------------------------------------------------
    private fun saveData() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val shortbox = mutableListOf<ShortBoxe>()
                for (i in barcodedao.getByManifestshort(manifest = currentManifest)) {
                    shortbox.add(ShortBoxe(boxCode = i.box, docketNo = i.docketno, remarks = i.shortreason))
                }

                val item = mutableListOf<com.example.atomops.models.loadtrip.Item>()
                for (i in barcodedao.getByManifestwithoutshort(currentManifest)) {
                    item.add(
                        com.example.atomops.models.loadtrip.Item(
                            boxCode = i.box,
                            damagePhotoUrl = i.damageurl,
                            damageRemarks = i.damagereason,
                            docketNo = i.docketno,
                            scanEventId = i.eventid
                        )
                    )
                }

                val mod = LoadTripMod(
                    actor = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                    branch = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toInt(),
                    items = item,
                    shortBoxes = shortbox
                )

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .bulkLoadBoxes(
                        "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        currentManifest,
                        mod
                    )

                DismissDialog()

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.status == "SUCCESS") {
                        barcodedao.updateLoadstatus(currentManifest)
                        Utils.showDialog(
                            this@LoadingAct, "SUCCESS", bodyMessage(body.message),
                            R.drawable.green_check, {
                                if (Utils.haveInternet(this@LoadingAct)) LaodingImage()
                            }
                        )
                    } else {
                        showError(bodyMessage(body?.message))
                    }
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    // ------------------------------------------------------------------
    // Loading photo
    // ------------------------------------------------------------------
    fun LaodingImage() {
        ShowDailog()
        captureAndUpload { path, success ->
            if (success && path != null) {
                lifecycleScope.launch {
                    ShowDailog()
                    try {
                        val mod = LoadImageMod(
                            actor = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                            branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                            captureStage = "LOAD_100_PERCENT",
                            geoLat = 56.43324,   // TODO: replace with real location if backend uses it
                            geoLong = 56.43324,
                            photoDocumentId = path
                        )
                        val response = ApiClient.getRetrofitclient()
                            .create(ServiceInterface::class.java)
                            .loadingPhoto(
                                "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                                currentManifest,
                                AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                                mod
                            )

                        DismissDialog()

                        if (response.isSuccessful) {
                            Utils.showDialog(
                                this@LoadingAct, "Message", "Are you sure want to Close this Loading?",
                                R.drawable.green_check, {
                                    if (Utils.haveInternet(this@LoadingAct)) LoadingClose()
                                }
                            )
                        } else {
                            showError(response.errorMessage())
                        }
                    } catch (e: Exception) {
                        DismissDialog()
                        Utils.gettrycatcherror(this@LoadingAct, e)
                    }
                }
                Log.d("Upload", "Done, document id: $path")
            } else {
                DismissDialog()
                Log.d("Upload", "Failed or cancelled")
            }
        }
    }

    // ------------------------------------------------------------------
    // Seal
    // ------------------------------------------------------------------
    fun closeSeal(sealNumber: String, photoUri: String) {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val mod = SealMod(
                    AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                    AtomOpsApp.getPreferences(Constants.BCODE, ""),
                    photoUri,
                    sealNumber,
                    "PHYSICAL"
                )
                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .loadingseal("Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""), tripId, mod)

                DismissDialog()

                if (resp.isSuccessful) {
                    Utils.showDialog(
                        this@LoadingAct, "Success",
                        bodyMessage(resp.body()?.message) + "\nReady for Dispatch?",
                        R.drawable.green_check, {
                            if (Utils.haveInternet(this@LoadingAct)) showReadyForDispatchDialog()
                        }
                    )
                    deleteDatafronted()
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    // ------------------------------------------------------------------
    // Close loading
    // ------------------------------------------------------------------
    fun LoadingClose() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                if (!barcodedao.areAllBoxesLoaded(tripId)) {
                    DismissDialog()
                    showError("Please load all boxes of the manifest")
                    return@launch
                }

                val mod = CloseMod(
                    AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                    AtomOpsApp.getPreferences(Constants.BCODE, "")
                )

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .LoadingClose(
                        "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        currentManifest,
                        mod
                    )

                DismissDialog()

                if (resp.isSuccessful) {
                    if (selectedPosition < manifestList.size - 1) {
                        selectedPosition++
                        selectTab(selectedPosition)
                        onManifestSelected(manifestList[selectedPosition], selectedPosition)
                        Utils.showDialog(
                            this@LoadingAct, "Success",
                            resp.body()?.message?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                                ?: "Manifest closed",
                            R.drawable.green_check
                        )
                    } else {
                        Utils.showDialog(
                            this@LoadingAct, "Success", "Do you want to close Seal?",
                            R.drawable.green_check, { addseal() }
                        )
                    }
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    fun addseal() {
        AddSealBottomSheet { sealNumber ->
            captureAndUpload { path, success ->
                if (success && path != null) {
                    if (Utils.haveInternet(this@LoadingAct)) closeSeal(sealNumber, path)
                    Log.d("Upload", "Done, document id: $path")
                } else {
                    Log.d("Upload", "Failed or cancelled")
                }
            }
        }.show(supportFragmentManager, AddSealBottomSheet.TAG)
    }

    private fun deleteDatafronted() {
        boxes.clear()
        lifecycleScope.launch {
            barcodedao.deleteByTrip(tripId)
        }
    }

    // ------------------------------------------------------------------
    // Camera + upload
    // ------------------------------------------------------------------
    // Callback invoked after camera + upload finish
    private var onUploadComplete: ((path: String?, success: Boolean) -> Unit)? = null

    private var photoFile: File? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = photoFile
            if (success && file != null && file.exists()) {
                uploadImage(file)
            } else {
                Toast.makeText(this, "Capture cancelled", Toast.LENGTH_SHORT).show()
                onUploadComplete?.invoke(null, false)
                onUploadComplete = null
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                }
                onUploadComplete?.invoke(null, false)
                onUploadComplete = null
            }
        }

    fun captureAndUpload(onComplete: (path: String?, success: Boolean) -> Unit) {
        onUploadComplete = onComplete
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) openCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        photoFile = file

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    private fun uploadImage(file: File) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                // heavy work off the main thread
                val compressed = withContext(Dispatchers.IO) { UtilsK.compressImage(file) }

                val body = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", compressed.name, body)

                val response = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .uploadDocument(
                        auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        templateCode = "PHOTOGRAPH",
                        isLinked = true,
                        documentName = "MyDoc",
                        file = part
                    )

                setLoading(false)

                if (response.isSuccessful) {
                    val id = response.body()?.data?.id
                    if (!id.isNullOrBlank()) {
                        Toast.makeText(this@LoadingAct, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                        onUploadComplete?.invoke(id, true)
                        onUploadComplete = null
                    } else {
                        // uploaded but no document id came back
                        showError(GENERIC_ERROR)
                    }
                } else {
                    showError(response.errorMessage())
                }
            } catch (e: Exception) {
                setLoading(false)
                Utils.gettrycatcherror(this@LoadingAct, e)
            } finally {
                setLoading(false)
                // runs the callback with failure only if the success path did not already consume it
                onUploadComplete?.invoke(null, false)
                onUploadComplete = null
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera permission needed")
            .setMessage("Please enable camera permission from app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------------------------
    // Ready for dispatch
    // ------------------------------------------------------------------
    private fun showReadyForDispatchDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_ready_dispatch, null)

        val vehicletype = view.findViewById<CheckBox>(R.id.vehicletype)
        val cbtarpaulin = view.findViewById<CheckBox>(R.id.cbtarpaulin)
        val cbLashingBelt = view.findViewById<CheckBox>(R.id.cbLashingBelt)
        val cbgps = view.findViewById<CheckBox>(R.id.cbgps)
        val digitallock = view.findViewById<CheckBox>(R.id.digitallock)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val checklist = Checklist(
                digitallock.isChecked,
                cbgps.isChecked,
                cbLashingBelt.isChecked,
                cbtarpaulin.isChecked,
                vehicletype.isChecked
            )
            val mod = DispatchMod(
                AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                checklist,
                "GENERAL",
                AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toInt()
            )
            dialog.dismiss()

            lifecycleScope.launch {
                try {
                    ShowDailog()
                    val resp = ApiClient.getRetrofitclient()
                        .create(ServiceInterface::class.java)
                        .readyfordispatch(
                            "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                            tripId,
                            mod
                        )

                    DismissDialog()

                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.status == "SUCCESS") {
                            Utils.showDialog(
                                this@LoadingAct, "Success", bodyMessage(body.message),
                                R.drawable.green_check, {
                                    startActivity(Intent(this@LoadingAct, LoadingUnloading::class.java))
                                }
                            )
                        } else {
                            showError(bodyMessage(body?.message))
                        }
                    } else {
                        showError(resp.errorMessage())
                    }
                } catch (e: Exception) {
                    DismissDialog()
                    Utils.gettrycatcherror(this@LoadingAct, e)
                }
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
    }


    private fun toggleSearch() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val opening = binding.etSearch.visibility != View.VISIBLE

        if (opening) {
            binding.tvTripId.visibility = View.GONE
            binding.etSearch.visibility = View.VISIBLE
            binding.etSearch.requestFocus()
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.etSearch.setText("")
            binding.etSearch.visibility = View.GONE
            binding.tvTripId.visibility = View.VISIBLE
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            clearBarcodeEdittext()
        }
    }

    private fun getManifestProgress() {
        if (!Utils.haveInternet(this)) return
        ShowDailog()
        lifecycleScope.launch {
            try {
                val tripNo     = tripId                        // already used in getByTrip(tripId)
                val manifestNo = currentManifest               // already used in getPendingBoxes(currentManifest)
                val branchCode = AtomOpsApp.getPreferences(Constants.BCODE, "")

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .getManifestProgress(
                        "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        tripNo,
                        manifestNo,
                        branchCode
                    )

                DismissDialog()

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.status == "SUCCESS") {
                        val data = body.data
                        val progress = data?.progress

                        if (data == null || progress == null) {
                            showError("No progress data received")
                            return@launch
                        }

                        // All boxes loaded and manifest can be closed → offer save
                        if(body.data.progress.closed==true){
                            Utils.showDialog(
                                this@LoadingAct, "Alert", "This manifest already close . plz select another manifest",
                                R.drawable.ic_baseline_error_24
                            )
                        }
                        else if(body.data.status.toString().equals("CREATED")){
                            Generateevenidbulk()
                        }
                        else if(body.data.status.toString().equals("LOADING")){
                            saveData()
                        }
                       else if(body.data.status.toString().equals("LOADED")){
                            LoadingClose()
                        }
                        else if(body.data.progress.photo100 != null){
                            LoadingClose()
                        }
                        else{
                            LaodingImage()
                        }



                        /*else if (progress.canClose == true) {
                            Utils.showDialog(
                                this@LoadingAct, "Alert", "Do you want save this manifest",
                                R.drawable.green_check, {
                                    if (Utils.haveInternet(this@LoadingAct)) saveData()
                                }
                            )
                        }
                        else {
                            // Build a readable summary of what is pending / blocking
                            val blockers = progress.closeBlockers?.joinToString("\n").orEmpty()
                            val summary = buildString {
                                append("Expected: ${progress.boxesExpected}\n")
                                append("Scanned : ${progress.boxesScanned}\n")
                                append("Pending : ${progress.boxesPending}\n")
                                append("Short   : ${progress.boxesShort}\n")
                                append("Excess  : ${progress.boxesExcess}\n")
                                if (progress.photo100Required == true && progress.photo100 == null) {
                                    append("\nPhoto 100% is required")
                                }
                                if (blockers.isNotEmpty()) {
                                    append("\n").append(blockers)
                                }
                            }
                            Utils.showDialog(
                                this@LoadingAct, "Manifest Progress", summary,
                                R.drawable.ic_baseline_error_24
                            )
                        }*/
                    } else {
                        showError(bodyMessage(body?.message))
                    }
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@LoadingAct, e)
            }
        }
    }

    private fun findShartandDamage(){
     lifecycleScope.launch {
         /*val manifest: String,
         @ColumnInfo(name = "trip_id")
         val tripId: String,*/

         val c = barcodedao.getShortDamageCount(currentManifest)
         binding.tvShort.text  = c.shortCount.toString()
         binding.tvDamage.text = c.damageCount.toString()
     }
    }

    override fun onResume() {
        super.onResume()
        if (currentManifest.isNotEmpty()) {
            findShartandDamage()
            lifecycleScope.launch {
                refreshFromDb(currentManifest)   // rebuilds docket list + progress counts
            }
        }
    }

}
