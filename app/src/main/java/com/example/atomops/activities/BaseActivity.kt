package com.example.atomops.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.atomops.R
import com.example.atomops.networking.NetworkMonitor
import kotlinx.coroutines.launch

/**
 * Extend this instead of AppCompatActivity.
 * It shows the "No Internet" screen on top of your layout automatically
 * and hides it when the internet comes back.
 */
open class BaseActivity : AppCompatActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private var noInternetView: View? = null
    private var wasOffline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(applicationContext)

        lifecycleScope.launch {
            // Only listens while the screen is visible
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { online ->
                    showNoInternet(!online)
                    if (online && wasOffline) onInternetBack()
                    wasOffline = !online
                }
            }
        }
    }

    /** Override in your Activity to reload data when internet returns. */
    open fun onInternetBack() {}

    private fun showNoInternet(show: Boolean) {
        if (noInternetView == null) {
            // Add the No Internet layout on top of the activity's own layout
            val root = findViewById<ViewGroup>(android.R.id.content)
            noInternetView = LayoutInflater.from(this)
                .inflate(R.layout.layout_no_internet, root, false)
            root.addView(noInternetView)

            noInternetView!!.findViewById<Button>(R.id.btnRetry).setOnClickListener {
                if (networkMonitor.isCurrentlyOnline()) {
                    showNoInternet(false)
                } else {
                    Toast.makeText(this, "Still offline", Toast.LENGTH_SHORT).show()
                }
            }
        }
        noInternetView!!.bringToFront()
        noInternetView!!.visibility = if (show) View.VISIBLE else View.GONE
    }
}