package com.example.atomops.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.inputmethod.EditorInfo
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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.UtilsK
import com.example.atomops.adapters.DocketItem
import com.example.atomops.adapters.ScanAdapter
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.database.daos.TripDetailsDao
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.databinding.ActivityLoadingBinding
import com.example.atomops.databinding.ActivityUnloadingBinding
import com.example.atomops.dialogs.AddSealBottomSheet
import com.example.atomops.models.DocketBox
import com.example.atomops.models.bulkids.BulkIdGenMod
import com.example.atomops.models.bulkids.Item
import com.example.atomops.models.closem.CloseMod
import com.example.atomops.models.completeunload.CompleteUnloadmod
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.String

class UnloadingAct : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityUnloadingBinding
    //lateinit var adapter: LoadingAdap
    lateinit var scanapdater: ScanAdapter
    lateinit var tripDetailsDao: TripDetailsDao
    lateinit var barcodedao: BarcodeDao
    //(val items: List<DocketBox>

    private var docketlisttable: List<TripDetailsTable> = emptyList()   // raw trips from DB
    private var docketlist: List<DocketBox> = emptyList()   // raw trips from DB
    private var docketItem: MutableList<DocketItem> = mutableListOf() // data given to adapter
    private lateinit var textToSpeech: TextToSpeech
    lateinit var serial: String
    lateinit var tripId:String
    private var boxes: MutableList<String> = mutableListOf()
    var currentManifest: String = ""
    var manifestList: List<String> = mutableListOf()
    var selectedPosition=0
    lateinit var pd: ProgressDialog
    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }
    var tripdestination=""
    private var manifestJob: Job? = null   // load of the selected manifest; cancelled when another tab is tapped
    private fun ShowDailog(){
        if(!appprogress.isShowing){
            appprogress.show()
        }
    }
    private fun DismissDialog(){
        if(appprogress.isShowing){
            appprogress.dismiss()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unloading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tripdestination=intent.getStringExtra("tripdestination").toString()
        pd= ProgressDialog(this)
        //  addseal()
        tripId=intent.getStringExtra("tripid").toString()
        binding.tvTripId.setText("Unload"+tripId)
        serial= Utils.getSerial(this)
        textToSpeech = TextToSpeech(this, this)
        tripDetailsDao = (application as AtomOpsApp).database.tripDetailDao()
        barcodedao = (application as AtomOpsApp).database.getBarcodeDao()
        setupManifestTabs()

        binding.rvDockets.setHasFixedSize(false)
        binding.rvDockets.layoutManager = LinearLayoutManager(this)
        (binding.rvDockets.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false



        /*finish()
        startActivity(Intent(this@UnloadingAct, LoadingUnloading::class.java))
*/


        /* lifecycleScope.launch {
             docketlist = tripDetailsDao.getByTrip(tripId)
             docketItem = docketlist.map { trip ->
                 DocketItem(
                     trip,
                     (1..trip.boxes).map { j ->
                         BarcodeTable(
                             box = trip.docketNo + Utils.formatNumber(j.toInt()),
                             timeScan = "",
                             manifest = trip.manifestNo,
                             tripId = trip.tripId.toString(),
                             status = false
                         )
                     }.toMutableList()
                 )
             }.toMutableList()


             val locallist=barcodedao.getByTrip(tripId)
             for(i in locallist){
             val dataexist=  docketItem.find { data->
                   data.docket.docketNo==i.box.substring(0,i.box.length-4)
               }
                 if(dataexist!=null){
                  val boxexist=   dataexist.scannedBoxes.find {
                         data->data.box== i.box
                     }
                     if(boxexist!=null){
                         boxexist.status=i.status
                         boxexist.timeScan=i.timeScan

                     }


                 }
             }


             // TODO: if scans are persisted, load them from barcodedao here and
             // set status/timeScan on the matching boxes so progress survives a restart.
             binding.tvDocketCount.setText(docketItem.size.toString())
             adapter.submit(docketItem)
         }*/

        clearBarcodeEdittext()

        // Filter hooks: connect these to whatever controls you add to the layout.
        // binding.etSearch.doAfterTextChanged { adapter.setQuery(it?.toString().orEmpty()) }
        // adapter.setFilter(DocketFilter.PENDING)   // ALL / PENDING / COMPLETE

        binding.save.setOnClickListener {
            // LaodingImage()
            //   closeUnloading()
            getManifestProgress()
        }


        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                var value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
                  //  value= Utils.revertTransform(value)
                    if(boxes.contains(value)) {
                        speak("Duplicate Barcode")
                        Utils.showDialog(
                            this@UnloadingAct,
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
    }

    private fun Generateevenidbulk(){
        if(Utils.haveInternet(this)){
            lifecycleScope.launch {
                val items: MutableList<Item> = mutableListOf()
                val a=   barcodedao.getByTrip(tripId)
                for ((index, i) in a.withIndex()) {
                    if(i.isScan)
                        items.add(
                            Item(
                                boxCode = i.box,
                                branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                                clientRef = i.clientRef,// serial + Utils.getCurrentTimestampunique() + (index + 1),
                                companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),
                                deviceId = serial,
                                eventType = "IN_SCAN",
                                expectedDocketNo = i.docketno,
                                locationCode = "",
                                scanStage = "UNLOAD",
                                scannedAt =/*"2026-10-22T10:32:00+05:30" ,*/Utils.getCurrentTimestamp(),
                                scannedBy = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),

                                )
                        )
                }


                val mod= BulkIdGenMod(companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(),items=items)
                try {
                    val resp=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkScan("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                    if (resp.isSuccessful) {
                        if(resp.body()?.status == "SUCCESS"){
                            //601769  //  Utils.showDialog(this@UnloadingAct,"success",resp.body().message.toString(),R.drawable.ic_baseline_check_circle_24)
                            var errors=""
                            for(i in resp.body()?.data?.results.orEmpty()){
                                if(i.outcome.equals("APPLIED")||i.outcome.equals("APPLIED_IDEMPOTENT")){
                                    barcodedao.updateeventid(i.boxCode,i.publicEventId.toString())
                                }else{
                                    errors=errors+i.boxCode+" "+i.outcome+" "+i.boxStatus.toString()+"\n"
                                }
                            }
                            if(errors.length>0){
                                Utils.showDialog(this@UnloadingAct,"error",errors,R.drawable.ic_baseline_error_24)
                            }else{//now check condtion of all boxes are they scan or not or should short remarks be given
                                val   listofboxes= barcodedao.getPendingBoxes(currentManifest)
                                if(listofboxes.size==0)
                                    saveData()
                                else {
                                    Utils.showDialog(this@UnloadingAct,"please scan Remaining box Status",listofboxes.toString(),R.drawable.ic_baseline_error_24)

                                }
                            }
                        } else {
                            showError(bodyMessage(resp.body()?.message))   // status != SUCCESS (was silent before)
                        }
                    }
                    else {
                        try{
                            Utils.showDialog(this@UnloadingAct, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                        }catch (e:Exception){
                            Utils.gettrycatcherror(this@UnloadingAct,e)
                        }
                    }
                }catch (e: Exception) {
                    Utils.gettrycatcherror(this@UnloadingAct,e)
                }

            }

        }
    }

    private fun validatebarcode(barcodevalue: String) {
        if (barcodevalue.length < 6) {
            showInvalid()
            return
        }
        else{lifecycleScope.launch {
            val count= barcodedao.updateStatus(barcodevalue, manfiest =currentManifest ,true)
            if(count>0){
                boxes.add(barcodevalue)
                speak("Scan")
                refreshDocketList(currentManifest)   // updates "scanned/total" in ScanAdapter + counters
            }
            else{
                speak("Invalid Barcode")
                Utils.showDialog(this@UnloadingAct,"error","Invalid Barcode for manifest ${currentManifest}",R.drawable.ic_baseline_error_24)
            }
        }

        }

        /*  when (adapter.markScanned(barcodevalue, Utils.getCurrentTimestamp())) {
              ScanResult.OK -> {
                  lifecycleScope.launch {
                  val update=    barcodedao.UpdateByBoxwithtime(barcodevalue, Utils.getCurrentTimestamp(),true)
                  if(update>0){
                      Log.d("ashishsikarwar","updated${barcodevalue}")
                  }
                      else{
                          Log.d("ashishsikarwar","not updated")
                      }
                  }
                  binding.rvDockets.scrollToPosition(0)
                  speak("Scan")
              }
              ScanResult.DUPLICATE -> speak("Already scanned")
              ScanResult.NOT_FOUND -> showInvalid()
          }*/
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
                        // barcodedao.updateBox(oldBox, newBox)
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

    fun selectTab(position: Int) {
        selectedPosition = position

        for (i in 0 until binding.llManifestTabs.childCount) {
            val tab = binding.llManifestTabs.getChildAt(i) as TextView

            if (i == selectedPosition) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(
                    android.graphics.Color.WHITE
                )
            } else {
                tab.background = null
                tab.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        tab.context,
                        R.color.text_primary
                    )
                )
            }
        }
    }
    private fun setupManifestTabs() {
        lifecycleScope.launch {
            manifestList=   tripDetailsDao.getByTripandmanifiestforUnload(tripId, AtomOpsApp.getPreferences(
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

                        // Handle selected manifest here
                        onManifestSelected(manifest, index)
                    }
                }

                binding.llManifestTabs.addView(textView)
            }

            if (manifestList.isNotEmpty()) {
                selectTab(0)
                onManifestSelected(manifestList[0], 0)   // called once, for the first tab only
            }
        }

    }
    private fun TripDetailsTable.toDocketBox(scanned: List<String>, totalBoxes: Int) = DocketBox(
        // Manifest / trip-level fields
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

        // Docket-level fields
        docketNo = docketNo,
        boxes = totalBoxes,            // was boxCountTotal = whole MANIFEST total -> wrong "x/y"
        weightKg = weightKg,
        serviceMode = serviceMode,
        agingHours = agingHours,
        documentStatus = documentStatus,
        missingMarkedAt = missingMarkedAt,
        missingMarkedBy = missingMarkedBy,
        fragile = fragile,
        weightCheckRequired = weightCheckRequired,
        liquidOil = liquidOil,

        scanboxelist = scanned         // SCANNED box codes of this docket
    )

    /**
     * Rebuilds the docket list + counters for [manifest] from the local DB.
     * ScanAdapter gets a NEW list every time, so "scanned/total" always updates.
     */
    private suspend fun refreshDocketList(manifest: String) {
        val rows = barcodedao.getByManifest(manifest)
        if (manifest != currentManifest) return            // user switched tab meanwhile

        val rowsByDocket = rows.groupBy { it.docketno }
        boxes.clear()
        boxes.addAll(rows.filter { it.isScan }.map { it.box })

        docketlist = docketlisttable.map { t ->
            val docketRows = rowsByDocket[t.docketNo].orEmpty()
            t.toDocketBox(
                scanned    = docketRows.filter { it.isScan }.map { it.box },
                totalBoxes = if (docketRows.isNotEmpty()) docketRows.size else t.boxes
            )
        }

        // keep the scroll position when the list is rebuilt
        val state = binding.rvDockets.layoutManager?.onSaveInstanceState()
        scanapdater = ScanAdapter(docketlist, onItemClick = {
            val intent = Intent(this@UnloadingAct, BoxDetailsAct::class.java)
            intent.putExtra("docketno", it.docketNo)
            intent.putExtra("tripId", tripId)
            startActivity(intent)
        })
        binding.rvDockets.adapter = scanapdater
        state?.let { binding.rvDockets.layoutManager?.onRestoreInstanceState(it) }

        binding.tvDocketCount.text = docketlist.size.toString()
        binding.tvDocketsCount.text = docketlist.size.toString()
        binding.tvBoxesCount.text = rows.size.toString()
        binding.tvscannedbarcode.text = boxes.size.toString()
        binding.tvManifestProgress.text = "/" + rows.size

        val c = barcodedao.getShortDamageCount(manifest)
        binding.tvShort.text  = c.shortCount.toString()
        binding.tvDamage.text = c.damageCount.toString()
    }

    private fun onManifestSelected(manifest: String, position: Int) {
        currentManifest=manifest
        manifestJob?.cancel()
        manifestJob = lifecycleScope.launch {
            docketlisttable = tripDetailsDao.getByManifest(manifest).distinctBy { it.docketNo }

            // 1) show what is already in the local DB (works offline too)
            refreshDocketList(manifest)

            // 2) sync boxes from the server, then redraw
            if(Utils.haveInternet(this@UnloadingAct)){
                val mydocketlist = docketlisttable.map { it.docketNo }
                val mod= FIndBoxMod(AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toIntOrNull() ?: 0, mydocketlist)
                try{
                    val response= ApiClient.getRetrofitclient().create(ServiceInterface::class.java).getAllboxes("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                    if (manifest != currentManifest) return@launch
                    if (response.isSuccessful) {
                        val body = response.body()
                        if(body?.status=="SUCCESS"){
                            val dockets = body.data?.dockets.orEmpty()
                            if(dockets.isNotEmpty()){
                                // one query instead of 2 per box
                                val existing = barcodedao.getByManifest(manifest).map { it.box }.toHashSet()
                                for(i in dockets){
                                    for(j in i.boxes){
                                        if (j.boxCode in existing) continue
                                        if (j.voided || !j.status.equals("IN_TRANSIT")) continue
                                        barcodedao.insert(
                                            BarcodeTable(
                                                docketno= i.docketNo,
                                                box = j.boxCode,
                                                timeScan = "",
                                                status = j.status,
                                                voided = j.voided,
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
                                        existing.add(j.boxCode)
                                    }
                                }
                                refreshDocketList(manifest)
                            }
                            else {
                                Utils.showDialog(this@UnloadingAct,"error","no data found",R.drawable.ic_baseline_error_24)
                            }
                        }else{
                            showError(bodyMessage(body?.message))
                        }
                    }
                    else{
                        try{
                            Utils.showDialog(this@UnloadingAct, "error", response.errorMessage(), R.drawable.ic_baseline_error_24)
                        }catch (e:Exception){
                            Utils.gettrycatcherror(this@UnloadingAct,e)
                        }
                    }
                }
                catch (e: CancellationException){
                    throw e     // tab switched, not an error
                }
                catch (e: Exception){
                    Utils.gettrycatcherror(this@UnloadingAct,e)
                }
            }
        }
    }

    private fun saveData(){
        lifecycleScope.launch {
            val shortbox=mutableListOf<ShortBoxe>()// MutableList<ShortBoxe>()
            val myshorbox=barcodedao.getByManifestshort(manifest = currentManifest)
            for(i in myshorbox){
                shortbox.add(ShortBoxe(  boxCode=i.box,
                    docketNo= i.docketno,
                    remarks= i.isshort))
            }
            val item=mutableListOf<com.example.atomops.models.loadtrip.Item>()
            val myitem=  barcodedao.getByManifestwithoutshort(currentManifest)
            for(i in myitem){
                item.add(com.example.atomops.models.loadtrip.Item(
                    boxCode=i.box,
                    damagePhotoUrl=i.damageurl,
                    damageRemarks= i.damage,
                    docketNo=i.docketno,
                    scanEventId=i.eventid

                ))
            }
            val mod= LoadTripMod(actor = AtomOpsApp.getPreferences(Constants.EMP_ID,""), branch = AtomOpsApp.getPreferences(Constants.BCODE,""),companyCode= AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(), items =item, shortBoxes = shortbox)
            try {
                val resp=ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkUnLoadBoxes("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),currentManifest,mod)
                if (resp.isSuccessful) {
                    if(resp.body()?.status == "SUCCESS"){
                        barcodedao.updateLoadstatus(currentManifest)
                        //LoadingClose()
                        LaodingImage()
                        Utils.showDialog(this@UnloadingAct,"SUCCESS",resp.body()?.message.toString(),R.drawable.green_check)
                    }
                    else   Utils.showDialog(this@UnloadingAct,"Fail",resp.body()?.message.toString(),R.drawable.ic_baseline_error_24)

                }
                else {
                    try{
                        Utils.showDialog(this@UnloadingAct, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@UnloadingAct,e)
                    }
                }
            }catch (e: Exception){
                Utils.gettrycatcherror(this@UnloadingAct,e)
            }

        }
    }

    fun LaodingImage(){
        captureAndUpload { path, success ->
            if (success && path != null) {
                // closeSeal(sealNumber,path)
                lifecycleScope.launch {
                    val mod= LoadImageMod(
                        actor = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                        branchCode = AtomOpsApp.getPreferences(Constants.BCODE, ""),
                        captureStage = "UNLOAD_100_PERCENT",
                        geoLat  =56.43324,
                        geoLong=56.43324,
                        photoDocumentId=path
                    )
                    ShowDailog()
                    try{
                        val response=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).loadingPhoto(
                            "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                            currentManifest,
                            AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),

                            mod
                        )
                        if (response.isSuccessful) {
                            DismissDialog()
                            Utils.showDialog(this@UnloadingAct,"Alert","Do you want to close Loading?",R.drawable.green_check,{
                                UnLoadingClose()
                            })
                        }
                        else{
                            DismissDialog()
                            try{
                                Utils.showDialog(this@UnloadingAct, "error", response.errorMessage(), R.drawable.ic_baseline_error_24)
                            }catch (e:Exception){
                                Utils.gettrycatcherror(this@UnloadingAct,e)
                            }
                        }
                    }
                    catch (e: Exception){
                        DismissDialog()
                        Utils.gettrycatcherror(this@UnloadingAct,e)

                    }
                }
                Log.d("Upload", "Done, file at: $path")
                // e.g. show preview: ivPreview.setImageURI(Uri.fromFile(File(path)))
            } else {
                Log.d("Upload", "Failed or cancelled")
            }
        }


    }

    fun closeSeal(sealNumber: String, photoUri: String) {
        lifecycleScope.launch {
            try {
                val mod= SealMod(AtomOpsApp.getPreferences(Constants.EMP_ID,""),
                    AtomOpsApp.getPreferences(Constants.BCODE,""),
                    photoUri,
                    sealNumber,
                    "PHYSICAL")
                val resp= ApiClient.getRetrofitclient().create(ServiceInterface::class.java).loadingseal(
                    "Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),tripId,mod)
                if (resp.isSuccessful) {
                    Utils.showDialog(this@UnloadingAct,"Success",resp.body()?.message.toString(),R.drawable.green_check)
                    deleteDatafronted()
                }else{
                    try{
                        Utils.showDialog(this@UnloadingAct, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@UnloadingAct,e)
                    }
                }
            }catch (e: Exception){
                Utils.gettrycatcherror(this@UnloadingAct,e)
            }}
    }
    fun UnLoadingClose() {
        lifecycleScope.launch {
            try {
                /*
                                val isAllBoxLoaded = barcodedao.areAllBoxesLoaded(tripId)

                                if (!isAllBoxLoaded) {
                                    Utils.showDialog(
                                        this@UnloadingAct,
                                        "Error",
                                        "Please load all boxes of the manifest",
                                        R.drawable.ic_baseline_error_24
                                    )
                                    return@launch
                                }*///because only one manfiest will show
                ShowDailog()
                val mod = CloseMod(
                    AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                    AtomOpsApp.getPreferences(Constants.BCODE, "")
                )

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .UnLoadingClose(
                        "Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        currentManifest,
                        mod
                    )

                if (resp.isSuccessful) {
                    DismissDialog()
                    if (selectedPosition < manifestList.size - 1) {
                        selectedPosition++
                        selectTab(selectedPosition)
                        onManifestSelected(manifestList[selectedPosition], selectedPosition)
                    } else {
                        Utils.showDialog(
                            this@UnloadingAct,
                            "Success",
                            resp.body()?.message.toString() ?: "Manifiest closed"+"\nPlease Complete Unloading ",
                            R.drawable.green_check,{
                                if(Utils.haveInternet(this@UnloadingAct)){


                                    if(tripdestination.equals(AtomOpsApp.getPreferences(Constants.BCODE,""))){
                                        CompleteTrip()}
                                    else {
                                        finish()
                                        startActivity(Intent(this@UnloadingAct, LoadingUnloading::class.java))

                                    }

                                }

                                //closeUnloading()

                            }
                        )
                        //   addseal()
                    }

                    Utils.showDialog(
                        this@UnloadingAct,
                        "Success",
                        resp.body()?.message.toString() ?: "Manifest closed",
                        R.drawable.green_check
                    )
                }
                else {
                    DismissDialog()     // loader was left open on error before
                    try{
                        Utils.showDialog(this@UnloadingAct, "error", resp.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@UnloadingAct,e)
                    }
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@UnloadingAct, e)
            }
        }
    }

    fun CompleteTrip(){
        ShowDailog()
        val mod= CompleteUnloadmod(AtomOpsApp.getPreferences(Constants.EMP_ID,""),AtomOpsApp.getPreferences(Constants.BCODE,""))
        lifecycleScope.launch {
            try {
                DismissDialog()
                val response=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).CompleteUnload(
                    token = "Bearer "+ AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),
                    tripNo = tripId,
                    body = mod
                )
                if (response.isSuccessful) {
                    Utils.showDialog(
                        this@UnloadingAct,
                        "Success",
                        response.body()?.message.toString() ?:    "Unload completed",
                        R.drawable.green_check,{
                            finish()
                            startActivity(Intent(this@UnloadingAct, LoadingUnloading::class.java))
                        }
                    )
                }
                else {
                    try{
                        Utils.showDialog(this@UnloadingAct, "error", response.errorMessage(), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(this@UnloadingAct,e)
                    }
                }
            }
            catch (e: Exception){
                DismissDialog()
                Utils.gettrycatcherror(this@UnloadingAct,e)
            }
        }




    }


    fun addseal(){
        AddSealBottomSheet { sealNumber ->
            // call your Gate Out API here
            captureAndUpload { path, success ->
                if (success && path != null) {
                    closeSeal(sealNumber,path)
                    Log.d("Upload", "Done, file at: $path")
                    // e.g. show preview: ivPreview.setImageURI(Uri.fromFile(File(path)))
                } else {
                    Log.d("Upload", "Failed or cancelled")
                }
            }

        }.show(supportFragmentManager, AddSealBottomSheet.TAG)
    }

    private fun deleteDatafronted(){
        boxes.clear()
        lifecycleScope.launch {
            barcodedao.deleteByTrip(tripId)
        }
    }
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

    // ===== PUBLIC FUNCTION YOU CALL =====
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
            var success = false
            var compressed: File? = null
            try {
                // run heavy work off the main thread
                compressed = withContext(Dispatchers.IO) { UtilsK.compressImage(file) }

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

                if (response.isSuccessful) {
                    success = true
                    onUploadComplete?.invoke(response.body()?.data?.id, success)
                    onUploadComplete = null
                    Toast.makeText(this@UnloadingAct, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UnloadingAct, "Error ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UnloadingAct, e.message ?: "Upload failed", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
                // Return path AFTER API response (success or failure)
                onUploadComplete?.invoke(file.absolutePath, success)
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
    private fun setLoading(isloading: Boolean){
        if(isloading){
            pd.show()
        }else pd.dismiss()
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
                            if(tripdestination.equals(AtomOpsApp.getPreferences(Constants.BCODE,""))){
                                CompleteTrip()}
                            else {
                                Utils.showDialog(
                                    this@UnloadingAct, "Alert", "This manifest already close . plz select another manifest",
                                    R.drawable.ic_baseline_error_24
                                )
                            }


                        }
                        else if(body.data.status.toString().equals("ARRIVED")){
                            Generateevenidbulk()
                        }
                        else if(body.data.status.toString().equals("UNLOADING") && body.data.progress.boxesPending!=0){
                            saveData()
                        }
                        else if(body.data.status.toString().equals("UNLOADING") && body.data.progress.boxesPending==0){
                            UnLoadingClose()
                        }
                        else if(body.data.progress.photo100 != null){
                            UnLoadingClose()
                        }
                        else{
                            LaodingImage()
                        }
                    } else {
                        showError(bodyMessage(body?.message))
                    }
                } else {
                    showError(resp.errorMessage())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@UnloadingAct, e)
            }
        }
    }
    private fun showError(msg: String) {
        Utils.showDialog(this, "error", msg, R.drawable.ic_baseline_error_24)
    }
    private fun bodyMessage(msg: Any?): String =
        msg?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: GENERIC_ERROR

    override fun onResume() {
        super.onResume()
        if (currentManifest.isNotEmpty() && ::barcodedao.isInitialized) {
            lifecycleScope.launch { refreshDocketList(currentManifest) }   // also updates short/damage
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

}