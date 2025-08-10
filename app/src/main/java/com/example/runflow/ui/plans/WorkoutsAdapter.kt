package com.example.runflow.ui.plans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.runflow.R
import com.example.runflow.model.Workout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox

class WorkoutsAdapter(
    private val onMarkCompleted: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutsAdapter.VH>() {

    private val data = mutableListOf<Workout>()

    fun submit(items: List<Workout>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(data[position], onMarkCompleted)
    }

    override fun getItemCount() = data.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView? = itemView as? MaterialCardView
        private val title: TextView = itemView.findViewById(R.id.item_workout_title)
        private val checkBtn: MaterialCheckBox = itemView.findViewById(R.id.item_workout_check)

        fun bind(item: Workout, onMarkCompleted: (Workout) -> Unit) {
            title.text = item.title
            renderState(item.completed)
            (card ?: itemView).setOnClickListener { /* no-op */ }
            checkBtn.setOnClickListener {
                if (!item.completed) onMarkCompleted(item)
            }
        }

        private fun renderState(completed: Boolean) {
            checkBtn.isChecked = completed
            checkBtn.isEnabled = !completed
            card?.let { c ->
                c.strokeWidth = if (completed) 2 else 1
                c.strokeColor = ContextCompat.getColor(
                    itemView.context,
                    if (completed)
                        com.google.android.material.R.color.m3_ref_palette_primary40
                    else
                        android.R.color.darker_gray
                )
            } ?: run { itemView.alpha = if (completed) 0.95f else 1f }
        }
    }
}
