package com.example.runflow.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.runflow.model.Run
import com.google.firebase.firestore.FirebaseFirestore
import com.example.runflow.model.MyLatLng

class ProfileViewModel : ViewModel() {

    private val _runs = MutableLiveData<List<Run>>()
    val runs: LiveData<List<Run>> = _runs

    private val _totalDistance = MutableLiveData<Float>()
    val totalDistance: LiveData<Float> = _totalDistance

    init {
        fetchRuns()
    }

    private fun fetchRuns() {
        FirebaseFirestore.getInstance().collection("runs")
            .get()
            .addOnSuccessListener { result ->
                val runList = mutableListOf<Run>()
                var total = 0f

                for (doc in result) {
                    val duration = doc.getLong("durationMillis") ?: 0L
                    val distance = doc.getDouble("distanceKm")?.toFloat() ?: 0f
                    val pace = doc.getString("pace") ?: ""

                    // המרה בטוחה מרשימת HashMap ל־MyLatLng
                    val pointsRaw = doc.get("pathPoints") as? List<*>
                    val path = pointsRaw?.mapNotNull { point ->
                        try {
                            val pointMap = point as Map<*, *>
                            val lat = (pointMap["latitude"] as Number).toDouble()
                            val lng = (pointMap["longitude"] as Number).toDouble()
                            MyLatLng(lat, lng)
                        } catch (e: Exception) {
                            null // במקרה של שגיאה בהמרה
                        }
                    } ?: emptyList()

                    runList.add(Run(duration, distance, pace, path))
                    total += distance

                }

                _runs.value = runList
                _totalDistance.value = total
            }
    }
}
