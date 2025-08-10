package com.example.runflow.ui.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.example.runflow.R
import com.example.runflow.databinding.FragmentPlanDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlanDetailsFragment : Fragment() {

    private var _binding: FragmentPlanDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: PlanDetailsFragmentArgs by navArgs()

    companion object { private const val TAG = "RUNFLOW-PLAN" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // חץ חזרה
        binding.planDetailsToolbar.setNavigationIcon(R.drawable.baseline_arrow_circle_left_24)
        binding.planDetailsToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // לרווח מהסטטוס־בר
        ViewCompat.setOnApplyWindowInsetsListener(binding.planDetailsToolbar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top)
            insets
        }

        // כותרות/תוכן
        binding.planDetailsLBLTitle.text = args.title
        binding.planDetailsLBLWeeks.text = args.weeks.joinToString("\n\n")

        // התחלת תוכנית
        binding.btnStartProgram.setOnClickListener {
            startProgramInFirestore(args.title, args.weeks.toList())
        }
    }

    private fun startProgramInFirestore(title: String, weeksText: List<String>) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            return
        }
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()
        val currentRef = db.collection("users").document(uid)
            .collection("active_plan").document("current")

        binding.btnStartProgram.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ניקוי תוכנית קודמת (אם קיימת)
                val weeksSnap = currentRef.collection("weeks").get().await()
                for (weekDoc in weeksSnap.documents) {
                    val workoutsSnap = weekDoc.reference.collection("workouts").get().await()
                    for (w in workoutsSnap.documents) w.reference.delete().await()
                    weekDoc.reference.delete().await()
                }

                // כתיבת המסמך הראשי של התוכנית
                currentRef.set(
                    mapOf(
                        "title" to title,
                        "startDate" to Timestamp.now(),
                        "totalWeeks" to weeksText.size,
                        "currentWeek" to 1
                    )
                ).await()

                // כתיבת השבועות/האימונים
                weeksText.forEachIndexed { wIndex, rawWeek ->
                    val weekId = "week${wIndex + 1}"
                    val weekRef = currentRef.collection("weeks").document(weekId)

                    val titleLine = rawWeek.lineSequence().firstOrNull()?.trim().orEmpty()
                    val weekTitle = if (titleLine.isNotBlank()) titleLine else "Week ${wIndex + 1}"
                    weekRef.set(mapOf("index" to (wIndex + 1), "title" to weekTitle)).await()

                    val workoutLines = rawWeek.lineSequence()
                        .drop(1)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toList()

                    workoutLines.forEachIndexed { dIndex, line ->
                        val clean = line.replace(Regex("^\\d+\\.?\\s*"), "").trim()
                        weekRef.collection("workouts").document("day${dIndex + 1}")
                            .set(
                                mapOf(
                                    "dayIndex" to (dIndex + 1),
                                    "title" to clean,
                                    "completed" to false
                                )
                            )
                            .await()
                    }
                }

                if (isAdded) {
                    findNavController().navigate(
                        R.id.planProgressFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.planDetailsFragment) { inclusive = true }
                            launchSingleTop = true
                        }
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to start program", e)
                Toast.makeText(
                    requireContext(),
                    "שגיאה בהפעלת התוכנית: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnStartProgram.isEnabled = true
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
