package com.luamuniz.dinlux.simulation

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout

/**
 * Container com zoom (pinça) e pan (arrastar) para o canvas de nós da simulação
 *
 * Não depende de biblioteca externa: usa ScaleGestureDetector pro pinça-pra-zoom e
 * GestureDetector.onScroll pro arraste com um dedo, aplicando escala/translação num
 * único filho (content), que vai hospedar os nós e as arestas do grafo nas próximas
 * etapas
 */
class ZoomPanCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 3f
    }

    /** Filho único que recebe as transformações de escala/translação */
    val content: FrameLayout = FrameLayout(context)

    private var scaleFactor = 1f

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, PanListener())

    init {
        addView(content, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            content.scaleX = scaleFactor
            content.scaleY = scaleFactor
            return true
        }
    }

    private inner class PanListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            content.translationX -= distanceX
            content.translationY -= distanceY
            return true
        }
    }
}
