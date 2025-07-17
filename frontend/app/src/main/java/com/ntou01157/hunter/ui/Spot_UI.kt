package com.ntou01157.hunter.ui

import android.annotation.SuppressLint
import com.ntou01157.hunter.models.*
import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import androidx.navigation.NavHostController
import com.ntou01157.hunter.models.Spot

@SuppressLint("UnrememberedMutableState")
@Composable
fun SpotMarker(spot: Spot, navController: NavHostController) {

    Marker(
        state = MarkerState(position = LatLng(spot.latitude, spot.longitude)),
        title = spot.spotName,
        snippet = spot.spotId,
        onClick = {
            navController.navigate("mission/${spot.spotId}")
            true
        }
    )
}