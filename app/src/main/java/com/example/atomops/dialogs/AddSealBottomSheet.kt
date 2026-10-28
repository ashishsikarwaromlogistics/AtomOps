package com.example.atomops.dialogs

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.atomops.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddSealBottomSheet(
    private val onSubmit: (sealNumber: String, photoUri: Uri) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var etSealNumber: EditText
    private lateinit var tvUploadLabel: TextView

    private var photoUri: Uri? = null

    // Gallery picker. Swap for TakePicture() if you want camera capture.
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoUri = it
                tvUploadLabel.text = it.lastPathSegment ?: "Photo selected"
            }
        }

    override fun getTheme() = R.style.BottomSheetTransparentTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_add_seal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSealNumber = view.findViewById(R.id.etSealNumber)
        tvUploadLabel = view.findViewById(R.id.tvUploadLabel)

        view.findViewById<View>(R.id.layoutUpload).setOnClickListener {
            pickImage.launch("image/*")
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dismiss() }

        view.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val seal = etSealNumber.text.toString().trim()
            val uri = photoUri
            when {
                seal.isEmpty() -> etSealNumber.error = "Enter seal number"
                uri == null -> Toast.makeText(
                    requireContext(), "Please upload seal photo", Toast.LENGTH_SHORT
                ).show()
                else -> {
                    onSubmit(seal, uri)
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val TAG = "AddSealBottomSheet"
    }
}