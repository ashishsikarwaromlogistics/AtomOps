package com.example.atomops

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import com.example.atomops.databinding.DialogProgressBinding

/**
 * A modern, reusable progress dialog.
 *
 * Indeterminate:
 *     val progress = AppProgressDialog(this)
 *     progress.show("Loading", "Please wait...")
 *     progress.dismiss()
 *
 * Determinate (0..100):
 *     progress.show("Uploading", "Sending your file...", determinate = true)
 *     progress.setProgress(45)
 */
class AppProgressDialog(context: Context) {

    private val binding = DialogProgressBinding.inflate(LayoutInflater.from(context))

    private val dialog = Dialog(context, R.style.Theme_App_ProgressDialog).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val dm = context.resources.displayMetrics
            val width = (dm.widthPixels * 0.82f).toInt()
                .coerceAtMost((340 * dm.density).toInt())
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.45f)
            attributes.windowAnimations = R.style.ProgressDialogAnimation
        }
    }

    val isShowing: Boolean get() = dialog.isShowing

    fun show(
        title: String = "Loading",
        message: String? = "Please wait...",
        determinate: Boolean = false,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null
    ): AppProgressDialog {
        binding.tvTitle.text = title
        setMessage(message)
        configureMode(determinate)

        dialog.setCancelable(cancelable)
        binding.btnCancel.isVisible = cancelable
        binding.btnCancel.setOnClickListener {
            dismiss()
            onCancel?.invoke()
        }
        dialog.setOnCancelListener { onCancel?.invoke() }

        if (!dialog.isShowing) dialog.show()
        return this
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    fun setMessage(message: String?) {
        binding.tvMessage.isVisible = !message.isNullOrBlank()
        binding.tvMessage.text = message
    }

    /** Updates progress (0..100). Switches to determinate mode automatically. */
    fun setProgress(percent: Int) {
        val value = percent.coerceIn(0, 100)
        if (binding.circularProgress.isIndeterminate) configureMode(true)
        binding.circularProgress.setProgressCompat(value, true)
        binding.linearProgress.setProgressCompat(value, true)
        binding.tvPercent.text = "$value%"
    }

    fun dismiss() {
        if (dialog.isShowing) dialog.dismiss()
    }

    private fun configureMode(determinate: Boolean) {
        binding.circularProgress.isIndeterminate = !determinate
        binding.linearProgress.isVisible = determinate
        binding.tvPercent.isVisible = determinate
        if (determinate) {
            binding.circularProgress.setProgressCompat(0, false)
            binding.linearProgress.setProgressCompat(0, false)
            binding.tvPercent.text = "0%"
        }
    }
}
