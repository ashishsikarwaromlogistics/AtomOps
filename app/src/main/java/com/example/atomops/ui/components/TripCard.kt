package com.example.atomops.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atomops.models.Trip

@Composable
fun TripCard(
    trip: Trip,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onViewDetails: (Trip) -> Unit = {},
    onCancelTrip: (Trip) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header row: icon, id/date, status chip, overflow menu
            Row(verticalAlignment = Alignment.CenterVertically) {


                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.tripId,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1F27)
                    )
                    Text(
                        text = trip.dateTime,
                        fontSize = 12.sp,
                        color = Color(0xFF8A8F99)
                    )
                }

                StatusChip(trip = trip)

                Spacer(modifier = Modifier.width(4.dp))

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = Color(0xFF8A8F99)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                menuExpanded = false
                                onViewDetails(trip)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cancel Trip") },
                            onClick = {
                                menuExpanded = false
                                onCancelTrip(trip)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 4.dp))

            // Route
            Text(
                text = trip.routeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B1F27),
                modifier = Modifier.padding(top = 6.dp)
            )

            // Meta row: service / dock / vehicle number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                MetaItem(label = trip.serviceLabel)
                Spacer(modifier = Modifier.width(16.dp))
                MetaItem(label = trip.dock)
                Spacer(modifier = Modifier.width(16.dp))
                MetaItem(label = trip.vehicleNumber)
            }
        }
    }
}

@Composable
private fun RowScope.MetaItem(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        color = Color(0xFF8A8F99)
    )
}

@Composable
private fun StatusChip(trip: Trip) {
    Surface(

        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = trip.status.label,

            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
