package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.databinding.ActivityDashboardBinding
import com.example.atomops.dialogs.DockYardBottomSheet

class Dashboard : AppCompatActivity() {
     lateinit var binding: ActivityDashboardBinding
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
         binding= DataBindingUtil.setContentView(this,R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootDashboard)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
         AtomOpsApp.savePreferences(Constants.BCODE,"1003")

         setText()
         setclick()

    }
    private fun setText(){
        binding.tvname.text="Hi,"+ AtomOpsApp.getPreferences(Constants.EMPNAME,"")
        binding.tvbranch.text= AtomOpsApp.getPreferences(Constants.BCODE,"")+", "+ AtomOpsApp.getPreferences(Constants.BNAME,"")
    }

    private fun setclick() {
        binding.crdmidmile.setOnClickListener {
            startActivity(Intent(this,MiddleMileAct::class.java))
        }
        binding.crdlocal.setOnClickListener {
            startActivity(Intent(this, LocalTripAct::class.java))
        }
        binding.crdyrdmgmt.setOnClickListener {
            DockYardBottomSheet { option ->
                when (option) {
                    DockYardBottomSheet.Option.GATE_OPERATIONS -> { /* open gate ops */
                   startActivity(Intent(this,GateOperationAct::class.java))
                    }

                    DockYardBottomSheet.Option.DOCK_MANAGEMENT -> { /* open dock mgmt */
                        startActivity(Intent(this, DockMgmtAct::class.java))
                    }
                }
            }.show(supportFragmentManager, DockYardBottomSheet.TAG)
          //  startActivity(Intent(this, YardMgmtAct::class.java))
        }
        binding.crdrequest.setOnClickListener {
            startActivity(Intent(this,ReqMngmtAct::class.java))
        }
        binding.navPrq.setOnClickListener {
            startActivity(Intent(this,PrqAct::class.java))
        }

        binding.navTrips.setOnClickListener {
           startActivity(Intent(this,AllTripsAct::class.java))
            //startActivity(Intent(this,AllTripsAct::class.java))
        }
        binding.loadunload.setOnClickListener {
           startActivity(Intent(this, LoadingUnloading::class.java))
            //startActivity(Intent(this,AllTripsAct::class.java))
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, MyProfile::class.java))

        }

    }
}