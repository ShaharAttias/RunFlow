package com.example.runflow.ui.plans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runflow.databinding.PlanCardBinding

class PlanAdapter(
    private val onClick: (Plan) -> Unit
) : ListAdapter<Plan, PlanAdapter.PlanViewHolder>(PlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = PlanCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = getItem(position)
        holder.bind(plan)
    }

    inner class PlanViewHolder(private val binding: PlanCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: Plan) {
            binding.planLBLTitle.text = plan.title
            binding.planLBLDescription.text = plan.description
            binding.planLBLDifficulty.text = plan.difficulty
            binding.planIMGCover.setImageResource(plan.imageRes)

            binding.root.setOnClickListener {
                onClick(plan)
            }
        }
    }

    class PlanDiffCallback : DiffUtil.ItemCallback<Plan>() {
        override fun areItemsTheSame(oldItem: Plan, newItem: Plan): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Plan, newItem: Plan): Boolean {
            return oldItem == newItem
        }
    }
}
