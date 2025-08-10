package com.example.runflow.ui.run

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.runflow.R
import com.example.runflow.databinding.FragmentRunBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

class RunFragment : Fragment() {

    private var _binding: FragmentRunBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var totalSeconds = 0
    private var goalReached = false

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private val pathPoints = mutableListOf<LatLng>()
    private var totalDistanceMeters = 0.0

    private val args: RunFragmentArgs by navArgs()

    // ריצה של הטיימר כל שנייה
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            totalSeconds++
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            binding.runLBLTimer.text = String.format("%02d:%02d", minutes, seconds)

            // יעד זמן (בדקות)
            if (!goalReached && args.targetType == "time") {
                val targetSeconds = (args.targetValue * 60f).toInt()
                if (totalSeconds >= targetSeconds) {
                    finishRunAndNavigate()
                    return
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        startLocationUpdates()
        handler.post(updateTimerRunnable)

        binding.runBTNFinish.setOnClickListener {
            finishRunAndNavigate()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val newLocation = result.lastLocation ?: return
                val newLatLng = LatLng(newLocation.latitude, newLocation.longitude)

                if (pathPoints.isNotEmpty()) {
                    val prevLatLng = pathPoints.last()
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        prevLatLng.latitude, prevLatLng.longitude,
                        newLatLng.latitude, newLatLng.longitude,
                        distance
                    )
                    totalDistanceMeters += distance[0]
                }
                pathPoints.add(newLatLng)
                updateDistanceUI()
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    private fun updateDistanceUI() {
        val distanceKm = totalDistanceMeters / 1000.0
        val rounded = (distanceKm * 100).roundToInt() / 100.0
        binding.runLBLDistance.text = "$rounded km"

        // Pace
        if (distanceKm >= 0.1) {
            val paceSecondsPerKm = totalSeconds / distanceKm
            val paceMin = (paceSecondsPerKm / 60).toInt()
            val paceSec = (paceSecondsPerKm % 60).toInt()
            val paceText = String.format("%d:%02d min/km", paceMin, paceSec)
            binding.runLBLPace.text = paceText
        } else {
            binding.runLBLPace.text = "0:00 min/km"
        }

        // יעד מרחק (בק״מ)
        if (!goalReached && args.targetType == "distance") {
            if (distanceKm >= args.targetValue.toDouble()) {
                finishRunAndNavigate()
            }
        }
    }

    private fun finishRunAndNavigate() {
        if (goalReached) return
        goalReached = true

        stopLocationUpdates()
        handler.removeCallbacks(updateTimerRunnable)

        val durationMillis = totalSeconds * 1000L
        val distanceKm = totalDistanceMeters.toFloat() / 1000f
        val pathArray = pathPoints.toTypedArray()

        val bundle = RunSummaryFragmentArgs(
            durationMillis = durationMillis,
            distanceKm = distanceKm,
            pathPoints = pathArray,
            fromPlan = args.fromPlan,
            planId = args.planId,
            week = args.week,
            index = args.index
        ).toBundle()

        findNavController().navigate(
            R.id.action_runFragment_to_runSummaryFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimerRunnable)
        stopLocationUpdates()
        _binding = null
    }
}
