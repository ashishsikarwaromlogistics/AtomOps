package com.example.atomops.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.adapters.LocalLoadDocketAdapter
import com.example.atomops.database.daos.LocalLoadbarcodeDao
import com.example.atomops.database.tables.LocalLoadbarcode
import com.example.atomops.databinding.ActivityLocalLoadingBinding
import com.example.atomops.localclasses.SaveValidationResult
import com.example.atomops.models.CloseLoadingRequest
import com.example.atomops.models.bulkids.BulkIdGenMod
import com.example.atomops.models.bulkids.Item
import com.example.atomops.models.bulkload.BulkException
import com.example.atomops.models.bulkload.BulkItem
import com.example.atomops.models.bulkload.BulkLoadingRequest
import com.example.atomops.models.localloadbox.DocketBoxInfo
import com.example.atomops.models.localloadbox.DocketBoxesRequest
import com.example.atomops.models.localtripload.TripDocket
import com.example.atomops.models.localtripload.TripLoadingData
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import java.util.Locale

class LocalLoading : AppCompatActivity() , LocalLoadDocketAdapter.OnItemClickListener, TextToSpeech.OnInitListener {

    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }

    private lateinit var binding: ActivityLocalLoadingBinding
    private fun ShowDailog() { if (!appprogress.isShowing) appprogress.show() }
    private fun DismissDialog() { if (appprogress.isShowing) appprogress.dismiss() }
    private val serviceinterface by lazy { ApiClient.getRetrofitclient().create(ServiceInterface::class.java)  }
   var tripId=""
    private var docketBoxes: List<DocketBoxInfo> = emptyList()
    var item=this//LocalLoadDocketAdapter.OnItemClickListener
    private val docketAdapter = LocalLoadDocketAdapter(item)
    private var tripLoading: TripLoadingData? = null
