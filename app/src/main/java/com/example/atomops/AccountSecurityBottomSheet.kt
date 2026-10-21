package com.example.atomops

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.atomops.databinding.BottomSheetAccountSecurityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout


class AccountSecurityBottomSheet(
    private val onSave: (current: String, new: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAccountSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAccountSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val current = binding.etCurrent.text.toString()
            val new = binding.etNew.text.toString()
            val confirm = binding.etConfirm.text.toString()

            binding.tilCurrent.error = null
            binding.tilNew.error = null
            binding.tilConfirm.error = null

            when {
                current.isBlank() -> binding.tilCurrent.error = "Enter current password"
                new.length < 8 -> binding.tilNew.error = "Minimum 8 characters"
                new != confirm -> binding.tilConfirm.error = "Passwords do not match"
                else -> {
                    onSave(current, new)
                    dismiss()
                }
            }
        }
    }

    override fun getTheme() = R.style.AppBottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}