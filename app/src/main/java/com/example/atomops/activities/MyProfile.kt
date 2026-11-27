package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AccountSecurityBottomSheet
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.database.daos.AcessBranchesDao
import com.example.atomops.databinding.ActivityMyProfileBinding
import com.example.atomops.dialogs.DeviceSessionsBottomSheet
import com.example.atomops.models.password.ChangePassMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch

class MyProfile : AppCompatActivity() {
lateinit var binding: ActivityMyProfileBinding
    val appprogress: AppProgressDialog by lazy { AppProgressDialog(this) }
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
    lateinit var dao: AcessBranchesDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
       binding= DataBindingUtil. setContentView(this,R.layout.activity_my_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        dao = (application as AtomOpsApp).database.acessBranchesDao()
        loadBranches()
        binding.ivBack.setOnClickListener { finish() }
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
            DeviceSessionsBottomSheet.show(supportFragmentManager)
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
    private fun loadBranches() {
        lifecycleScope.launch {
            val list = dao.getAll()                       // List<AcessBranches>
            val labels = list.map { it.branch }

            if (list.isEmpty()) {
                binding.spBranches.isEnabled = false
                binding.spBranches.adapter = ArrayAdapter(
                    this@MyProfile,
                    android.R.layout.simple_spinner_item,
                    listOf("No branches")
                )
                return@launch
            }

            binding.spBranches.adapter = ArrayAdapter(
                this@MyProfile,
                android.R.layout.simple_spinner_item,
                labels
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // 1. Find the saved branch
            val saved = AtomOpsApp.getPreferences(Constants.BCODE, "")
            val index = list.indexOfFirst { it.branch.trim() == saved.trim() }
            // If your entity has a separate code field, use it instead:
            // val index = list.indexOfFirst { it.branchCode.trim() == saved.trim() }

            // 2. Select it BEFORE attaching the listener (false = no animation)
            if (index >= 0) binding.spBranches.setSelection(index, false)

            // 3. Attach the listener after, so the initial selection doesn't trigger it
            binding.spBranches.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        val selected = list[position]
                        AtomOpsApp.savePreferences(Constants.BCODE, selected.branch)
                        // use selected.branch here
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }
}