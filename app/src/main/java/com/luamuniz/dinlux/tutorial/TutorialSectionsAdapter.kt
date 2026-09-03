package com.luamuniz.dinlux.tutorial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

/**
 * Uma seção do índice do Tutorial: title mostrado na linha,
 * iconRes o ícone circular já pronto, e
 * key o identificador usado em TutorialContent pra achar os slides dessa seção
 */
data class TutorialSection(val title: String, val iconRes: Int, val key: String)

class TutorialSectionsAdapter(
    private val sections: List<TutorialSection>,
    private val onSectionClick: (TutorialSection) -> Unit
) : RecyclerView.Adapter<TutorialSectionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_section, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sections[position], onSectionClick)
    }

    override fun getItemCount(): Int = sections.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.image_tutorial_section_icon)
        private val title: TextView = view.findViewById(R.id.text_tutorial_section_title)

        fun bind(section: TutorialSection, onSectionClick: (TutorialSection) -> Unit) {
            icon.setImageResource(section.iconRes)
            title.text = section.title
            itemView.setOnClickListener { onSectionClick(section) }
        }
    }
}
