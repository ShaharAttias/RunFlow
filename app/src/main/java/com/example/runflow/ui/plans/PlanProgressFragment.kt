package com.example.runflow.ui.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runflow.R
import com.example.runflow.databinding.FragmentPlanProgressBinding
import com.example.runflow.model.Workout
import com.example.runflow.ui.common.VerticalSpaceDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * מסך הצגת התקדמות בתכנית ריצה פעילה.
 * מציג את השבוע הנוכחי ברשימת אימונים ומאפשר לסמן אימונים שהושלמו,
 * לעבור לשבוע הבא או לסיים את התכנית.
 */
class PlanProgressFragment : Fragment() {

    private var _binding: FragmentPlanProgressBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var totalWeeks = 0
    private var currentWeek = 1
    private var currentWeekId = "week1"

    private lateinit var workoutsAdapter: WorkoutsAdapter

    // מאזינים ל-Firestore (ננתק כשעוזבים את המסך)
    private var currentListener: ListenerRegistration? = null
    private var workoutsListener: ListenerRegistration? = null

    private var isFinishingPlan = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // כפתור חזרה
        binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_circle_left_24)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        // התאמת Insets (סטטוס בר / נביגיישן בר)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActions) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = bottom.coerceAtLeast(v.paddingBottom))
            insets
        }

        // הגדרת RecyclerView
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(VerticalSpaceDecoration(8))
        workoutsAdapter = WorkoutsAdapter(
            onMarkCompleted = { workout ->
                if (!workout.completed) markWorkoutCompleted(currentWeekId, workout.id)
            }
        )
        binding.recycler.adapter = workoutsAdapter

        // כפתורים
        binding.btnFinishPlan.setOnClickListener { finishPlan() }
        binding.btnNextWeek.setOnClickListener { goToNextWeek() }

        // טעינת התכנית הפעילה
        loadActivePlan()
    }

    /** מאזין לטעינת התכנית הפעילה מה-Firestore */
    private fun loadActivePlan() {
        currentListener?.remove()
        currentListener = db.collection("users").document(uid)
            .collection("active_plan").document("current")
            .addSnapshotListener(requireActivity()) { snap: DocumentSnapshot?, err: FirebaseFirestoreException? ->
                if (!isAdded || _binding == null || isFinishingPlan) return@addSnapshotListener
                if (err != null) {
                    Snackbar.make(binding.root, "שגיאה בטעינת התוכנית", Snackbar.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val doc = snap ?: return@addSnapshotListener
                if (!doc.exists()) {
                    safeNavigateToPlans()
                    return@addSnapshotListener
                }

                totalWeeks = (doc.getLong("totalWeeks") ?: 0).toInt()
                currentWeek = (doc.getLong("currentWeek") ?: 1).toInt().coerceAtLeast(1)
                currentWeekId = "week$currentWeek"

                binding.toolbar.title = getString(R.string.plan_weeks_title)
                binding.title.text = "Week $currentWeek:"

                binding.btnNextWeek.visibility =
                    if (currentWeek >= totalWeeks) View.GONE else View.VISIBLE

                loadWorkoutsOf(currentWeekId)
            }
    }

    /** מאזין לרשימת אימונים של השבוע הנוכחי */
    private fun loadWorkoutsOf(weekId: String) {
        workoutsListener?.remove()
        workoutsListener = db.collection("users").document(uid)
            .collection("active_plan").document("current")
            .collection("weeks").document(weekId)
            .collection("workouts")
            .orderBy("dayIndex")
            .addSnapshotListener(requireActivity()) { snap: QuerySnapshot?, err: FirebaseFirestoreException? ->
                if (!isAdded || _binding == null || isFinishingPlan) return@addSnapshotListener
                if (err != null) {
                    Snackbar.make(binding.root, "שגיאה בטעינת אימונים", Snackbar.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val items = snap?.documents?.map { doc: DocumentSnapshot ->
                    com.example.runflow.model.Workout(
                        id = doc.id,
                        dayIndex = doc.getLong("dayIndex")?.toInt() ?: 0,
                        title = doc.getString("title") ?: "Workout",
                        completed = doc.getBoolean("completed") ?: false
                    )
                }?.filter { w ->
                    val t = w.title.trim()
                    t.isNotEmpty() && !t.equals("rest", true) && !t.contains("rest day", true)
                }.orEmpty()

                workoutsAdapter.submit(items)
            }
    }

    /** ניווט בטוח חזרה למסך התכניות */
    private fun safeNavigateToPlans() {
        if (!isAdded) return
        findNavController().navigate(
            R.id.navigation_dashboard,
            null,
            navOptions {
                popUpTo(R.id.planProgressFragment) { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        )
    }

    /** מעבר לשבוע הבא */
    private fun goToNextWeek() {
        if (currentWeek >= totalWeeks) return
        val newWeek = currentWeek + 1
        db.collection("users").document(uid)
            .collection("active_plan").document("current")
            .update("currentWeek", newWeek)
            .addOnFailureListener {
                if (_binding != null) {
                    Snackbar.make(binding.root, "שגיאה במעבר לשבוע הבא", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    /** סיום התכנית (מחיקת כל האימונים והשבועות מה-Firestore) */
    private fun finishPlan() {
        if (isFinishingPlan) return
        isFinishingPlan = true

        // נטרול כפתורים
        binding.btnFinishPlan.isEnabled = false
        binding.btnNextWeek.isEnabled = false

        // ניתוק מאזינים
        currentListener?.remove(); currentListener = null
        workoutsListener?.remove(); workoutsListener = null

        viewLifecycleOwner.lifecycleScope.launch {
            val currentRef = db.collection("users").document(uid)
                .collection("active_plan").document("current")
            try {
                val weeks = currentRef.collection("weeks").get().await()
                for (w in weeks.documents) {
                    val workouts = w.reference.collection("workouts").get().await()
                    for (wo in workouts.documents) {
                        wo.reference.delete().await()
                    }
                    w.reference.delete().await()
                }
                currentRef.delete().await()

                if (isAdded) {
                    delay(100)
                    safeNavigateToPlans()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Snackbar.make(binding.root, "שגיאה בסיום התוכנית", Snackbar.LENGTH_SHORT).show()
                }
                isFinishingPlan = false
            } finally {
                if (_binding != null) {
                    binding.btnFinishPlan.isEnabled = true
                    binding.btnNextWeek.isEnabled = true
                }
            }
        }
    }

    /** סימון אימון כהושלם */
    private fun markWorkoutCompleted(weekId: String, workoutId: String) {
        db.collection("users").document(uid)
            .collection("active_plan").document("current")
            .collection("weeks").document(weekId)
            .collection("workouts").document(workoutId)
            .update("completed", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentListener?.remove()
        workoutsListener?.remove()
        currentListener = null
        workoutsListener = null
        _binding = null
    }
}
