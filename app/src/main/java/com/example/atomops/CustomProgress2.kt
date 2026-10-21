package com.example.atomops

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog


class CustomProgress2( context: Context) {

    private val dialog: AlertDialog
    private val ivOuterRing: ImageView
    private val ivInnerRing: ImageView
    private val ivCenterCore: ImageView
    private val tvLoadingText: TextView
    private var colorAnimator: ValueAnimator? = null
    private var pulseAnimatorSet: AnimatorSet? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        ivOuterRing = view.findViewById(R.id.ivOuterRing)
        ivInnerRing = view.findViewById(R.id.ivInnerRing)
        tvLoadingText = view.findViewById(R.id.tvLoadingText)
        ivCenterCore = view.findViewById(R.id.ivCenterCore)
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        // Make dialog container transparent so rounded corners of custom layout show correctly
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun show() {
        dialog.show()
        startAnimations()
    }

    fun dismiss() {
        colorAnimator?.cancel()
        ivOuterRing.clearAnimation()
        ivInnerRing.clearAnimation()
        dialog.dismiss()
    }
    private fun startAnimations() {
        // 1. Clockwise Rotation (0 to 360)
        val clockwiseAnim = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            interpolator = LinearInterpolator()
            duration = 500
            repeatCount = Animation.INFINITE
        }

        // 2. Counter-Clockwise Rotation (360 to 0)
        val counterClockwiseAnim = RotateAnimation(
            360f, 0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            interpolator = LinearInterpolator()
            duration = 500 // Different duration makes the movement look dynamic
            repeatCount = Animation.INFINITE
        }

        // Apply animations to images
        ivOuterRing.startAnimation(clockwiseAnim)
        ivInnerRing.startAnimation(counterClockwiseAnim)



        val scaleX = ObjectAnimator.ofFloat(ivCenterCore, View.SCALE_X, 0.8f, 1.3f)
        val scaleY = ObjectAnimator.ofFloat(ivCenterCore, View.SCALE_Y, 0.8f, 1.3f)

        pulseAnimatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000 // Time taken to zoom in completely

            // This loop configuration makes it reverse smoothly back to original size
            scaleX.repeatCount = ValueAnimator.INFINITE
            scaleX.repeatMode = ValueAnimator.REVERSE
            scaleY.repeatCount = ValueAnimator.INFINITE
            scaleY.repeatMode = ValueAnimator.REVERSE

            start()
        }


        // 3. Text Color Changing Animation
        val colorPurple = Color.parseColor("#3F38B1")
        val colorPink = Color.parseColor("#FF4A70")

        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorPurple, colorPink, colorPurple).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                tvLoadingText.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
    }
}
