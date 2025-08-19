package com.ntou01157.hunter.utils

import com.google.android.gms.maps.model.LatLng
import android.location.Location

object GeoVerifier {
    const val DEFAULT_THRESHOLD_METERS = 30f

    fun distanceMeters(a: LatLng, b: LatLng): Float {
        val r = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
        return r[0]
    }

    fun isWithinRange(userLatLng: LatLng?, spotLatLng: LatLng, thresholdMeters: Float = DEFAULT_THRESHOLD_METERS): Boolean {
        if (userLatLng == null) return false
        return distanceMeters(userLatLng, spotLatLng) <= thresholdMeters
    }
}
