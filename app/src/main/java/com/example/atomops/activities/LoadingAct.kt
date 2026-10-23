package com.example.atomops.activities

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.adapters.DocketItem
import com.example.atomops.adapters.ScanAdapter
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.database.daos.TripDetailsDao
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.databinding.ActivityLoadingBinding
import com.example.atomops.models.bulkids.BulkIdGenMod
import com.example.atomops.models.bulkids.Item
import com.example.atomops.models.findbox.FIndBoxMod
import com.example.atomops.models.loadtrip.LoadTripMod
import com.example.atomops.models.loadtrip.ShortBoxe
import com.example.atomops.models.savedata.SaveDataMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.Any
import kotlin.String

class LoadingAct : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityLoadingBinding
    //lateinit var adapter: LoadingAdap
    lateinit var scanapdater: ScanAdapter
    lateinit var tripDetailsDao: TripDetailsDao
    lateinit var barcodedao: BarcodeDao

    private var docketlist: List<TripDetailsTable> = emptyList()   // raw trips from DB
    private var docketItem: MutableList<DocketItem> = mutableListOf() // data given to adapter
    private lateinit var textToSpeech: TextToSpeech
    lateinit var serial: String
     lateinit var tripId:String
    private var boxes: MutableList<String> = mutableListOf()
    var currentManifest: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tripId=intent.getStringExtra("tripid").toString()

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
                                eventType = "OUT_SCAN",
                                expectedDocketNo = i.docketno,
                                locationCode = "",
                                scanStage = "LOAD",
                                scannedAt ="2026-10-22T10:32:00+05:30" ,//Utils.getCurrentTimestamp(),
                                scannedBy = AtomOpsApp.getPreferences(Constants.EMP_ID, ""),

                            )
                        )
                    }


                val mod= BulkIdGenMod(companyCode = AtomOpsApp.getPreferences(Constants.COMP_CODE,"").toInt(),items=items)
                 try {
                     val resp=  ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkScan("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                     if(resp.code()==200){
                         if(resp.body()!!.status.equals("SUCCESS")){
                           //601769  //  Utils.showDialog(this@LoadingAct,"success",resp.body().message.toString(),R.drawable.ic_baseline_check_circle_24)
                            for(i in resp.body()!!.data.results){
                             if(i.outcome.equals("APPLIED")||i.outcome.equals("APPLIED_IDEMPOTENT")){
                                 barcodedao.updateeventid(i.boxCode,i.publicEventId.toString())
                             }else{

                             }
                            }
                             saveData()
                         }
                     }
                     else {
                         Utils.showDialog(this@LoadingAct,"error"+resp.code(), Utils.getHttpErrorMessage(resp.code(),resp.errorBody().toString()),R.drawable.ic_baseline_error_24)

                     }
                 }catch (e: Exception) {
                     Utils.gettrycatcherror(this@LoadingAct,e)
                 }

                }

            }
        }


        binding.barcodeText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val value = binding.barcodeText.text.toString().trim()
                if (value.length > 5) {
                    if(boxes.contains(value)) {
                        speak("Duplicate Barcode")
                        Utils.showDialog(
                            this@LoadingAct,
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

    private fun validatebarcode(barcodevalue: String) {
        if (barcodevalue.length < 6) {
            showInvalid()
            return
        }
        else{lifecycleScope.launch {
           val count= barcodedao.updateStatus(barcodevalue, manfiest =currentManifest ,true)
            if(count>0){
                speak("Scan")
            }
            else{
                speak("Invalid Barcode")
                Utils.showDialog(this@LoadingAct,"error","Invalid Barcode for manifest ${currentManifest}",R.drawable.ic_baseline_error_24)
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
    private fun setupManifestTabs() {
      lifecycleScope.launch {
        val  manifestList=   tripDetailsDao.getByTripandmanifiest(tripId)
          binding.llManifestTabs.removeAllViews()

          var selectedPosition = 0

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
       lifecycleScope.launch {
             docketlist= tripDetailsDao.getByManifest(manifest)
          // docketlist = tripDetailsDao.getByTrip(tripId)
           scanapdater = ScanAdapter(docketlist)
           binding.rvDockets.adapter = scanapdater
          if(Utils.haveInternet(this@LoadingAct)){
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
                          for(i in response.body()!!.data.dockets){
                          for(j in i.boxes){
                              if(barcodedao.checkexist(j.boxCode)>0){
                                  if(barcodedao.checkScan(j.boxCode))
                                  boxes.add(j.boxCode)
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
                                      clientRef = Utils.GenrateUUID()
                                  )
                                  barcodedao.insert(barcodedata)
                              }

                          }

                          }


                          val totalbarcode=barcodedao.getByManifest(manifest).size.toString()
                          binding.tvBoxesCount.setText(totalbarcode)
                          binding.tvManifestProgress.setText(boxes.size.toString()+"/"+totalbarcode)


                      }
                         else {
                             Utils.showDialog(this@LoadingAct,"error","no data found",R.drawable.ic_baseline_error_24)
                         }
                     }
                     else{
                         Utils.showDialog(this@LoadingAct,"error",response.body()?.message.toString(),R.drawable.ic_baseline_error_24)
                     }
                    }else{
                        Utils.showDialog(this@LoadingAct,"error",response.body()?.message.toString(),R.drawable.ic_baseline_error_24)
                    }

                }
                else{
                    Utils.geterrormessagewithcode(this@LoadingAct,response.code(),"")
                }
            }
            catch (e: Exception){
                Utils.gettrycatcherror(this@LoadingAct,e)
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
             val resp=ApiClient.getRetrofitclient().create(ServiceInterface::class.java).bulkLoadBoxes("Bearer "+AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),tripId,mod)
             if(resp.code()==200){}
         }catch (e: Exception){}

       }
        }

}