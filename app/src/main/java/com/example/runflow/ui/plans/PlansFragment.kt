package com.example.runflow.ui.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.runflow.R
import com.example.runflow.databinding.FragmentPlansBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PlansFragment : Fragment() {

    private var _binding: FragmentPlansBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlansViewModel
    private lateinit var adapter: PlanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // קודם בודקים אם יש תוכנית פעילה
        checkActivePlanOrShowCatalog()
    }

    private fun checkActivePlanOrShowCatalog() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // לא מחוברים? מציגים קטלוג
            setupCatalog()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .collection("active_plan").document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc.exists()) {
                    // יש תוכנית פעילה → לעמוד התוכנית (שבוע נוכחי)
                    findNavController().navigate(R.id.planProgressFragment)
                } else {
                    setupCatalog()
                }
            }
            .addOnFailureListener {
                setupCatalog()
            }
    }

    private fun setupCatalog() {
        if (!this::viewModel.isInitialized) {
            viewModel = ViewModelProvider(this).get(PlansViewModel::class.java)
        }

        if (!this::adapter.isInitialized) {
            adapter = PlanAdapter { selectedPlan ->
                val action = PlansFragmentDirections.actionPlansFragmentToPlanDetailsFragment(
                    title = selectedPlan.title,
                    description = selectedPlan.description,
                    difficulty = selectedPlan.difficulty,
                    imageRes = selectedPlan.imageRes,
                    weeks = selectedPlan.weeks.toTypedArray()
                )
                findNavController().navigate(action)
            }
            binding.plansRecyclerView.adapter = adapter
        }

        viewModel.plans.observe(viewLifecycleOwner) { plans ->
            adapter.submitList(plans)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
