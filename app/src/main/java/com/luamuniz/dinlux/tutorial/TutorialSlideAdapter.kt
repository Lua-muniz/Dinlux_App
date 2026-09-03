package com.luamuniz.dinlux.tutorial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

/**
 * Adapter dos slides dentro de uma seção do Tutorial
 */
class TutorialSlideAdapter(
    private val slides: List<TutorialSlide>
) : RecyclerView.Adapter<TutorialSlideAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_slide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.image_tutorial_slide)
        private val image2: ImageView = view.findViewById(R.id.image_tutorial_slide_2)
        private val title: TextView = view.findViewById(R.id.text_tutorial_slide_title)
        private val text: TextView = view.findViewById(R.id.text_tutorial_slide_body)
        private val blocksContainer: LinearLayout = view.findViewById(R.id.layout_tutorial_slide_blocks)
        private val density = view.resources.displayMetrics.density

        fun bind(slide: TutorialSlide) {
            title.text = slide.title

            if (slide.blocks != null) {
                // Slide com blocos imagem mais texto intercalados
                image.visibility = View.GONE
                image2.visibility = View.GONE
                text.visibility = View.GONE
                blocksContainer.visibility = View.VISIBLE
                montarBlocos(slide.blocks)
                return
            }

            blocksContainer.visibility = View.GONE
            blocksContainer.removeAllViews()
            text.visibility = View.VISIBLE
            text.text = slide.text

            if (slide.imageRes != null) {
                image.visibility = View.VISIBLE
                image.setImageResource(slide.imageRes)
            } else {
                image.visibility = View.GONE
            }
            // Segunda imagem opcional
            if (slide.imageRes2 != null) {
                image2.visibility = View.VISIBLE
                image2.setImageResource(slide.imageRes2)
            } else {
                image2.visibility = View.GONE
            }
        }

        // Monta, em código, um ImageView (se o bloco tiver imagem) seguido de um TextView
        // pra cada TutorialSlideBlock, na mesma ordem da lista
        private fun montarBlocos(blocks: List<TutorialSlideBlock>) {
            blocksContainer.removeAllViews()
            val dp20 = (20 * density).toInt()
            val corTextoPadrao = text.currentTextColor

            for (bloco in blocks) {
                if (bloco.imageRes != null) {
                    val imageView = ImageView(blocksContainer.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = (8 * density).toInt() }
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageResource(bloco.imageRes)
                    }
                    blocksContainer.addView(imageView)
                }
                val textView = TextView(blocksContainer.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp20 }
                    setTextColor(corTextoPadrao)
                    textSize = 16f
                    setLineSpacing(4 * density, 1f)
                    text = bloco.text
                }
                blocksContainer.addView(textView)
            }
        }
    }
}
