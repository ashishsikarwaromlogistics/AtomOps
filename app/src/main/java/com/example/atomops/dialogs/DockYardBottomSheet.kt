package com.example.atomops.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import android.widget.Button
import com.example.atomops.R

class DockYardBottomSheet(
    private val onConfirm: (Option) -> Unit
) : BottomSheetDialogFragment() {

    enum class Option { GATE_OPERATIONS, DOCK_MANAGEMENT }

    private var selected = Option.GATE_OPERATIONS

    private lateinit var cardGate: MaterialCardView
    private lateinit var cardDock: MaterialCardView

    override fun getTheme() = R.style.BottomSheetTransparentTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_dock_yard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardGate = view.findViewById(R.id.cardGate)
        cardDock = view.findViewById(R.id.cardDock)

        updateSelection()

        cardGate.setOnClickListener {
            selected = Option.GATE_OPERATIONS
            updateSelection()
        }
        cardDock.setOnClickListener {
            selected = Option.DOCK_MANAGEMENT
            updateSelection()
        }

        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            onConfirm(selected)
            dismiss()
        }
    }

    private fun updateSelection() {
        applyState(cardGate, selected == Option.GATE_OPERATIONS)
        applyState(cardDock, selected == Option.DOCK_MANAGEMENT)
    }

    private fun applyState(card: MaterialCardView, isSelected: Boolean) {
        val ctx = requireContext()
        if (isSelected) {
            card.strokeColor = ContextCompat.getColor(ctx, R.color.teal_selected_stroke)
            card.strokeWidth = dp(2)
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.teal_selected_bg))
        } else {
            card.strokeColor = ContextCompat.getColor(ctx, R.color.card_unselected_stroke)
            card.strokeWidth = dp(1)
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "DockYardBottomSheet"
    }
}