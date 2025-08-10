package com.example.runflow.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceDecoration(private val spaceDp: Int) : RecyclerView.ItemDecoration() {
    private fun Int.dp(view: View) = (this * view.resources.displayMetrics.density).toInt()
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val px = spaceDp.dp(view)
        outRect.top = px / 2
        outRect.bottom = px / 2
    }
}
