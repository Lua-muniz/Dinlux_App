package com.luamuniz.dinlux.core

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView que encolhe pro tamanho do conteúdo (poucos itens) mas nunca passa de
 * maxHeightPx (muitos itens). Nesse caso vira scroll interno em vez de esticar e
 * quebrar o layout ao redor
 */
class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    var maxHeightPx: Int = Int.MAX_VALUE

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val cappedHeightSpec = if (maxHeightPx == Int.MAX_VALUE) {
            heightSpec
        } else {
            MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthSpec, cappedHeightSpec)
    }
}
