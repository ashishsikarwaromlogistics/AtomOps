package com.example.atomops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atomops.models.Trip
import com.example.atomops.ui.components.TripCard
import com.example.atomops.viewmodels.AllTripsViewModel
import com.example.atomops.viewmodels.TripTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTripsScreen(
    onBackClick: () -> Unit = {},
    onTripClick: (Trip) -> Unit = {},
    viewModel: AllTripsViewModel = viewModel()
) {
    val filteredTrips = viewModel.filteredTrips

    Scaffold(
        containerColor = Color(0xFFF5F6FA),
        topBar = {
            AllTripsTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SummaryCard(
                total = viewModel.totalCount,
                loading = viewModel.loadingCount,
                unloading = viewModel.unloadingCount,
                completed = viewModel.completedCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
            )

            SearchAndFilterRow(
                query = viewModel.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TripTabsRow(
                selectedTab = viewModel.selectedTab,
                onTabSelected = viewModel::onTabSelected
            )

            if (filteredTrips.isEmpty()) {
                EmptyTripsState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(filteredTrips, key = { it.tripId }) { trip ->
                        TripCard(
                            trip = trip,
                            onClick = { onTripClick(trip) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllTripsTopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "All Trips",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: focus search */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
            // Profile avatar
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF123B54), Color(0xFF1E8FA6)))
        )
    )
}

@Composable
private fun SummaryCard(
    total: Int,
    loading: Int,
    unloading: Int,
    completed: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStat(
                label = "Total Trips",
                value = total,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(Color(0xFFE7E9EF))
            )

            Row(
                modifier = Modifier
                    .weight(2f)
                    .background(Color(0xFFFCEFE4)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat(label = "Loading", value = loading, dotColor = Color(0xFF8B5CF6))
                SummaryStat(label = "Unloading", value = unloading, dotColor = Color(0xFFF59E0B))
                SummaryStat(label = "Completed", value = completed, dotColor = Color(0xFF22C55E))
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    dotColor: Color? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            dotColor?.let {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(it)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = label, fontSize = 12.sp, color = Color(0xFF8A8F99))
        }
        Text(
            text = value.toString().padStart(2, '0'),
            fontSize = if (dotColor == null) 20.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B1F27)
        )
    }
}

@Composable
private fun SearchAndFilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            placeholder = { Text("Search By 'Trip ID'", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8A8F99)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedIndicatorColor = Color(0xFFE5E7EC),
                focusedIndicatorColor = Color(0xFF2563EB)
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EC))
        ) {
            IconButton(onClick = { /* TODO: open advanced filters */ }) {
                Icon(Icons.Filled.Tune, contentDescription = "Filter", tint = Color(0xFF1B1F27))
            }
        }
    }
}

@Composable
private fun TripTabsRow(
    selectedTab: TripTab,
    onTabSelected: (TripTab) -> Unit
) {
    val tabs = TripTab.entries
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        containerColor = Color(0xFFF5F6FA),
        contentColor = Color(0xFF2563EB),
        edgePadding = 16.dp
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        color = if (selectedTab == tab) Color(0xFF2563EB) else Color(0xFF8A8F99)
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No trips found",
            fontSize = 14.sp,
            color = Color(0xFF8A8F99)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllTripsScreenPreview() {
    AllTripsScreen()
}
