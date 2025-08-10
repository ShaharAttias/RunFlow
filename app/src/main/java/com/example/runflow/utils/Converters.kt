package com.example.runflow.utils

import com.example.runflow.model.MyLatLng
import com.google.android.gms.maps.model.LatLng

fun convertLatLngListToMyLatLngList(latLngList: List<LatLng>): List<MyLatLng> {
    return latLngList.map { latLng ->
        MyLatLng(latLng.latitude, latLng.longitude)
    }
}
