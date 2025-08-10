package com.example.runflow.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runflow.R
import com.example.runflow.databinding.FragmentHomeBinding
import com.example.runflow.ui.run.RunFragmentArgs
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.home_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Run בלי יעד
        binding.homeBTNRun.setOnClickListener {
            val bundle = RunFragmentArgs(
                fromPlan = false,
                planId = "",
                week = 0,
                index = 0,
                targetType = "",
                targetValue = 0f
            ).toBundle()

            findNavController().navigate(
                R.id.action_navigation_home_to_runFragment,
                bundle
            )
        }

        // Set a goal
        binding.homeBTNSetGoal.setOnClickListener { showGoalPicker() }
    }

    private fun showGoalPicker() {
        val items = arrayOf("Distance (km)", "Time (min)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose a goal")
            .setItems(items) { dlg, which ->
                when (which) {
                    0 -> askForValueAndStart("distance", "Enter km (e.g. 3.0)")
                    1 -> askForValueAndStart("time", "Enter minutes (e.g. 30)")
                }
                dlg.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askForValueAndStart(targetType: String, hint: String) {
        val input = TextInputEditText(requireContext()).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val box = TextInputLayout(requireContext()).apply {
            setPadding(24, 8, 24, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (targetType == "distance") "Distance goal" else "Time goal")
            .setView(box)
            .setPositiveButton("Start") { _, _ ->
                val v = input.text?.toString()?.toFloatOrNull()
                if (v == null || v <= 0f) {
                    Snackbar.make(requireView(), "Invalid value", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                startRunWithGoal(targetType, v)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRunWithGoal(targetType: String, value: Float) {
        val bundle = RunFragmentArgs(
            fromPlan = false,
            planId = "",
            week = 0,
            index = 0,
            targetType = targetType,   // "distance" / "time"
            targetValue = value        // ק״מ או דקות
        ).toBundle()

        findNavController().navigate(
            R.id.action_navigation_home_to_runFragment,
            bundle
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        map.isMyLocationEnabled = true

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 0L
        )
            .setMinUpdateIntervalMillis(0L)
            .setMaxUpdates(1)
            .build()

        val fused = LocationServices.getFusedLocationProviderClient(requireContext())
        fused.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            },
            Looper.getMainLooper()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
