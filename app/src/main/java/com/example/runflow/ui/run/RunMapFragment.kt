package com.example.runflow.ui.run

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runflow.R
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class RunMapFragment : Fragment(R.layout.fragment_run_map), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private var lats: List<Float> = emptyList()
    private var lngs: List<Float> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // קבלת נתונים מה־arguments
        arguments?.let {
            lats = it.getFloatArray("lats")?.toList() ?: emptyList()
            lngs = it.getFloatArray("lngs")?.toList() ?: emptyList()
        }

        // MapView
        mapView = view.findViewById(R.id.full_map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // חץ חזרה עם התאמת מרווח מה־status bar
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        ViewCompat.setOnApplyWindowInsetsListener(backButton) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset + (16 * resources.displayMetrics.density).toInt() // 16dp למטה
            }
            insets
        }
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (lats.isEmpty() || lngs.isEmpty() || lats.size != lngs.size) return

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.setAllGesturesEnabled(true)

        val points = ArrayList<LatLng>(lats.size)
        for (i in lats.indices) {
            points.add(LatLng(lats[i].toDouble(), lngs[i].toDouble()))
        }

        if (points.size >= 2) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(8f)
                    .color(Color.BLACK)
            )
        }

        map.addMarker(
            MarkerOptions()
                .position(points.first())
                .title("Start Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        if (points.size >= 2) {
            map.addMarker(
                MarkerOptions()
                    .position(points.last())
                    .title("Finish Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        map.setOnMapLoadedCallback {
            when {
                points.size >= 2 -> {
                    val b = LatLngBounds.Builder()
                    for (p in points) b.include(p)
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 80))
                }
                points.size == 1 -> {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(points[0], 16f))
                }
            }
        }
    }

    // מחזור חיים של MapView
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroyView() { googleMap = null; mapView.onDestroy(); super.onDestroyView() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
