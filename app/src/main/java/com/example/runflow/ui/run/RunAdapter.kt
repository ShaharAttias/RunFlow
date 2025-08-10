package com.example.runflow.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.runflow.databinding.ItemRunBinding
import com.example.runflow.model.Run
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions

class RunAdapter(
    private val onOpenRoute: (Run) -> Unit
) : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    private val runs = mutableListOf<Run>()

    inner class RunViewHolder(
        private val binding: ItemRunBinding
    ) : RecyclerView.ViewHolder(binding.root), OnMapReadyCallback {

        private var currentRun: Run? = null
        private var googleMap: GoogleMap? = null

        init {
            // הפעלת המפה
            binding.itemMapView.onCreate(null)
            binding.itemMapView.getMapAsync(this)

            // מאזין ללחיצה על האוברליי של המפה
            binding.itemMapClickOverlay.setOnClickListener {
                currentRun?.let { run -> onOpenRoute(run) }
            }
        }

        fun bind(run: Run) {
            currentRun = run

            val minutes = run.durationMillis / 1000 / 60
            val seconds = run.durationMillis / 1000 % 60

            binding.itemLBLTime.text = String.format("Time: %02d:%02d", minutes, seconds)
            binding.itemLBLDistance.text = "Distance: %.2f km".format(run.distanceKm)
            binding.itemLBLPace.text = "Pace: ${run.pace}"

            if (run.pathPoints.isNullOrEmpty()) {
                binding.itemMapView.visibility = View.GONE
                binding.itemLBLNoRoute.visibility = View.VISIBLE
            } else {
                binding.itemMapView.visibility = View.VISIBLE
                binding.itemLBLNoRoute.visibility = View.GONE
                binding.itemMapView.onResume()
                // אם המפה כבר מוכנה, נצייר מיד
                googleMap?.let { drawRoute(it, run) }
            }
        }

        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            map.uiSettings.isScrollGesturesEnabled = false
            map.uiSettings.isZoomGesturesEnabled = false
            currentRun?.let { run -> drawRoute(map, run) }
        }

        private fun drawRoute(map: GoogleMap, run: Run) {
            if (run.pathPoints.isNotEmpty()) {
                val polyline = PolylineOptions()
                val boundsBuilder = LatLngBounds.builder()

                for (point in run.pathPoints) {
                    val latLng = LatLng(point.latitude, point.longitude)
                    polyline.add(latLng)
                    boundsBuilder.include(latLng)
                }

                map.clear()
                map.addPolyline(polyline)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val binding = ItemRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        holder.bind(runs[position])
    }

    override fun getItemCount(): Int = runs.size

    fun setData(newRuns: List<Run>) {
        runs.clear()
        runs.addAll(newRuns)
        notifyDataSetChanged()
    }
}
