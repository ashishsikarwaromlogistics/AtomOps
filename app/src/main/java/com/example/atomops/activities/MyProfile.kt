package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AccountSecurityBottomSheet
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.databinding.ActivityMyProfileBinding
import com.example.atomops.models.password.ChangePassMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch

class MyProfile : AppCompatActivity() {
lateinit var binding: ActivityMyProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
       binding= DataBindingUtil. setContentView(this,R.layout.activity_my_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.tvName.setText(AtomOpsApp.getPreferences(Constants.EMPNAME,""))
        binding.tvEmpId.setText(AtomOpsApp.getPreferences(Constants.EMP_CODE,""))
        binding.tvEmployeeId.setText(AtomOpsApp.getPreferences(Constants.ID,""))
        binding.tvEmail.setText(AtomOpsApp.getPreferences(Constants.EMAIL,""))

       binding.tvBranch.setText(AtomOpsApp.getPreferences(Constants.BNAME,"")+AtomOpsApp.getPreferences(Constants.BCODE,""))

        binding.btnUpdate.setOnClickListener {
            AccountSecurityBottomSheet { current, new ->
                val mod=ChangePassMod(current,current,new)
                lifecycleScope.launch {
                 val resp=   ApiClient.getRetrofitclient().create(ServiceInterface::class.java).ChangePassword(
                        AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),mod)
                   try {
                       if(resp.code()==200){}
                       else{
                           Utils.geterrormessagewithcode(this@MyProfile,resp.code(),"")
                       }
                   }catch (e: Exception){
                       Utils.gettrycatcherror(this@MyProfile,e)
                   }

                }
                // call your change-password API here
            }.show(supportFragmentManager, "AccountSecurity")
        }
       // binding.tvName.setText(AtomOpsApp.getPreferences(Constants.EMPNAME,""))
        //binding.tvName.setText(AtomOpsApp.getPreferences(Constants.EMPNAME,""))

        binding.cardDevice.setOnClickListener {

        }


        binding.llLogout.setOnClickListener {
            // 1. Clear session data
            AtomOpsApp.logoutdevice()   // or remove JWT token, user data etc.

            // 2. Open Login and wipe the whole back stack
            val intent = Intent(this, LoginAct::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }
}