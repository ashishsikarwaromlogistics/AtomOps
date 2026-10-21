package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.atomops.R
import com.example.atomops.databinding.ActivityDashboardBinding

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
         setclick()

    }

    private fun setclick() {
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