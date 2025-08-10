package com.example.runflow.ui.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.runflow.R
import kotlin.math.min

data class Plan(
    val title: String,
    val description: String,
    val difficulty: String,
    val imageRes: Int,
    val weeks: List<String>
)

class PlansViewModel : ViewModel() {

    // 5K – 3 אימונים/שבוע (4 שבועות)
    private fun weeks5K(): List<String> = listOf(
        "Week 1:\n1. Walk 2 min + Run 1 min × 6\n2. Run 1.5 km\n3. Run 2 km",
        "Week 2:\n1. Walk 1 min + Run 3 min × 4\n2. Run 2.5 km\n3. Run 3 km",
        "Week 3:\n1. Run 3.5 km\n2. Run 5 min / Walk 1 min × 3\n3. Run 4 km",
        "Week 4:\n1. Run 4.5 km\n2. Easy run 2 km\n3. Run 5 km"
    )

    // 10K – 3 אימונים/שבוע (10 שבועות)
    private fun weeks10K(): List<String> = listOf(
        "Week 1:\n1. Run 2 km\n2. Walk 1 min + Run 3 min × 5\n3. Run 3.5 km",
        "Week 2:\n1. Run 4 min / Walk 1 min × 4\n2. Run 4 km\n3. Tempo 2 km",
        "Week 3:\n1. Run 5 km\n2. 1 km intervals × 2\n3. Easy jog 3 km",
        "Week 4:\n1. Run 6 km\n2. Hill sprints 6×30 sec\n3. Tempo 3 km",
        "Week 5:\n1. Run 5 km\n2. 2 km fast / 1 km slow × 2\n3. Run 6.5 km",
        "Week 6:\n1. Tempo 4 km\n2. Intervals 3×1.5 km\n3. Run 7 km",
        "Week 7:\n1. Easy run 3.5 km\n2. Tempo 5 km\n3. Run 7.5 km",
        "Week 8:\n1. Jog 3 km\n2. Short tempo 2 km\n3. Run 5 km",
        "Week 9:\n1. Tempo 4 km\n2. Intervals 1 km × 3\n3. Run 8 km",
        "Week 10:\n1. Jog 2 km\n2. Easy run 3 km\n3. Race prep 5 km"
    )

    // Half Marathon – 3 אימונים/שבוע (22 שבועות), בנייה הדרגתית
    private fun weeksHalf(): List<String> = List(22) { week ->
        val w = week + 1
        val intervals = if (week % 2 == 0) "Intervals 6×400m" else "Intervals 4×800m"
        val tempoKm = 2 + (week / 4)
        val longKm = min(6 + week, 21) // לא עובר 21K
        "Week $w:\n1. $intervals\n2. Tempo $tempoKm km\n3. Long run $longKm km"
    }

    // Marathon – 3 אימונים/שבוע (35 שבועות), בנייה עד שיא ואז שמירה
    private fun weeksMarathon(): List<String> = List(35) { week ->
        val w = week + 1
        val intervals = when {
            week < 10 -> "Intervals 6×400m"
            week < 20 -> "Intervals 5×800m"
            week < 30 -> "Intervals 4×1 km"
            else      -> "Intervals 3×1.5 km"
        }
        val tempoKm = 3 + (week / 5)
        val longKm = min(12 + week, 35) // קאפ סביב 35K
        "Week $w:\n1. $intervals\n2. Tempo $tempoKm km\n3. Long run $longKm km"
    }

    private val _plans = MutableLiveData<List<Plan>>().apply {
        value = listOf(
            Plan(
                title = "5K Training Plan (4 Weeks)",
                description = "Beginner-friendly 4 weeks with exactly 3 key workouts per week. No rest days listed — העיקר להשלים את שלושת האימונים.",
                difficulty = "Easy",
                imageRes = R.drawable.k5,
                weeks = weeks5K()
            ),
            Plan(
                title = "10K Training Plan (10 Weeks)",
                description = "10 weeks, 3 key workouts per week: mix of intervals, tempo, and long run. No rest days listed.",
                difficulty = "Medium",
                imageRes = R.drawable.k10,
                weeks = weeks10K()
            ),
            Plan(
                title = "Half Marathon Training Plan (22 Weeks)",
                description = "22 weeks with 3 focused sessions each week: intervals, tempo, and progressive long run.",
                difficulty = "Hard",
                imageRes = R.drawable.halfmara,
                weeks = weeksHalf()
            ),
            Plan(
                title = "Marathon Training Plan (35 Weeks)",
                description = "35 weeks built around 3 weekly key sessions: intervals, tempo, and long run (gradual build).",
                difficulty = "Very Hard",
                imageRes = R.drawable.mara,
                weeks = weeksMarathon()
            )
        )
    }

    val plans: LiveData<List<Plan>> = _plans
}
