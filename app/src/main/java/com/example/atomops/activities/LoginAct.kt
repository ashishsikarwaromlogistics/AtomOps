package com.example.atomops.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.util.Util
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.databinding.ActivityLoginBinding
import com.example.atomops.models.login.DeviceInfo
import com.example.atomops.models.login.LoginMod
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.JwtDecoder
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch

class LoginAct : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    lateinit var pd: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= DataBindingUtil.setContentView(this,R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Bottom padding uses whichever is larger: the nav bar
            // or the keyboard, so content isn't hidden behind either.
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, imeInsets.bottom)
            )
            insets
        }
        pd= ProgressDialog(this)
        binding.btnLogin.setOnClickListener {
            pd.show()
            lifecycleScope.launch {
                val deviceinfo= DeviceInfo(Utils.getDeviceIMEI(this@LoginAct),"Scanner", Utils.getLocalIpAddress(),"Android")
             //   val mod= LoginMod(deviceinfo, password = "K8CSU646m42$",username = "ashis")
                //val mod= LoginMod(deviceinfo, password = "asdfghjkl",username = "vaibhav21")
                val mod= LoginMod(deviceinfo, password = "asdfghjkl",username = "amit.shah")
                //val mod= LoginMod(deviceinfo, password = "Chandan@1998",username = "mis.admin.400021")
             //   val mod= LoginMod(deviceinfo, password = binding.etPassword.text.toString(),username = binding.etUserId.text.toString())
                try {
                    val resp= ApiClient.getRetrofitclient().create(ServiceInterface::class.java).Login(mod)
                    pd.dismiss()
                    if (resp?.isSuccessful == true && resp.body()?.status == "success") {
                        val data = resp.body()?.data
                        if (data != null) {
                            // Save tokens securely, then navigate
                            val token = "${data.tokenType.trim()} ${data.accessToken}"
                            val payload = JwtDecoder.decodePayload(token)

                            if (payload != null) {

                                // Get String and numeric values
                                val companyCode = payload.optInt("companyCode")
                                val branchCode = payload.optString("branchCode")
                                val companyId = payload.optString("companyId")
                                val userId = payload.optString("userId")
                                val username = payload.optString("sub")
                                val emailId = payload.optString("emailId")
                                val tokenType = payload.optString("type")

                                // Get Boolean value
                                val allIndia = payload.optBoolean("allIndia")

                                // Get token timestamps
                                val issuedAt = payload.optLong("iat")
                                val expiry = payload.optLong("exp")
                                AtomOpsApp.savePreferences(Constants.BCODE,branchCode)
                                AtomOpsApp.savePreferences(Constants.BNAME,"")
                                AtomOpsApp.savePreferences(Constants.COMP_CODE,companyCode.toString())

                                Log.d("JWT", "Company Code: $companyCode")
                                Log.d("JWT", "Branch Code: $branchCode")
                                Log.d("JWT", "Company ID: $companyId")
                                Log.d("JWT", "User ID: $userId")
                                Log.d("JWT", "Username: $username")
                                Log.d("JWT", "Email: $emailId")
                                Log.d("JWT", "Token Type: $tokenType")
                                Log.d("JWT", "All India: $allIndia")
                                Log.d("JWT", "Issued At: $issuedAt")
                                Log.d("JWT", "Expiry: $expiry")
                            }
                            AtomOpsApp.savePreferences(Constants.ID,data.user.id)
                            AtomOpsApp.savePreferences(Constants.EMP_CODE,data.user.username)
                            AtomOpsApp.savePreferences(Constants.EMP_ID,data.user.id)
                            AtomOpsApp.savePreferences(Constants.EMPNAME,data.user.firstName+" "+data.user.lastName)
                            AtomOpsApp.savePreferences(Constants.EMAIL,data.user.email)
                            AtomOpsApp.savePreferences(Constants.JWTTOKEN,data.accessToken)
                            AtomOpsApp.savePreferences(Constants.REFRESH,data.refreshToken)
                            AtomOpsApp.savePreferences(Constants.ISLOGIN,"true")

                            startActivity(Intent(this@LoginAct, Dashboard::class.java))
                        //    startActivity(Intent(this@LoginAct, BoxDetailsAct::class.java))
                        } else {
                            showError("Login failed")
                        }
                    } else {
                        Utils.showDialog(this@LoginAct,"error", Utils.getHttpErrorMessage(resp!!.code(),""),R.drawable.ic_baseline_error_24)
                    }
                }
                catch (e: Exception){
                    pd.dismiss()
                   Utils.showDialog(this@LoginAct,"error", Utils.getErrorMessage(e),R.drawable.ic_baseline_error_24)
                }


            }

        }
    }
    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}