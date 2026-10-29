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
import com.example.atomops.models.findbox.FIndBoxMod
import com.example.atomops.models.loadphoto.LoadImageMod
import com.example.atomops.models.loadtrip.LoadTripMod
import com.example.atomops.models.loadtrip.ShortBoxe
import com.example.atomops.models.seal.SealMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.Dispatchers
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unloading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
            Generateevenidbulk()
        }


        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
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
                    if(resp.code()==200){
                        if(resp.body()!!.status.equals("SUCCESS")){
                            //601769  //  Utils.showDialog(this@UnloadingAct,"success",resp.body().message.toString(),R.drawable.ic_baseline_check_circle_24)
                            var errors=""
                            for(i in resp.body()!!.data.results){
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
                        }
                    }
                    else {
                        Utils.showDialog(this@UnloadingAct,"error"+resp.code(), Utils.getHttpErrorMessage(resp.code(),resp.errorBody().toString()),R.drawable.ic_baseline_error_24)

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
                binding.tvscannedbarcode.setText(boxes.size.toString())
                speak("Scan")
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
            manifestList=   tripDetailsDao.getByTripandmanifiest(tripId)
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
                    onManifestSelected(manifest, 0)
                }

                binding.llManifestTabs.addView(textView)
            }

            if (manifestList.isNotEmpty()) {
                selectTab(0)
            }
        }

    }
    private fun onManifestSelected(manifest: String, position: Int) {
        currentManifest=manifest
        //LoadingClose()
        lifecycleScope.launch {
            docketlisttable= tripDetailsDao.getByManifest(manifest)
            docketlist = docketlisttable.map { item ->
                DocketBox(
                    // Manifest / trip-level fields
                    manifestNo = item.manifestNo,
                    legSequence = item.legSequence,
                    sourceBranch = item.sourceBranch,
                    destinationBranch = item.destinationBranch,
                    cnCount = item.cnCount,
                    boxCountTotal = item.boxCountTotal,
                    status = item.status,
                    boxesScanned = item.boxesScanned,
                    boxesUnloaded = item.boxesUnloaded,
                    tripId = item.tripId,

                    // Docket-level fields
                    docketNo = item.docketNo,
                    boxes = item.boxCountTotal,
                    weightKg = 0.0,
                    serviceMode = null,
                    agingHours = 0,
                    documentStatus = null,
                    missingMarkedAt = null,
                    missingMarkedBy = null,
                    fragile = false,
                    weightCheckRequired = false,
                    liquidOil = false,

                    // Initially empty; update when needed
                    scanboxelist = emptyList()
                )
            }

            // docketlist = tripDetailsDao.getByTrip(tripId)
            scanapdater = ScanAdapter(docketlist)
            binding.rvDockets.adapter = scanapdater
            if(Utils.haveInternet(this@UnloadingAct)){
                val mydocketlist=mutableListOf<String>()
                for(i in docketlist){
                    mydocketlist.add(i.docketNo)
                }
                val mod= FIndBoxMod(AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(), mydocketlist)
                try{
                    val response= ApiClient.getRetrofitclient().create(ServiceInterface::class.java).getAllboxes("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                    if(response.code()==200){
                        if(response.body()?.status=="SUCCESS"){
                            if(response.body()?.data!=null) {
                                if( response.body()!!.data.dockets.size>0){
                                    binding.tvDocketCount.setText( response.body()!!.data.dockets.size.toString())
                                    binding.tvDocketsCount.setText( response.body()!!.data.dockets.size.toString())
                                    boxes.clear()
                                    for(i in response.body()!!.data.dockets){
                                        for(j in i.boxes){
                                            if(barcodedao.checkexist(j.boxCode)>0){
                                                if(barcodedao.checkScan(j.boxCode))
                                                    boxes.add(j.boxCode)
                                                docketlist = docketlist.map { item ->
                                                    if(item.docketNo == i.docketNo) {
                                                        item.copy(
                                                            scanboxelist = item.scanboxelist + j.boxCode
                                                        )
                                                    } else {
                                                        item
                                                    }
                                                }
                                                Log.d("ashishsikarwar","box already exist")
                                            }
                                            else{

                                                val barcodedata = BarcodeTable(
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
                                                if(!j.voided && j.status.equals("PICKED"))
                                                    barcodedao.insert(barcodedata)
                                            }

                                        }

                                    }


                                    val totalbarcode=barcodedao.getByManifest(manifest).size.toString()
                                    binding.tvBoxesCount.setText(totalbarcode)
                                    binding.tvscannedbarcode.setText(boxes.size.toString())
                                    binding.tvManifestProgress.setText("/"+totalbarcode)


                                }
                                else {
                                    Utils.showDialog(this@UnloadingAct,"error","no data found",R.drawable.ic_baseline_error_24)
                                }
                            }
                            else{
                                Utils.showDialog(this@UnloadingAct,"error",response.body()?.message.toString(),R.drawable.ic_baseline_error_24)
                            }
                        }else{
                            Utils.showDialog(this@UnloadingAct,"error",response.body()?.message.toString(),R.drawable.ic_baseline_error_24)
                        }

                    }
                    else{
                        Utils.geterrormessagewithcode(this@UnloadingAct,response.code(),"")
                    }
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
                if(resp.code()==200){
                    if(resp.body()!!.status.equals("SUCCESS")){
                        barcodedao.updateLoadstatus(currentManifest)
                        LoadingClose()
                        //LaodingImage()
                        Utils.showDialog(this@UnloadingAct,"SUCCESS",resp.body()!!.message.toString(),R.drawable.green_check)
                    }
                    else   Utils.showDialog(this@UnloadingAct,"Fail",resp.body()!!.message.toString(),R.drawable.green_check)

                }
                else {
                    Utils.geterrormessagewithcode(this@UnloadingAct,resp.code(),"")
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
                        captureStage = "LOAD_100_PERCENT",
                        geoLat  =56.43324,
                        geoLong=56.43324,
                        photoUrl=path
                    )
                    val response=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).loadingPhoto(
                        "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        currentManifest,
                        AtomOpsApp.getPreferences(Constants.COMP_CODE, ""),

                        mod
                    )
                    if(response.code()==200 || response.code()==201){
                        LoadingClose()
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
                    AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),tripId,mod)
                if(resp.code()==200 || resp.code()==201){
                    Utils.showDialog(this@UnloadingAct,"Success",resp.body()!!.message.toString(),R.drawable.green_check)
                    deleteDatafronted()
                }else{
                    Utils.geterrormessagewithcode(this@UnloadingAct,resp.code(),"")
                }
            }catch (e: Exception){
                Utils.gettrycatcherror(this@UnloadingAct,e)
            }}
    }
    fun LoadingClose() {
        lifecycleScope.launch {
            try {
                val isAllBoxLoaded = barcodedao.areAllBoxesLoaded(tripId)

                if (!isAllBoxLoaded) {
                    Utils.showDialog(
                        this@UnloadingAct,
                        "Error",
                        "Please load all boxes of the manifest",
                        R.drawable.ic_baseline_error_24
                    )
                    return@launch
                }

                val mod = CloseMod(
                    AtomOpsApp.getPreferences(Constants.EMP_ID, ""),
                    AtomOpsApp.getPreferences(Constants.BCODE, "")
                )

                val resp = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .UnLoadingClose(
                        AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        currentManifest,
                        mod
                    )

                if (resp.code() == 200) {
                    if (selectedPosition < manifestList.size - 1) {
                        selectedPosition++
                        selectTab(selectedPosition)
                        onManifestSelected(manifestList[selectedPosition], selectedPosition)
                    } else {
                        Utils.showDialog(
                            this@UnloadingAct,
                            "Success",
                            resp.body()?.message.toString() ?: "Manifest closed",
                            R.drawable.green_check
                        )
                     //   addseal()
                    }

                    Utils.showDialog(
                        this@UnloadingAct,
                        "Success",
                        resp.body()?.message.toString() ?: "Manifest closed",
                        R.drawable.green_check
                    )
                } else {
                    // Error payload lives in errorBody(), not body()
                    val errorMsg = try {
                        val json = JSONObject(resp.errorBody()?.string() ?: "")
                        json.optString("message", "Something went wrong")
                    } catch (e: Exception) {
                        "Something went wrong (code ${resp.code()})"
                    }

                    Utils.showDialog(
                        this@UnloadingAct,
                        "Error",
                        errorMsg,
                        R.drawable.ic_baseline_error_24
                    )
                }
            } catch (e: Exception) {
                Utils.gettrycatcherror(this@UnloadingAct, e)
            }
        }
    }



    fun addseal(){
        AddSealBottomSheet { sealNumber, photoUri ->
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

}