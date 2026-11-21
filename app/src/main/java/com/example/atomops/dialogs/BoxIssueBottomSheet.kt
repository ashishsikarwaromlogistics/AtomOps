package com.example.atomops.dialogs

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.atomops.AtomOpsApp
import com.example.atomops.Constants
import com.example.atomops.R
import com.example.atomops.UtilsK
import com.example.atomops.activities.BoxStatus
import com.example.atomops.databinding.BottomSheetBoxIssueBinding
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class BoxIssueBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBoxIssueBinding? = null
    private val binding get() = _binding!!

    private val boxId by lazy { requireArguments().getString(ARG_BOX_ID).orEmpty() }
    // BoxStatus.SHORT or BoxStatus.DAMAGE
    private val type by lazy { requireArguments().getString(ARG_TYPE) ?: BoxStatus.SHORT }
    private val isDamage get() = type == BoxStatus.DAMAGE

    private val chips: List<Chip>
        get() = with(binding) {
            listOf(chipCrushed, chipTorn, chipWet, chipOpenSeal, chipPunctured, chipBrokenSeals)
        }

    // ---- damage photo state (kept across rotation) ----
    private var photoFile: File? = null        // file the camera writes into
    private var photoDocId: String? = null     // document id returned by uploadDocument
    private var isUploading = false

    // ---- camera + permission (registered as properties, before the fragment is created) ----
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = photoFile
            if (success && file != null && file.exists()) {
                uploadImage(file)
            } else {
                Toast.makeText(requireContext(), "Capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> openCamera()
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showSettingsDialog()
                else -> Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).also {
            it.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBoxIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            photoFile = it.getString(STATE_PHOTO_PATH)?.let(::File)
            photoDocId = it.getString(STATE_PHOTO_ID)
        }

        val label = if (isDamage) "Damage" else "Short"
        binding.tvTitle.text = "Mark Box as $label"
        binding.tvMessage.text = "The box will be reported as ${label.lowercase()}"
        binding.tvBoxId.text = "Box Id: $boxId"

        // Photo only for damage
        binding.llPhotoSection.visibility = if (isDamage) View.VISIBLE else View.GONE
        binding.btnUploadPhoto.setOnClickListener { if (!isUploading) checkPermissionAndOpenCamera() }
        binding.llPhoto.setOnClickListener { if (!isUploading) checkPermissionAndOpenCamera() }
        updatePhotoUi()

        setupChips()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener { onConfirm() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoFile?.let { outState.putString(STATE_PHOTO_PATH, it.absolutePath) }
        photoDocId?.let { outState.putString(STATE_PHOTO_ID, it) }
    }

    // ------------------------------------------------------------------
    // Reason chips
    // ------------------------------------------------------------------
    /**
     * Tap a chip  -> it gets selected (background + border change), others are cleared.
     * Tap it again -> it gets unselected.
     */
    private fun setupChips() {
        chips.forEach { chip ->
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            applyChipStyle(chip, chip.isChecked)

            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                applyChipStyle(buttonView as Chip, isChecked)
                if (isChecked) {
                    // single selection: clear every other chip
                    chips.forEach { other ->
                        if (other !== buttonView && other.isChecked) other.isChecked = false
                    }
                }
            }
        }
    }

    private fun applyChipStyle(chip: Chip, selected: Boolean) {
        val ctx = requireContext()
        val bg = if (selected) R.color.chip_selected_bg else android.R.color.white
        val stroke = if (selected) R.color.teal_accent else R.color.chip_unselected_stroke
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
        chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, stroke))
        chip.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    // ------------------------------------------------------------------
    // Camera + upload
    // ------------------------------------------------------------------
    private fun checkPermissionAndOpenCamera() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) openCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val ctx = requireContext()
        val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "DMG_${boxId}_${System.currentTimeMillis()}.jpg")
        photoFile = file

        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    private fun uploadImage(file: File) {
        setUploading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val compressed = withContext(Dispatchers.IO) { UtilsK.compressImage(file) }   // <= 1 MB

                val body = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", compressed.name, body)

                val res = ApiClient.getRetrofitclient()
                    .create(ServiceInterface::class.java)
                    .uploadDocument(
                        auth = "Bearer " + AtomOpsApp.getPreferences(Constants.JWTTOKEN, ""),
                        templateCode = "PHOTOGRAPH",
                        isLinked = true,
                        documentName = "DamagePhoto_$boxId",
                        file = part
                    )

                if (res.isSuccessful) {
                    val id = res.body()?.data?.id
                    if (!id.isNullOrBlank()) {
                        photoDocId = id
                        Toast.makeText(requireContext(), "Photo uploaded", Toast.LENGTH_SHORT).show()
                    } else {
                        photoDocId = null
                        Utils.showDialog(requireContext(), "error", "Photo upload failed, please try again", R.drawable.ic_baseline_error_24)
                    }
                } else {
                    try{
                        Utils.showDialog(requireContext(), "error", errorText(res), R.drawable.ic_baseline_error_24)
                    }catch (e:Exception){
                        Utils.gettrycatcherror(requireContext(),e)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.localizedMessage ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                setUploading(false)
            }
        }
    }

    /** Server "message" from the error body, else a readable text for the HTTP code. */
    private fun errorText(res: retrofit2.Response<*>): String =
        try {
            org.json.JSONObject(res.errorBody()?.string().orEmpty()).optString("message")
                .takeIf { it.isNotBlank() && it != "null" }
        } catch (e: Exception) { null } ?: Utils.getHttpErrorMessage(res.code(), "")

    private fun setUploading(uploading: Boolean) {
        isUploading = uploading
        _binding ?: return
        binding.btnConfirm.isEnabled = !uploading
        binding.btnUploadPhoto.isEnabled = !uploading
        updatePhotoUi()
    }

    private fun updatePhotoUi() {
        _binding ?: return
        binding.tvPhoto.text = when {
            isUploading        -> "Uploading photo..."
            photoDocId != null -> "Photo added ✓  (tap to retake)"
            else               -> getText(R.string.photo_required)      // "Photo*"
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera permission needed")
            .setMessage("Please enable camera permission from app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------------------------
    // Confirm
    // ------------------------------------------------------------------
    private fun onConfirm() {
        if (isUploading) {
            Toast.makeText(requireContext(), "Please wait, photo is uploading", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = chips.firstOrNull { it.isChecked }
        if (selected == null) {
            Toast.makeText(requireContext(), "Please select a reason", Toast.LENGTH_SHORT).show()
            return
        }
        // Damage -> camera photo is mandatory
        if (isDamage && photoDocId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please capture a damage photo", Toast.LENGTH_SHORT).show()
            binding.llPhoto.requestFocus()
            return
        }
        val result = Bundle().apply {
            putString(KEY_BOX_ID, boxId)                                  // box number
            putString(KEY_TYPE, type)                                     // SHORT or DAMAGE
            putString(KEY_REASON, selected.text.toString())               // reason chip
            putString(KEY_REMARK, binding.etRemark.text.toString().trim())// remark
            putString(KEY_PHOTO_ID, photoDocId.orEmpty())                 // uploaded photo id ("" for short)
        }
        parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BoxIssueBottomSheet"
        const val REQUEST_KEY = "box_issue_request"
        const val KEY_BOX_ID = "box_id"
        const val KEY_TYPE = "type"
        const val KEY_REASON = "reason"
        const val KEY_REMARK = "remark"
        const val KEY_PHOTO_ID = "photo_id"

        private const val ARG_BOX_ID = "arg_box_id"
        private const val ARG_TYPE = "arg_type"
        private const val STATE_PHOTO_PATH = "state_photo_path"
        private const val STATE_PHOTO_ID = "state_photo_id"

        fun newInstance(boxId: String, type: String) = BoxIssueBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_BOX_ID, boxId)
                putString(ARG_TYPE, type)
            }
        }
    }
}
