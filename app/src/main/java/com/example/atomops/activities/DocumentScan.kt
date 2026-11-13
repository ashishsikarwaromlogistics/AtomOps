package com.example.atomops.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AppProgressDialog
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.models.offlinedata.Docket
import com.example.atomops.DocketAdapter
import com.example.atomops.R
import com.example.atomops.databinding.ActivityDocumentScanBinding
import com.example.atomops.models.scandoc.ScanDocMod
import com.example.atomops.models.scandoc.SubmitManifiest
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch

class DocumentScan : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentScanBinding
    private lateinit var adapter: DocketAdapter
    private val api: ServiceInterface by lazy {
        ApiClient.getRetrofitclient().create(ServiceInterface::class.java)
    }

    // Source of truth; replace with API / Room data
    private val allDockets: MutableList<Docket> = mutableListOf()
    var tripid = ""
    var empid = ""
    var compcode = ""
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_document_scan)

        // Header gets the status-bar inset so the gradient draws behind it
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.setPadding(bars.left, bars.top, bars.right, 0)
            binding.main.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        compcode = AtomOpsApp.getPreferences(Constants.COMP_CODE, "")
        empid = AtomOpsApp.getPreferences(Constants.EMP_ID, "")
        tripid = intent.getStringExtra("tripid").toString()

        // Create the adapter up front so refreshList() is always safe to call
        adapter = DocketAdapter()
        binding.rvDockets.adapter = adapter

        setupList()
        setupListeners()
        updateProgress()
    }

    private fun setupList() {
        lifecycleScope.launch {
            try {
                ShowDailog()
                allDockets.clear()
                val resp = api.getDocumentData(
                    "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                    tripid,
                    AtomOpsApp.getPreferences(Constants.COMP_CODE, "")
                )
                if (resp.code() == 200) {
                    DismissDialog()
                    val manifestList = resp.body()?.data?.manifests
                    if (manifestList != null) {
                        for (i in manifestList) {
                            for (j in i.dockets) {
                                allDockets.add(
                                    Docket(j.docketNo, j.documentStatus.equals("AVAILABLE"))
                                )
                            }
                        }
                    }
                    refreshList()
                    updateProgress()   // FIX: update progress once the data has loaded
                } else {
                    DismissDialog()
                   // Utils.geterrormessagewithcode(this@DocumentScan, resp.code(), resp.message())
                    Utils.handleHttpError(this@DocumentScan, resp.code())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@DocumentScan, e)
            }
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        // Scan box: typing / scanning a docket number marks it as scanned
        binding.etscanSearch.doAfterTextChanged { text ->
            val input = text?.toString()?.trim().orEmpty()
            if (input.isEmpty()) return@doAfterTextChanged

            val index = allDockets.indexOfFirst { it.docketNo.equals(input, ignoreCase = true) }
            if (index == -1) return@doAfterTextChanged   // not a full match yet, keep waiting

            if (!allDockets[index].isScanned) {
                allDockets[index] = allDockets[index].copy(isScanned = true)
                refreshList()      // FIX: show the tick in the list
                updateProgress()   // FIX: progress bar, count and percent
                callAPiforScan(input)
                Toast.makeText(this, "$input scanned", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "$input already scanned", Toast.LENGTH_SHORT).show()
            }

            // FIX: clear the scan box (was clearing etSearch before)
            binding.etscanSearch.text?.clear()
        }

        // List search box: only filters the list
        binding.etSearch.doAfterTextChanged { refreshList() }

        binding.btnFilter.setOnClickListener {
            // TODO: show filter bottom sheet (All / Scanned / Pending)
        }
        binding.btnScan.setOnClickListener {
            // TODO: launch barcode scanner and call markScanned(result)
            Toast.makeText(this, "Open scanner", Toast.LENGTH_SHORT).show()
        }
        binding.btnSubmit.setOnClickListener {
            callSubmitApi()
        }
    }

    private fun callSubmitApi() {
        val body = SubmitManifiest(
            actor = empid,
            companyCode = compcode.toInt()
        )
        lifecycleScope.launch {
            try {
                val resp = api.submitManifest(
                    "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""), tripid, body
                )
                ShowDailog()
                if (resp.code() == 200) {
                    DismissDialog()
                    Utils.showDialog(this@DocumentScan,"Success",resp.body()!!.message.toString(),R.drawable.green_check,{
                        finish()
                        startActivity(Intent(this@DocumentScan, LoadingUnloading::class.java))
                    })
                } else {
                    DismissDialog()
                    Utils.geterrormessagewithcode(this@DocumentScan, resp.code(), resp.message())
                }
            } catch (e: Exception) {
                DismissDialog()
                Utils.gettrycatcherror(this@DocumentScan, e)
            }
        }
    }

    private fun callAPiforScan(docketNo: String) {
        val mod = ScanDocMod(empid, compcode.toInt(), docketNo)
        lifecycleScope.launch {
            try {
                val response = api.scandocument(
                    "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""), tripid, mod
                )
                if (response.code() != 200) {
                    revertScan(docketNo)   // server rejected it, so undo locally
                    Utils.geterrormessagewithcode(
                        this@DocumentScan, response.code(), response.message()
                    )
                }
            } catch (e: Exception) {
                revertScan(docketNo)
                Utils.gettrycatcherror(this@DocumentScan, e)
            }
        }
    }

    /** Undo a local scan if the API call failed, so progress matches the server. */
    private fun revertScan(docketNo: String) {
        val index = allDockets.indexOfFirst { it.docketNo.equals(docketNo, true) }
        if (index == -1) return
        allDockets[index] = allDockets[index].copy(isScanned = false)
        refreshList()
        updateProgress()
    }

    private fun markScanned(docketNo: String) {
        val index = allDockets.indexOfFirst { it.docketNo.equals(docketNo, true) }
        if (index == -1) {
            Toast.makeText(this, "Docket not found", Toast.LENGTH_SHORT).show()
            return
        }
        if (allDockets[index].isScanned) {
            Toast.makeText(this, "$docketNo already scanned", Toast.LENGTH_SHORT).show()
            return
        }
        allDockets[index] = allDockets[index].copy(isScanned = true)
        refreshList()
        updateProgress()
        callAPiforScan(docketNo)
    }

    private fun refreshList() {
        if (!::adapter.isInitialized) return
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) allDockets
        else allDockets.filter { it.docketNo.contains(query, ignoreCase = true) }
        adapter.submitList(filtered.toList())
    }

    private fun updateProgress() {
        val total = allDockets.size
        val done = allDockets.count { it.isScanned }
        val percent = if (total == 0) 0 else Math.round(done * 100f / total)
        binding.tvCount.text = String.format("%02d/%02d", done, total)
        binding.tvPercent.text = "$percent%"
        binding.progressBar.progress = percent
    }
}