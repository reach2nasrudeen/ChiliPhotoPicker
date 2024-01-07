package lv.chi.photopicker.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

internal class SpacingItemDecoration(
    private val spacingLeft: Int = 0,
    private val spacingTop: Int = 0,
    private val spacingRight: Int = 0,
    private val spacingBottom: Int = 0
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(out: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        out.set(spacingLeft, spacingTop, spacingRight, spacingBottom)
    }
}