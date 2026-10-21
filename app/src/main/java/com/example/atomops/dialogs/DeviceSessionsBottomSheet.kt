package com.example.atomops.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.example.atomops.R
import com.example.atomops.adapters.DeviceSessionAdapter
import com.example.atomops.databinding.DialogDeviceSessionsBinding
import com.example.atomops.models.DeviceSession
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DeviceSessionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogDeviceSessionsBinding? = null
    private val binding get() = _binding!!

    // Hook these up to your ViewModel / repository
    var onLogoutSession: ((DeviceSession) -> Unit)? = null
    var onEndAllSessions: (() -> Unit)? = null
    var onSave: (() -> Unit)? = null

    private val adapter = DeviceSessionAdapter { session -> confirmLogout(session) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeviceSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSessions.adapter = adapter
        adapter.submitList(sampleSessions()) // replace with real data

        binding.btnEndAll.setOnClickListener { confirmEndAll() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener {
            onSave?.invoke()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<FrameLayout>(
                R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            // let our rounded drawable show instead of the default sheet background
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
        return dialog
    }

    private fun confirmLogout(session: DeviceSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out of this device?")
            .setMessage("${session.deviceName} • ${session.location}")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                onLogoutSession?.invoke(session)
                adapter.submitList(adapter.currentList.filterNot { it.id == session.id })
            }
            .show()
    }

    private fun confirmEndAll() {
        AlertDialog.Builder(requireContext())
            .setTitle("End all sessions?")
            .setMessage("You will be logged out from all devices.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("End All") { _, _ ->
                onEndAllSessions?.invoke()
                adapter.submitList(emptyList())
            }
            .show()
    }

    private fun sampleSessions() = listOf(
        DeviceSession("1", "Chrome on Windows", "v124.0.0.0", "Bangalore, India", true),
        DeviceSession("2", "Safari on iPhone", "v124.0.0.0", "Bangalore, India", false, "2 days ago")
    )

    override fun onDestroyView() {
        binding.rvSessions.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DeviceSessionsBottomSheet"

        fun show(fm: FragmentManager) = DeviceSessionsBottomSheet().show(fm, TAG)
    }
}

/*
 Usage (e.g. in ProfileFragment, on "Device Sessions" card click):

 binding.cardDeviceSessions.setOnClickListener {
     DeviceSessionsBottomSheet.show(childFragmentManager)
 }

 build.gradle (app):
   android { buildFeatures { viewBinding = true } }
   implementation("com.google.android.material:material:1.12.0")
   implementation("androidx.recyclerview:recyclerview:1.3.2")
*/
