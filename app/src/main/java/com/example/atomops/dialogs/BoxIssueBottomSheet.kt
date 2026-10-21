package com.example.atomops.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.atomops.R
import com.example.atomops.activities.BoxStatus
import com.example.atomops.databinding.BottomSheetBoxIssueBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class BoxIssueBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBoxIssueBinding? = null
    private val binding get() = _binding!!

    private val boxId by lazy { requireArguments().getString(ARG_BOX_ID).orEmpty() }
    // BoxStatus.SHORT or BoxStatus.DAMAGE
    private val type by lazy { requireArguments().getString(ARG_TYPE) ?: BoxStatus.SHORT }

    private val chips: List<Chip>
        get() = with(binding) {
            listOf(chipCrushed, chipTorn, chipWet, chipOpenSeal, chipPunctured, chipBrokenSeals)
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

        val label = if (type == BoxStatus.DAMAGE) "Damage" else "Short"
        binding.tvTitle.text = "Mark Box as $label"
        binding.tvMessage.text = "The box will be reported as ${label.lowercase()}"
        binding.tvBoxId.text = "Box Id: $boxId"

        setupChips()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener { onConfirm() }
    }

    /**
     * Tap a chip  -> it gets selected (background + border change), others are cleared.
     * Tap it again -> it gets unselected.
     */
    private fun setupChips() {
        chips.forEach { chip ->
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            applyChipStyle(chip, false)

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
    }

    private fun onConfirm() {
        val selected = chips.firstOrNull { it.isChecked }
        if (selected == null) {
            Toast.makeText(requireContext(), "Please select a reason", Toast.LENGTH_SHORT).show()
            return
        }
        val result = Bundle().apply {
            putString(KEY_BOX_ID, boxId)
            putString(KEY_TYPE, type)
            putString(KEY_REASON, selected.text.toString())
            putString(KEY_REMARK, binding.etRemark.text.toString().trim())
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

        private const val ARG_BOX_ID = "arg_box_id"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(boxId: String, type: String) = BoxIssueBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_BOX_ID, boxId)
                putString(ARG_TYPE, type)
            }
        }
    }
}
