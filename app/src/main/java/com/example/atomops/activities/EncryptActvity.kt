package com.example.atomops.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.atomops.R
import com.example.atomops.utils.Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class EncryptActvity : AppCompatActivity() {
    private lateinit var etInput: EditText
    private lateinit var value: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnDecrypt: Button
    private lateinit var ivQRCode: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_encrypt_actvity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        etInput = findViewById(R.id.etInput)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnDecrypt = findViewById(R.id.btnDecrypt)
        value = findViewById(R.id.value)
        ivQRCode = findViewById(R.id.ivQRCode)

        btnDecrypt.setOnClickListener {
            var inputText = Utils.revertTransform(etInput.text.toString().trim())
            value.setText(inputText)
        }

        btnGenerate.setOnClickListener {

            var inputText = Utils.transform(etInput.text.toString().trim())


            if (inputText.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter some text",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            try {
                val qrBitmap = generateQRCode(inputText)

                ivQRCode.setImageBitmap(qrBitmap)

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "QR generation failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun generateQRCode(text: String): Bitmap {

        val size = 512

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until size) {
            for (y in 0 until size) {

                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                )
            }
        }

        return bitmap
    }
}