// in onCreate, before loadTripLoading(tripId):

    lateinit var dao: LocalLoadbarcodeDao
    private lateinit var textToSpeech: TextToSpeech
    private var boxes: MutableList<String> = mutableListOf()
     var serial:String=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_local_loading)
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
        binding.tvTripId.setText(tripId)
        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
                    if(boxes.contains(value)) {
                        speak("Duplicate Barcode")
                        Utils.showDialog(
                            this@LocalLoading,
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
                        speak("All boxes scanned")
                        saveLoading(result)          // TODO: call your save / submit API
                    } else {
                        // ❌ pending boxes → speak + show remaining dockets
                        speak("${result.pendingBoxes} boxes remaining")
                        showPendingDialog(result)
                    }
                } catch (e: Exception) {
                    Utils.gettrycatcherror(this@LocalLoading, e)
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
            this@LocalLoading,
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
                val res = serviceinterface.getTripLoading(
                    auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo = tripNo,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "")
                )
                if (res.code() == 200) {
                    tripLoading = res.body()?.data
                    bindSummary()
                    docketAdapter.submitDockets(tripLoading?.dockets ?: emptyList())

                    val docketNos = tripLoading?.dockets?.mapNotNull { it.docketNo } ?: emptyList()
                    if (docketNos.isNotEmpty()) loadDocketBoxes(docketNos)
                } else {
                    Utils.handleHttpError(this@LocalLoading, res.code())
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }
    private suspend fun loadDocketBoxes(docketNos: List<String>) {
        val request = DocketBoxesRequest(
            companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toIntOrNull() ?: 0,
            docketNos = docketNos
        )
        val res = serviceinterface.getDocketBoxes(
            auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
            request = request
        )

        if (res.code() == 200) {
            docketBoxes = res.body()?.data?.dockets ?: emptyList()

            // 1. Codes already stored in DB for this trip (for dedupe insert)
            val existingBoxCodes = dao.getExistingBoxCodes(tripId).toHashSet()

            // 2. Codes that are ALREADY SCANNED (for duplicate-scan block)
            boxes.clear()
            boxes.addAll(dao.getScannedBoxCodes(tripId))

            val itemsToInsert = mutableListOf<LocalLoadbarcode>()

            for (docket in docketBoxes) {
                for (box in docket.boxes.orEmpty()) {
                    val boxCode = box.boxCode ?: continue

                    if (boxCode in existingBoxCodes) {
                        existingBoxCodes.add(boxCode)
                        continue                       // already in DB → don't insert again
                    }

                    itemsToInsert.add(
                        LocalLoadbarcode(
                            tripId     = tripId,
                            barcodebox = boxCode,
                            docket     = docket.docketNo,
                            scanStatus = "Unscanned",
                            scanAt     = "",
                            scanId     = "",
                            eventId    = ""
                        )
                    )
                    existingBoxCodes.add(boxCode)      // dedupe within response
                }
            }

            if (itemsToInsert.isNotEmpty()) dao.insertAll(itemsToInsert)
            refreshLocalSummary()
            binding.tvscannedbarcode.text = boxes.size.toString()

            val map = docketBoxes
                .filter { it.docketNo != null }
                .associate { it.docketNo!! to it.boxes.orEmpty().filter { b -> !b.voided } }
            docketAdapter.submitBoxes(map)
        } else {
            Utils.handleHttpError(this@LocalLoading, res.code())
        }
    }
    /*private suspend fun loadDocketBoxes(docketNos: List<String>) {
        val request = DocketBoxesRequest(
            companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "").toIntOrNull() ?: 0,
            docketNos = docketNos
        )
        val res = serviceinterface.getDocketBoxes(
            auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
            request = request
        )
        if (res.code() == 200) {
            docketBoxes = res.body()?.data?.dockets ?: emptyList()

            for (docket in docketBoxes) {
                for(i in docket.boxes){
                    val item = LocalLoadbarcode(
                        tripId = tripId,
                        barcodebox = i.boxCode,
                        docket = docket.docketNo,
                        scanStatus = "",
                        scanAt = System.currentTimeMillis().toString(),
                        scanId = "",
                        eventId = ""
                    )
                    dao.insert(item)
                }

            }

            val map = docketBoxes
                .filter { it.docketNo != null }
                .associate { it.docketNo!! to it.boxes.orEmpty().filter { b -> !b.voided } }
               docketAdapter.submitBoxes(map)
        } else {
            Utils.handleHttpError(this@LocalLoading, res.code())
        }
    }*/

    private fun bindSummary() {
        val d = tripLoading ?: return
        binding.tvVehicleNo.text    = d.vehicleNo ?: "-"
        binding.tvDocketsCount.text = d.totalDockets.toString()
        binding.tvDocketCount.text  = d.totalDockets.toString()
        binding.tvExcess.text       = d.excessBoxes.toString()
        // Short / Damage / Scanned / Progress → handled by refreshLocalSummary()
    }

    override fun onItemClick(docket: TripDocket) {
        startActivity(
            Intent(
                    this, LocalLoading::class.java).
                putExtra("tripId",tripId)
                .putExtra("docketNo",docket.docketNo)
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
                    this@LocalLoading, "error",
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
                binding.tvDocketCount.text = (tripLoading?.totalDockets ?: 0).toString()
                binding.tvBoxesCount.text  = total.toString()
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoading, e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // User may have marked Short / Damage in LocalLoading
        if (::dao.isInitialized) refreshLocalSummary()
    }
    private fun Generateevenidbulk(){
        if(Utils.haveInternet(this)){
            ShowDailog()
            lifecycleScope.launch {
                val items: MutableList<Item> = mutableListOf()
                val a=   dao.getByTrip(tripId)
                for ((index, i) in a.withIndex()) {
                    if(i.scanStatus.equals("Scanned",true))
                        items.add(
                            Item(
                                boxCode = i.barcodebox.toString(),
                                branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                                clientRef = Utils.GenrateUUID(),// serial + Utils.getCurrentTimestampunique() + (index + 1),
                                companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                                deviceId = serial,
                                eventType = "OUT_SCAN",
                                expectedDocketNo = i.docket.toString(),
                                locationCode = "",
                                scanStage = "LOAD",
                                scannedAt =i.scanAt.toString(),
                                scannedBy = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),

                                )
                        )
                }


                val mod= BulkIdGenMod(companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(),items=items)
                try {
                    val resp=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkScan("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                    if(resp.code()==200){
                        DismissDialog()
                        if(resp.body()!!.status.equals("SUCCESS")){
                            submitBulk()
                        }
                    }
                    else {
                        DismissDialog()
                        Utils.handleHttpError(this@LocalLoading,resp.code())
                        // Utils.showDialog(this@LocalLoading,"error"+resp.code(), Utils.handleHttpError(this@LocalLoading,resp.code()),R.drawable.ic_baseline_error_24)

                    }
                }catch (e: Exception) {
                    DismissDialog()
                    Utils.gettrycatcherror(this@LocalLoading,e)
                }

            }

        }
    }


    private fun submitBulk() {
        ShowDailog()
        lifecycleScope.launch {
            try {
                val rows = dao.getByTrip(tripId)   // all LocalLoadbarcode rows for this trip

                if (rows.isEmpty()) {
                    Toast.makeText(this@LocalLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
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
                                publicScanId = row.scanId.orEmpty()
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
                    Toast.makeText(this@LocalLoading, "Nothing to submit", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ "complete" only when EVERY box is scanned OR short
                val allAccounted = rows.all {
                    it.scanStatus.equals("Scanned", true) ||
                            it.shortremarks.isNotBlank()
                }

                val res = serviceinterface.submitBulkLoading(
                    auth        = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripNo      = tripId,
                    companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                    request = BulkLoadingRequest(
                        closeIfComplete = allAccounted,
                        actor           = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                        items           = items,
                        exceptions      = exceptions
                    )
                )

                if (res.code() == 200) {
                    Toast.makeText(this@LocalLoading, "Saved successfully", Toast.LENGTH_SHORT).show()
                    if (allAccounted) confirmCloseLoading()
                } else {
                    val err = res.errorBody()?.string()
                    if (!err.isNullOrBlank()) {
                        Toast.makeText(this@LocalLoading, err, Toast.LENGTH_LONG).show()
                    } else {
                        Utils.handleHttpError(this@LocalLoading, res.code())
                    }
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoading, e)
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
                if (res.code() == 200) {
                    Toast.makeText(this@LocalLoading, "Loading closed", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    // show server message if present (e.g. boxes still pending)
                    val err = res.errorBody()?.string()
                    if (!err.isNullOrBlank() && res.code() == 400) {
                        Toast.makeText(this@LocalLoading, err, Toast.LENGTH_LONG).show()
                    } else {
                        Utils.handleHttpError(this@LocalLoading, res.code())
                    }
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@LocalLoading, e)
            } finally {
                DismissDialog()
            }
        }
    }

// In onCreate (after adding a Save button with id "btnSave" to the layout):
// binding.btnSave.setOnClickListener { submitBulk() }
}