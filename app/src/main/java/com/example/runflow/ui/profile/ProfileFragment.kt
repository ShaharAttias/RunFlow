package com.example.runflow.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.runflow.LoginActivity
import com.example.runflow.R
import com.example.runflow.databinding.FragmentProfileBinding
import com.example.runflow.model.Run
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
    val name: String = "",
    val age: Int = 0,
    val gender: String = ""
)

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var adapter: RunAdapter

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            Glide.with(this).load(uri).circleCrop().into(binding.profileImg)
        } else {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ----- רשימת ריצות -----
        adapter = RunAdapter(
            onOpenRoute = { run -> openRunOnMap(run) } // לוחצים על המפה בכרטיס → מסך מפה
        )
        binding.profileRunsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.profileRunsRecycler.adapter = adapter

        // סכום ק״מ
        viewModel.totalDistance.observe(viewLifecycleOwner) { total ->
            binding.profileTotalKm.text = String.format("%.2f", total)
        }

        // עדכון כמות ריצות + Personal Records
        viewModel.runs.observe(viewLifecycleOwner) { runList ->
            adapter.setData(runList)
            binding.profileRunsCount.text = runList.size.toString()

            // 1) שיא זמן
            val longestTimeMs = runList.maxOfOrNull { it.durationMillis } ?: 0L
            binding.profilePrTime.text =
                if (longestTimeMs > 0) formatDuration(longestTimeMs) else "--:--"

            // 2) שיא מרחק
            val longestDist = runList.maxOfOrNull { it.distanceKm.toDouble() } ?: 0.0
            binding.profilePrDistance.text = String.format("%.2f km", longestDist)

            // 3) קצב הכי מהיר (נמוך יותר = מהיר יותר)
            val fastestPaceMinPerKm = runList
                .asSequence()
                .filter { it.distanceKm > 0f && it.durationMillis > 0L }
                .map { (it.durationMillis / 60000.0) / it.distanceKm } // דקות לק״מ
                .minOrNull()

            binding.profilePrPace.text =
                fastestPaceMinPerKm?.let { formatPace(it) } ?: "--:-- min/km"
        }

        // פרופיל
        loadProfile()

        binding.profileChangePhoto.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        binding.profileBtnEdit.setOnClickListener { showEditDialog() }

        // ===== כפתור התנתקות =====
        binding.profileBtnLogout?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()

                    // מעבר למסך ההתחברות וניקוי היסטוריית המסכים
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .show()
        }
    }

    // ---------- ניווט למסך מפה מלא ----------
    private fun openRunOnMap(run: Run) {
        val pts = run.pathPoints
        if (pts.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No route available", Toast.LENGTH_SHORT).show()
            return
        }
        val lats = pts.map { it.latitude.toFloat() }.toFloatArray()
        val lngs = pts.map { it.longitude.toFloat() }.toFloatArray()
        findNavController().navigate(
            R.id.runMapFragment,
            bundleOf("lats" to lats, "lngs" to lngs)
        )
    }

    // ---------- Helpers ----------
    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    private fun formatPace(minPerKm: Double): String {
        val minutes = minPerKm.toInt()
        val seconds = ((minPerKm - minutes) * 60).toInt().coerceAtMost(59)
        return String.format("%d:%02d min/km", minutes, seconds)
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("profile").document("main")
            .get()
            .addOnSuccessListener { doc ->
                val p = doc.toObject(UserProfile::class.java) ?: UserProfile()
                binding.profileMeta.text = buildMeta(p)
            }
    }

    private fun showEditDialog() {
        val ctx = requireContext()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 8)
        }

        val tilName = TextInputLayout(ctx).apply { hint = "Name" }
        val etName = TextInputEditText(ctx)
        tilName.addView(etName)
        container.addView(tilName)

        val tilAge = TextInputLayout(ctx).apply {
            hint = "Age"
            setPadding(0, 16, 0, 0)
        }
        val etAge = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        tilAge.addView(etAge)
        container.addView(tilAge)

        val rg = RadioGroup(ctx).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val female = RadioButton(ctx).apply { id = View.generateViewId(); text = "Female" }
        val male = RadioButton(ctx).apply { id = View.generateViewId(); text = "Male" }
        rg.addView(female); rg.addView(male)
        container.addView(rg)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Edit details")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                val age = etAge.text?.toString()?.toIntOrNull() ?: 0
                val gender = when (rg.checkedRadioButtonId) {
                    female.id -> "Female"
                    male.id -> "Male"
                    else -> ""
                }
                saveProfile(UserProfile(name, age, gender))
            }
            .show()
    }

    private fun saveProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(uid)
            .collection("profile").document("main")
            .set(profile)
            .addOnSuccessListener {
                binding.profileMeta.text = buildMeta(profile)
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buildMeta(p: UserProfile): String {
        val parts = mutableListOf<String>()
        if (p.name.isNotBlank()) parts += p.name
        parts += "Age ${if (p.age > 0) p.age else "--"}"
        if (p.gender.isNotBlank()) parts += p.gender
        return parts.joinToString(" • ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
