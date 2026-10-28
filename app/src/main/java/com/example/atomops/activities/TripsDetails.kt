package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.database.daos.TripDetailsDao
import com.example.atomops.database.daos.UserDao
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.database.tables.User
import com.example.atomops.databinding.ActivityTripsDetailsBinding
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch

class TripsDetails : AppCompatActivity() {
    lateinit var binding: ActivityTripsDetailsBinding
    //val db = (application as AtomOpsApp).database
    lateinit var userDao: UserDao //db.userDao()
    lateinit var tripDetailsDao: TripDetailsDao //db.userDao()
    lateinit var getbarcodedao: BarcodeDao //db.userDao()
    var tripId=""
    var currentstatus=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= DataBindingUtil.setContentView(this,R.layout.activity_trips_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentstatus=intent.getStringExtra("currentstatus").toString()
        userDao = (application as AtomOpsApp).database.userDao()
        getbarcodedao = (application as AtomOpsApp).database.getBarcodeDao()
        tripDetailsDao = (application as AtomOpsApp).database.tripDetailDao()
       /* lifecycleScope.launch {

            val user = userDao.insertUser(User(firstName = "John", lastName = "Doe"))   // suspend function in DAO
            val users = userDao.getAll()
            println(users)
        }*/
        tripId=intent.getStringExtra("tripid").toString()
        binding.tvTripId.text=tripId

        getTripDetails(tripId)
        binding.btnStartLoading.setOnClickListener {
            if(currentstatus.equals("UNLOADING")){
                startActivity(Intent(this, UnloadingAct::class.java).putExtra("tripid",intent.getStringExtra("tripid")))

            }
            else
            startActivity(Intent(this,LoadingAct::class.java).putExtra("tripid",intent.getStringExtra("tripid")))
        }
    }
    fun getTripDetails(tripid:String){
        lifecycleScope.launch {
            val resp=ApiClient.getRetrofitclient().create(ServiceInterface::class.java).getTripDetails("Bearer "+ AtomOpsApp.getPreferences(
                Constants.JWTTOKEN,""),tripid)
            if(resp?.isSuccessful==true){
                val data=resp.body()?.data
                binding.tvService.setText(data?.trip?.routeType)
                binding.tvDock.text = data?.trip?.currentDockNo ?: "--"
                binding.tvVehicleNo.setText(data?.trip?.vehicleNo)
                binding.tvTripDate.text=data?.trip?.createdAt?.substring(0,11)
                lifecycleScope.launch {
                    for (i in data!!.manifests) {
                        for (j in i.dockets) {
                            val exists = tripDetailsDao.existsDocket(j.docketNo)
                            if (!exists) {
                                tripDetailsDao.insert(
                                    TripDetailsTable(
                                        manifestNo = i.manifestNo,
                                        legSequence = i.legSequence,
                                        sourceBranch = i.sourceBranch,
                                        destinationBranch = i.destinationBranch,
                                        cnCount = i.cnCount,
                                        boxCountTotal = i.legSequence, // Verify this field
                                        status = i.status,
                                        boxesScanned = i.boxesScanned,
                                        boxesUnloaded = i.boxesUnloaded,
                                        tripId = tripid,
                                        docketNo = j.docketNo,
                                        boxes = j.boxes,
                                        weightKg = j.weightKg.toDouble(),
                                        serviceMode = j.serviceMode,
                                        agingHours = j.agingHours,
                                        documentStatus = j.documentStatus,
                                        missingMarkedAt = j.missingMarkedAt,
                                        missingMarkedBy = j.missingMarkedBy,
                                        fragile = j.fragile,
                                        weightCheckRequired = j.weightCheckRequired,
                                        liquidOil = j.liquidOil
                                    )
                                )
                            }
                        }
                    }

                    val docketlist = tripDetailsDao.getByTrip(tripId)
                    for(k in docketlist){
                        for (i in 1..k.boxes.toInt()) {
                            val boxNo = k.docketNo + Utils.formatNumber(i).toString()

                              }
                    }
                }


               // binding.tvDriverName.setText(data?.trip?.d)
            }
        }
    }

}