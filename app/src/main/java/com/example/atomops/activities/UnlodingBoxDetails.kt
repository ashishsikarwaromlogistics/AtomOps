package com.example.atomops.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.UtilsK
import com.example.atomops.adapters.BoxAdapter
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.databinding.ActivityBoxDetailsBinding
import com.example.atomops.databinding.ActivityUnlodingBoxDetailsBinding
import com.example.atomops.dialogs.BoxIssueBottomSheet
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UnlodingBoxDetails : AppCompatActivity() {
    lateinit var binding: ActivityUnlodingBoxDetailsBinding
    private enum class Filter { ALL, SCANNED, UNSCANNED, SHORT }

   
    private lateinit var adapter: BoxAdapter

    private val allBoxes = mutableListOf<BoxItem>()
    private var currentFilter = Filter.ALL
    private lateinit var docketno: String
    private lateinit var barcodedao: BarcodeDao
    private var photoFile: File? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = photoFile
            if (success && file != null && file.exists()) {
                // ivPreview.visibility = View.VISIBLE
                //ivPreview.setImageURI(Uri.fromFile(file))
                uploadImage(file)
            } else {
                Toast.makeText(this, "Capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    // Permission result
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // "Don't ask again" selected
                showSettingsDialog()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    // 1) Check permission
    private fun checkPermissionAndOpenCamera() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) openCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 2) Create file + open camera
    private fun openCamera() {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        photoFile = file

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraLauncher.launch(uri)
    }
    /*private fun uploadImage(file: File) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                val compressed = compressImage(file)   // <= 1 MB

                val body = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", compressed.name, body)

                val response = RetrofitClient.api.uploadDocument(
                    auth = "Bearer $token",
                    templateCode = "PHOTOGRAPH",
                    isLinked = true,
                    documentName = "MyDoc",
                    file = part
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                    file.delete()          // clean up original
                    compressed.delete()    // clean up compressed
                } else {
                    Toast.makeText(this@MainActivity, "Error ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message ?: "Upload failed", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }*/

    // 3) API call
    private fun uploadImage(file: File) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                val compressed = UtilsK.compressImage(file)   // <= 1 MB

                val body = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", compressed.name, body)



                //   val body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                // val part = MultipartBody.Part.createFormData("file", file.name, body)

                val response = ApiClient.getRetrofitclient().create(ServiceInterface::class.java).uploadDocument(
                    auth = "Bearer "+ AtomOpsApp.getPreferences(Constants.JWTTOKEN,""),
                    templateCode = "PHOTOGRAPH",
                    isLinked = true,
                    documentName = "MyDoc",
                    file = part
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@UnlodingBoxDetails, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UnlodingBoxDetails, "Error ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UnlodingBoxDetails, e.message ?: "Upload failed", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        //  progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        //btnCamera.isEnabled = !loading
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoFile?.let { outState.putString("photo_path", it.absolutePath) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
       binding= DataBindingUtil. setContentView(this,R.layout.activity_unloding_box_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermissionAndOpenCamera()
        barcodedao = (application as AtomOpsApp).database.getBarcodeDao()
        docketno = intent.getStringExtra("docketno") ?: ""

        setupToolbar()
        setupRecycler()
        setupFilters()
        setupResultListener()
        loadBoxes()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
        // binding.ivSearch.setOnClickListener { /* open search */ }
    }

    private fun setupRecycler() {
        adapter = BoxAdapter { box -> onActionClick(box) }
        binding.rvBoxes.layoutManager = LinearLayoutManager(this)
        binding.rvBoxes.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipScanned -> UnlodingBoxDetails.Filter.SCANNED
                R.id.chipUnscanned -> UnlodingBoxDetails.Filter.UNSCANNED
                R.id.chipShort -> UnlodingBoxDetails.Filter.SHORT
                else -> UnlodingBoxDetails.Filter.ALL
            }
            applyFilter()
        }
    }

    private fun loadBoxes() {
        lifecycleScope.launch {
            val dbBoxes = barcodedao.getAllBox(docketno)

            allBoxes.clear()
            // If getAllBox() already returns List<BoxItem>, just use: allBoxes.addAll(dbBoxes)
            allBoxes.addAll(dbBoxes.map { i ->
                BoxItem(
                    docketno = i.docketno,
                    box = i.box,
                    timeScan = i.timeScan,
                    status = i.status,
                    voided = i.voided,
                    manifest = i.manifest,
                    isshort = i.isshort,
                    damage = i.damage,
                    damageurl = i.damageurl,
                    tripId = i.tripId,
                    isScan = i.isScan
                )
            })

            allBoxes.firstOrNull()?.let { binding.tvTripId.text = it.tripId.toString() }

            refresh()   // must run AFTER the data is loaded
        }
    }

    /** Scanned box -> Damage sheet, unscanned box -> Short sheet, already marked -> undo. */
    private fun onActionClick(box: BoxItem) {
        if (box.isScan) {
            if(box.damage.equals("DAMAGE")){
                updateStatus(box.box, BoxStatus.NONE,"DAMAGE")   // undo

            }
            else{
                val type = BoxStatus.DAMAGE
                BoxIssueBottomSheet.newInstance(box.box, type)
                    .show(supportFragmentManager, BoxIssueBottomSheet.TAG)
            }

            return
        }

        else if (box.isshort.equals("SHORT")) {
            updateStatus(box.box, BoxStatus.NONE,"SHORT")   // undo
            return
        }
        else{
            updateStatus(box.box, BoxStatus.SHORT,"")
        }


    }

    /** Receives the result when the user taps Confirm in the bottom sheet. */
    private fun setupResultListener() {
        supportFragmentManager.setFragmentResultListener(
            BoxIssueBottomSheet.REQUEST_KEY, this
        ) { _, bundle ->
            val boxId = bundle.getString(BoxIssueBottomSheet.KEY_BOX_ID).orEmpty()
            val type = bundle.getString(BoxIssueBottomSheet.KEY_TYPE).orEmpty()
            val reason = bundle.getString(BoxIssueBottomSheet.KEY_REASON).orEmpty()
            val remark = bundle.getString(BoxIssueBottomSheet.KEY_REMARK).orEmpty()

            updateStatus(boxId, type,remark,reason)

            // TODO: save reason + remark (and damage photo url) to your database, e.g.
            // lifecycleScope.launch { barcodedao.updateStatus(docketno, boxId, type) }
        }
    }

    private fun updateStatus(boxId: String, newStatus: String,tagname:String) {
        val index = allBoxes.indexOfFirst { it.box == boxId }
        if (index != -1) {
            lifecycleScope.launch {

                if(tagname.equals("")||tagname.equals("SHORT")){
                    barcodedao.updateShortStatus(boxId,newStatus)
                    allBoxes[index] = allBoxes[index].copy(isshort = newStatus)}
                else{
                    barcodedao.updateDamageStatus(boxId,newStatus)
                    allBoxes[index] = allBoxes[index].copy(damage = newStatus)
                }
                refresh()
            }




        }
    }

    private fun updateStatus(boxId: String, newStatus: String, remark: String,reason:String) {
        val index = allBoxes.indexOfFirst { it.box == boxId }
        if (index != -1) {
            lifecycleScope.launch {
                barcodedao.updateDamageStatus(boxId,newStatus)
            }

            allBoxes[index] = allBoxes[index].copy(status = newStatus)

            refresh()
        }
    }

    private fun refresh() {
        updateSummary()
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            UnlodingBoxDetails.Filter.ALL -> allBoxes
            UnlodingBoxDetails.Filter.SCANNED -> allBoxes.filter { it.isScan }
            UnlodingBoxDetails.Filter.UNSCANNED -> allBoxes.filter { !it.isScan }
            UnlodingBoxDetails.Filter.SHORT -> allBoxes.filter { it.status == BoxStatus.SHORT }
        }
        adapter.submitList(filtered.toList())
    }

    private fun updateSummary() {
        val total = allBoxes.size
        val scanned = allBoxes.count { it.isScan }

        binding.tvScannedCount.text = scanned.toString()
        binding.tvTotalCount.text = "/$total"
        binding.progressLoading.max = total
        binding.progressLoading.progress = scanned

        binding.tvShortCount.text = allBoxes.count { it.isshort == BoxStatus.SHORT }.toString()
        binding.tvExcessCount.text = allBoxes.count { it.status == BoxStatus.EXCESS }.toString()
        binding.tvDamageCount.text = allBoxes.count { it.damage == BoxStatus.DAMAGE }.toString()
    }
}
