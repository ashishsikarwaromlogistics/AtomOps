package com.example.atomops

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.atomops.activities.LoginAct
import com.example.atomops.networking.ApiClient
import com.example.atomops.networking.ServiceInterface
import com.example.atomops.utils.Utils
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

private val api: ServiceInterface by lazy {
    ApiClient.getRetrofitclient().create(ServiceInterface::class.java)
}

fun <T> AppCompatActivity.callApi(
    request: suspend (ServiceInterface) -> Response<T>,
    onUnauthorized: () -> Unit = { goToLogin() },
    onSuccess: (T?) -> Unit
) {
    lifecycleScope.launch {
        try {
            val response = request(api)
            when {
                response.isSuccessful -> onSuccess(response.body())

                response.code() == 401 || response.code() == 403 -> onUnauthorized()

                else -> showRetryDialog(
                    "Error(${response.code()})",
                    Utils.getHttpErrorMessage(response.code(), "")
                ) { callApi(request, onUnauthorized, onSuccess) }
            }
        } catch (e: CancellationException) {
            throw e // don't swallow coroutine cancellation
        } catch (e: Exception) {
            showRetryDialog("Error", Utils.getErrorMessage(e)) {
                callApi(request, onUnauthorized, onSuccess)
            }
        }
    }
}

fun AppCompatActivity.goToLogin() {
    startActivity(Intent(this, LoginAct::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
    finish()
}

fun AppCompatActivity.showRetryDialog(title: String, message: String, onRetry: () -> Unit) {
    if (isFinishing || isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton("Retry") { d, _ -> d.dismiss(); onRetry() }
        .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        .show()
}