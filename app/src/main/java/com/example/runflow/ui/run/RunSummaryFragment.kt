package com.example.runflow.ui.run

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.runflow.R
import com.example.runflow.databinding.FragmentRunSummaryBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class RunSummaryFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRunSummaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap

    private val args: RunSummaryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // זמן
        val minutes = TimeUnit.MILLISECONDS.toMinutes(args.durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(args.durationMillis) % 60
        binding.summaryLBLTime.text = String.format("Time: %02d:%02d", minutes, seconds)

        // מרחק
        binding.summaryLBLDistance.text = "Distance: %.2f km".format(args.distanceKm)

        // קצב
        val paceSecondsPerKm = if (args.distanceKm > 0) args.durationMillis / 1000f / args.distanceKm else 0f
        val paceMin = paceSecondsPerKm.toInt() / 60
        val paceSec = paceSecondsPerKm.toInt() % 60
        val paceString = String.format("%d:%02d min/km", paceMin, paceSec)
        binding.summaryLBLPace.text = "Pace: $paceString"

        // המרה ל-GeoPoint
        val geoPoints = args.pathPoints.map { com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude) }

        // שמירה ל־Firestore תחת המשתמש
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        val runMap = hashMapOf(
            "uid" to uid,
            "startedAt" to Timestamp.now(),
            "durationMillis" to args.durationMillis,
            "distanceKm" to args.distanceKm.toDouble(),
            "pace" to paceString,
            "pathPoints" to geoPoints
        )

        db.collection("users").document(uid ?: "unknown")
            .collection("runs")
            .add(runMap)
            .addOnSuccessListener { doc ->
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving run", e)
            }

        // מפה
        val mapFragment = childFragmentManager.findFragmentById(R.id.summary_MAP) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // כפתור בית
        binding.summaryBTNHome.setOnClickListener {
            findNavController().navigate(R.id.navigation_home)
        }

        // חזרה לריצה
        binding.summaryBTNResume.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // מאתר שבוע לפי index, ואז אימון לפי dayIndex → completed=true
    private fun markWorkoutCompletedByIndex(db: FirebaseFirestore, uid: String, weekIndex: Int, dayIndex: Int) {
        val weeksRef = db.collection("users").document(uid)
            .collection("active_plan").document("current")
            .collection("weeks")

        weeksRef.whereEqualTo("index", weekIndex).limit(1).get()
            .addOnSuccessListener { weekSnap ->
                val weekDoc = weekSnap.documents.firstOrNull() ?: return@addOnSuccessListener
                val weekId = weekDoc.id
                weeksRef.document(weekId).collection("workouts")
                    .whereEqualTo("dayIndex", dayIndex).limit(1).get()
                    .addOnSuccessListener { woSnap ->
                        val woDoc = woSnap.documents.firstOrNull() ?: return@addOnSuccessListener
                        weeksRef.document(weekId).collection("workouts").document(woDoc.id)
                            .update("completed", true)
                    }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (args.pathPoints.isNotEmpty()) {
            val poly = PolylineOptions()
                .addAll(args.pathPoints.toList())
                .color(android.graphics.Color.BLUE)
                .width(10f)
            map.addPolyline(poly)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(args.pathPoints.first(), 16f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
