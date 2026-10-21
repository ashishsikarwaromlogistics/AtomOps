package com.example.atomops.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.atomops.ui.screens.AllTripsScreen

class AllTripsAct : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    AllTripsScreen(
                        onBackClick = { finish() },
                        onTripClick = { trip ->
                            // TODO: navigate to Trip Detail screen, e.g.:
                            // startActivity(Intent(this, TripDetailAct::class.java).putExtra("tripId", trip.tripId))
                        }
                    )
                }
            }
        }
    }
}
