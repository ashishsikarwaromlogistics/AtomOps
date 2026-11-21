package com.example.atomops.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.adapters.LocalUnLoadDocketAdapter
import com.example.atomops.adapters.UnloadDocketRow
import com.example.atomops.database.daos.LocalLoadbarcodeDao
import com.example.atomops.database.tables.LocalLoadbarcode
import com.example.atomops.databinding.ActivityLocalUnLoadingBinding
import com.example.atomops.localclasses.SaveValidationResult
import com.example.atomops.models.CloseLoadingRequest
import com.example.atomops.models.bulkids.BulkIdGenMod
import com.example.atomops.models.bulkids.Item
import com.example.atomops.models.bulkids.Result
import com.example.atomops.models.bulkload.BulkException
import com.example.atomops.models.bulkload.BulkItem
import com.example.atomops.models.bulkload.BulkLoadingRequest
import com.example.atomops.models.localtripunload.Data as TripUnloadingData
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import java.util.Locale

class LocalUnLoading : AppCompatActivity() , LocalUnLoadDocketAdapter.OnItemClickListener, TextToSpeech.OnInitListener {

    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }

    private lateinit var binding: ActivityLocalUnLoadingBinding
    private fun ShowDailog() { if (!appprogress.isShowing) appprogress.show() }
    private fun DismissDialog() { if (appprogress.isShowing) appprogress.dismiss() }
    private val serviceinterface by lazy { ApiClient.getRetrofitclient().create(ServiceInterface::class.java)  }
    var tripId=""
    var item=this//LocalUnLoadDocketAdapter.OnItemClickListener
    private val docketAdapter = LocalUnLoadDocketAdapter(item)
    private var tripUnloading: TripUnloadingData? = null
    private var docketlist: MutableList<String> = mutableListOf()                       // every docketNo
    private var docketBoxIds: MutableList<String> = mutableListOf()                     // every boxId
    private val docketBoxMap: LinkedHashMap<String, MutableList<String>> = linkedMapOf() // docketNo -> boxIds

    lateinit var dao: LocalLoadbarcodeDao
    private lateinit var textToSpeech: TextToSpeech
    private var boxes: MutableList<String> = mutableListOf()
    var serial:String=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_local_un_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        serial= Utils.getSerial(this)
        textToSpeech = TextToSpeech(this, this)
        dao = (application as AtomOpsApp).database.getLocalLoadbarcode()
        binding.rvDockets.adapter = docketAdapter
        tripId=   intent.getStringExtra("tripId").toString()
        loadTripLoading(tripId)
        binding.tvTripId.setText("Unloading:"+tripId)
        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
                    if(boxes.contains(value)) {
                        speak("Duplicate Barcode")
                        Utils.showDialog(
                            this@LocalUnLoading,
                            "error",
                            "box already scanned",
                            R.drawable.ic_baseline_error_24
                        )
                    } else
                        validatebarcode(value)
                }
                clearBarcodeEdittext()
                true
            } else {
                clearBarcodeEdittext()
                false
            }
        }

        binding.save.setOnClickListener {
            lifecycleScope.launch {
                ShowDailog()
                try {
                    val result = validateBeforeSave(tripId)

                    if (result.canSave) {
                        // ✅ every box is scanned or short
                        //speak("All boxes scanned")
                        saveLoading(result)
                        //submitBulk()
                        Generateevenidbulk()// TODO: call your save / submit API
                    } else {
                        // ❌ pending boxes → speak + show remaining dockets
                        speak("${result.pendingBoxes} boxes remaining")
                        showPendingDialog(result)
                    }
                } catch (e: Exception) {
                    Utils.gettrycatcherror(this@LocalUnLoading, e)
                } finally {
                    DismissDialog()
                }
            }
        }
    }
    private fun showPendingDialog(result: SaveValidationResult) {
        val message = buildString {
            appendLine("${result.pendingBoxes} box(es) still pending in:")
            appendLine()
            result.pendingByDocket.forEach { (docket, boxCodes) ->
                appendLine("Docket: $docket")
                boxCodes.forEach { appendLine("   • $it") }
                appendLine()
            }
            append("Please scan them or mark as Short before saving.")
        }

        Utils.showDialog(
            this@LocalUnLoading,
            "Pending",
            message,
            R.drawable.ic_baseline_error_24
        )
    }
    private fun saveLoading(result: SaveValidationResult) {
        /*lifecycleScope.launch {
            ShowDailog()
            try {
                // Build payload from DB rows
                val rows = dao.getByTrip(tripId)
                val payload = rows.map {
                    SaveBarcodeItem(
                        tripId       = tripId,
                        barcode      = it.barcodebox.orEmpty(),
                        docket       = it.docket.orEmpty(),
                        scanStatus   = it.scanStatus.orEmpty(),
                        isDamage     = it.isDamage,
                        damageUrl    = it.damageURL,
                        shortRemarks = it.shortremarks,
                        damageRemarks= it.damageremarks,
                        scanId       = it.scanId.orEmpty(),
                        scanAt       = it.scanAt.orEmpty()
                    )
                }
                val res = serviceinterface.saveLoadBarcode(
                    auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    request = SaveLoadBarcodeRequest(payload)
                )
                if (res.code() == 200) {
                    Utils.showDialog(this@LocalLoading, "Success",
                        "Saved successfully", R.drawable.ic_check_circle_green) {
                        finish()
                    }
                } else {
                    Utils.handleHttpError(this@LocalLoading, res.code())
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoading, e)
            } finally {
                DismissDialog()
            }
        }*/
    }
    private suspend fun validateBeforeSave(tripId: String): SaveValidationResult {
        val all     = dao.getByTrip(tripId)
        val pending = dao.getPendingBoxes(tripId)

        val scannedCount = all.count { it.scanStatus.equals("Scanned", true) }
        val shortCount   = all.count { it.shortremarks.isNotBlank() }
        val pendingCount = pending.size

        val pendingByDocket = pending
            .groupBy { it.docket.orEmpty().ifBlank { "-" } }
            .mapValues { (_, list) -> list.mapNotNull { it.barcodebox } }

        return SaveValidationResult(
            canSave         = pendingCount == 0,
            totalBoxes      = all.size,
            scannedBoxes    = scannedCount,
            shortBoxes      = shortCount,
            pendingBoxes    = pendingCount,
            pendingByDocket = pendingByDocket
        )
    }
    private fun loadTripLoading(tripNo: String) {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val res = serviceinterface.getTripUnLoading(
                    auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo = tripNo,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "")
                )
                if (res.isSuccessful) {
                    val data = res.body()?.data
                    if (data != null) {
                        tripUnloading = data
                        collectDocketsAndBoxes(data)   // fills docketlist + docketBoxIds (all boxes come in this API)
                        saveBoxesToDb()                // insert new boxes as "Unscanned" in LocalLoadbarcode
                        bindSummary()
                        refreshLocalSummary()          // counters + docket list from DB
                    } else {
                        Utils.showDialog(this@LocalUnLoading, "error", "No data found", R.drawable.ic_baseline_error_24)
                    }
                } else {
                    try{
                        Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LocalUnLoading,e)
                    }
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalUnLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }

    /** delivery[] + pickups[].dockets[] -> docketlist, docketBoxIds and docketBoxMap (docketNo -> boxIds). */
    private fun collectDocketsAndBoxes(data: TripUnloadingData) {
        docketlist.clear()
        docketBoxIds.clear()
        docketBoxMap.clear()

        fun add(docketNo: String?, boxId: String?) {
            val d = docketNo?.takeIf { it.isNotBlank() } ?: return
            if (d !in docketlist) docketlist.add(d)
            val boxList = docketBoxMap.getOrPut(d) { mutableListOf() }
            val b = boxId?.takeIf { it.isNotBlank() } ?: return
            if (b !in docketBoxIds) docketBoxIds.add(b)
            if (b !in boxList) boxList.add(b)
        }

        // 1) delivery boxes (goods coming back)
        for (d in data.delivery.orEmpty()) add(d.docketNo, d.boxId)

        // 2) pickup boxes
        for (p in data.pickups.orEmpty()) {
            for (dk in p.dockets.orEmpty()) {
                if (dk.boxes.isNullOrEmpty()) add(dk.docketNo, null)   // docket with no boxes still shows in list
                for (b in dk.boxes.orEmpty()) add(dk.docketNo, b.boxId)
            }
        }
    }

    /** Insert boxes that are not in the local DB yet. Already-saved boxes keep their scan / short / damage state. */
    private suspend fun saveBoxesToDb() {
        val existingBoxCodes = dao.getExistingBoxCodes(tripId).toHashSet()

        // boxes already scanned on this device (for duplicate-scan block)
        boxes.clear()
        boxes.addAll(dao.getScannedBoxCodes(tripId))

        val itemsToInsert = mutableListOf<LocalLoadbarcode>()
        for ((docketNo, boxIds) in docketBoxMap) {
            for (boxId in boxIds) {
                if (boxId in existingBoxCodes) continue
                itemsToInsert.add(
                    LocalLoadbarcode(
                        tripId     = tripId,
                        barcodebox = boxId,
                        docket     = docketNo,
                        scanStatus = "Unscanned",
                        scanAt     = "",
                        scanId     = "",
                        eventId    = ""
                    )
                )
                existingBoxCodes.add(boxId)
            }
        }
        if (itemsToInsert.isNotEmpty()) dao.insertAll(itemsToInsert)
    }

    /** Docket rows with LOCAL scan progress, so the list updates after every scan. */
    private suspend fun refreshDocketList() {
        val rows = dao.getByTrip(tripId)
        val scannedByDocket = rows
            .filter { it.scanStatus.equals("Scanned", true) }
            .groupingBy { it.docket.orEmpty() }
            .eachCount()

        docketAdapter.submitDockets(
            docketlist.map { docketNo ->
                UnloadDocketRow(
                    docketNo     = docketNo,
                    boxIds       = docketBoxMap[docketNo].orEmpty(),
                    scannedBoxes = scannedByDocket[docketNo] ?: 0
                )
            }
        )
    }

    private fun bindSummary() {
        val d = tripUnloading ?: return
        binding.tvVehicleNo.text    = "-"        // not in getTripUnLoading response
        binding.tvDocketsCount.text = docketlist.size.toString()
        binding.tvDocketCount.text  = docketlist.size.toString()
        binding.tvExcess.text       = "0"        // not in getTripUnLoading response
        // Short / Damage / Scanned / Progress -> handled by refreshLocalSummary()
    }

    override fun onItemClick(docketNo: String) {
        startActivity(
            Intent(
                this, LocalLoadBoxDetails::class.java).
            putExtra("tripId",tripId)
                .putExtra("docketNo",docketNo)
        )

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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US)
        }
    }

    private fun clearBarcodeEdittext() {
        binding.barcodeText.setText("")
        binding.barcodeText.requestFocus()
    }
    private fun validatebarcode(barcodevalue: String) {
        if (barcodevalue.length < 6) { showInvalid(); return }

        lifecycleScope.launch {
            val count = dao.updateScanFieldsByBarcode(
                barcodebox = barcodevalue,
                tripId     = tripId,
                scanStatus = "Scanned",
                scanAt     = Utils.getCurrentTimestamp(),
                scanId     = Utils.GenrateUUID()
            )
            if (count > 0) {
                boxes.add(barcodevalue)
                speak("Scan")
                refreshLocalSummary()          // ✅ update tvShort / tvDamage / progress
            } else {
                speak("Invalid Barcode")
                Utils.showDialog(
                    this@LocalUnLoading, "error",
                    "Invalid Barcode ", R.drawable.ic_baseline_error_24
                )
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
    private fun refreshLocalSummary() {
        lifecycleScope.launch {
            try {
                val total   = dao.getTotalCount(tripId)
                val scanned = dao.getScannedCount(tripId)
                val short   = dao.getShortCount(tripId)
                val damage  = dao.getDamageCount(tripId)

                // Scanned counter
                binding.tvscannedbarcode.text = scanned.toString()
                binding.tvTotalProgress.text  = "$scanned/$total"
                binding.progressTotal.max      = if (total == 0) 1 else total
                binding.progressTotal.progress = scanned
                binding.tvManifestProgress.text =
                    if (total == 0) "0%" else "${(scanned * 100) / total}%"

                // Short / Damage — now live from DB ✅
                binding.tvShort.text  = short.toString()
                binding.tvDamage.text = damage.toString()

                // Docket count comes from trip (server) — leave it
                binding.tvDocketCount.text = docketlist.size.toString()
                binding.tvBoxesCount.text  = total.toString()

                refreshDocketList()            // docket rows show local scanned/total
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalUnLoading, e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // User may have marked Short / Damage in LocalUnLoading
        if (::dao.isInitialized) refreshLocalSummary()
    }
    private fun Generateevenidbulk(){
        if(Utils.haveInternet(this)){
            ShowDailog()
            lifecycleScope.launch {
                val items: MutableList<Item> = mutableListOf()
                val a=   dao.getByTrip(tripId)
                for ((index, i) in a.withIndex()) {
                    if(i.scanStatus.equals("Scanned",true) && i.eventId.equals(""))
                        items.add(
                            Item(
                                boxCode = i.barcodebox.toString(),
                                branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                                clientRef = Utils.GenrateUUID(),// serial + Utils.getCurrentTimestampunique() + (index + 1),
                                companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                                deviceId = serial,
                                eventType = "IN_SCAN",
                                expectedDocketNo = i.docket.toString(),
                                locationCode = "",
                                scanStage = "UNLOAD",
                                scannedAt =i.scanAt.toString(),
                                scannedBy = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),

                                )
                        )
                }


                val mod= BulkIdGenMod(companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(),items=items)
                if(items.size==0){
                    DismissDialog()
                    submitBulk()
                    return@launch
                }
                try {

                    val resp=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkScan("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)

                    DismissDialog()

                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.status == "SUCCESS") {
                            var errors = ""
                            for (r in body.data?.results.orEmpty()) {
                                if (r.outcome == "APPLIED" || r.outcome == "APPLIED_IDEMPOTENT") {
                                    // box accepted
                                } else {
                                    errors += r.boxCode + " " + r.outcome + " " + r.boxStatus.toString() + "\n"
                                }
                            }

                            if (errors.isNotEmpty()) {
                                Utils.showDialog(this@LocalUnLoading, "error", errors, R.drawable.ic_baseline_error_24)
                            } else {
                                submitBulk(body.data.results)
                            }
                        } else {
                            val msg = body?.message?.toString()
                                ?.takeIf { it.isNotBlank() && it != "null" } ?: "Something went wrong"
                            Utils.showDialog(this@LocalUnLoading, "error", msg, R.drawable.ic_baseline_error_24)
                        }
                    } else {
                        Utils.showDialog(this@LocalUnLoading, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }
                }catch (e: Exception) {
                    DismissDialog()
                    Utils.gettrycatcherror(this@LocalUnLoading,e)
                }

            }

        }
    }


    private fun submitBulk(result: List<Result>) {
        ShowDailog()
        lifecycleScope.launch {


            try {
                for(i in result){
                    dao.updateeventid(i.boxCode,tripId,i.publicEventId.toString())
                }

                val rows = dao.getByTrip(tripId)   // all LocalLoadbarcode rows for this trip

                if (rows.isEmpty()) {
                    Toast.makeText(this@LocalUnLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val items      = mutableListOf<BulkItem>()
                val exceptions = mutableListOf<BulkException>()

                for (row in rows) {
                    val docketNo = row.docket.orEmpty()
                    val boxId    = row.barcodebox.orEmpty()
                    if (docketNo.isBlank() || boxId.isBlank()) continue

                    val isScanned = row.scanStatus.equals("Scanned", true)
                    val isDamage  = row.isDamage.equals("Damage", true)
                    val isShort   = row.shortremarks.isNotBlank()

                    // ✅ items[]  → every SCANNED box (damaged boxes are ALSO scanned)
                    if (isScanned) {
                        items.add(
                            BulkItem(
                                docketNo     = docketNo,
                                boxId        = boxId,
                                publicScanId = row.eventId.orEmpty()
                            )
                        )
                    }

                    // ✅ exceptions[]  → DAMAGED_BOX  (damaged boxes can also be in items)
                    if (isDamage) {
                        exceptions.add(
                            BulkException(
                                docketNo      = docketNo,
                                boxId         = boxId,
                                exceptionType = "DAMAGED_BOX",
                                reason        = row.damageremarks.ifBlank { "damaged" },
                                publicScanId  = row.scanId.orEmpty().ifBlank { null }
                            )
                        )
                    }

                    // ✅ exceptions[]  → BOX_MISSING  (short, not scanned)
                    if (isShort && !isScanned) {
                        exceptions.add(
                            BulkException(
                                docketNo      = docketNo,
                                boxId         = boxId,
                                exceptionType = "BOX_MISSING",
                                reason        = row.shortremarks.ifBlank { "not at the dock" },
                                publicScanId  = null
                            )
                        )
                    }
                }

                if (items.isEmpty() && exceptions.isEmpty()) {
                    Toast.makeText(this@LocalUnLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
                    return@launch
                }                // ✅ "complete" only when EVERY box is scanned OR short
                val allAccounted = rows.all {
                    it.scanStatus.equals("Scanned", true) ||
                            it.shortremarks.isNotBlank()
                }

                val res = serviceinterface.submitBulkUnLoading(
                    auth        = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo      = tripId,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                    request = BulkLoadingRequest(
                        closeIfComplete = allAccounted,
                        actor           = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                        items           = items,
                        exceptions      =if(exceptions.isEmpty()) null else exceptions
                    )
                )

                if (res.isSuccessful) {
                    Utils.showDialog(this@LocalUnLoading, "Success", "Saved successfully", R.drawable.ic_check_circle_green) {
                        setResult(RESULT_OK)
                        startActivity(Intent(this@LocalUnLoading, LocalTripAct::class.java))
                        finish()
                    }
                    Toast.makeText(this@LocalUnLoading, "Saved successfully", Toast.LENGTH_SHORT).show()
                   // if (allAccounted) confirmCloseLoading()
                } else {
                    try{
                        Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LocalUnLoading,e)
                    }
                    //Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalUnLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }
    private fun submitBulk() {
        ShowDailog()
        lifecycleScope.launch {


            try {


                val rows = dao.getByTrip(tripId)   // all LocalLoadbarcode rows for this trip

                if (rows.isEmpty()) {
                    Toast.makeText(this@LocalUnLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val items      = mutableListOf<BulkItem>()
                val exceptions = mutableListOf<BulkException>()

                for (row in rows) {
                    val docketNo = row.docket.orEmpty()
                    val boxId    = row.barcodebox.orEmpty()
                    if (docketNo.isBlank() || boxId.isBlank()) continue

                    val isScanned = row.scanStatus.equals("Scanned", true)
                    val isDamage  = row.isDamage.equals("Damage", true)
                    val isShort   = row.shortremarks.isNotBlank()

                    // ✅ items[]  → every SCANNED box (damaged boxes are ALSO scanned)
                    if (isScanned) {
                        items.add(
                            BulkItem(
                                docketNo     = docketNo,
                                boxId        = boxId,
                                publicScanId = row.eventId.orEmpty()
                            )
                        )
                    }

                    // ✅ exceptions[]  → DAMAGED_BOX  (damaged boxes can also be in items)
                    if (isDamage) {
                        exceptions.add(
                            BulkException(
                                docketNo      = docketNo,
                                boxId         = boxId,
                                exceptionType = "DAMAGED_BOX",
                                reason        = row.damageremarks.ifBlank { "damaged" },
                                publicScanId  = row.scanId.orEmpty().ifBlank { null }
                            )
                        )
                    }

                    // ✅ exceptions[]  → BOX_MISSING  (short, not scanned)
                    if (isShort && !isScanned) {
                        exceptions.add(
                            BulkException(
                                docketNo      = docketNo,
                                boxId         = boxId,
                                exceptionType = "BOX_MISSING",
                                reason        = row.shortremarks.ifBlank { "not at the dock" },
                                publicScanId  = null
                            )
                        )
                    }
                }

                if (items.isEmpty() && exceptions.isEmpty()) {
                    Toast.makeText(this@LocalUnLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
                    return@launch
                }                // ✅ "complete" only when EVERY box is scanned OR short
                val allAccounted = rows.all {
                    it.scanStatus.equals("Scanned", true) ||
                            it.shortremarks.isNotBlank()
                }

                val res = serviceinterface.submitBulkUnLoading(
                    auth        = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo      = tripId,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                    request = BulkLoadingRequest(
                        closeIfComplete = allAccounted,
                        actor           = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                        items           = items,
                        exceptions      =if(exceptions.isEmpty()) null else exceptions
                    )
                )

                if (res.isSuccessful) {
                    Utils.showDialog(this@LocalUnLoading, "Success", "Saved successfully", R.drawable.ic_check_circle_green) {
                        setResult(RESULT_OK)
                        startActivity(Intent(this@LocalUnLoading, LocalTripAct::class.java))
                        finish()
                    }
                    Toast.makeText(this@LocalUnLoading, "Saved successfully", Toast.LENGTH_SHORT).show()
                    // if (allAccounted) confirmCloseLoading()
                } else {
                    try{
                        Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LocalUnLoading,e)
                    }
                    //Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalUnLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }




    // ================= 3. Activity (LocalLoading) =================
    private fun confirmCloseLoading() {
        AlertDialog.Builder(this)
            .setTitle("Close loading")
            .setMessage("Close loading for $tripId? This cannot be undone.")
            .setPositiveButton("Close") { _, _ -> closeLoading() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun closeLoading() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val res = serviceinterface.closeLoading(
                    auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo = tripId,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                    request = CloseLoadingRequest(actor = AtomOpsApp.getPreferences(Constants.EMP_ID, ""))
                )
                if (res.isSuccessful) {
                    Toast.makeText(this@LocalUnLoading, "Loading closed", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    try{
                        Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@LocalUnLoading,e)
                    }
                  //  Utils.showDialog(this@LocalUnLoading, "error", res.errorMessage(), R.drawable.ic_baseline_error_24)
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalUnLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }

// In onCreate (after adding a Save button with id "btnSave" to the layout):
// binding.btnSave.setOnClickListener { submitBulk() }
}