package com.luamuniz.dinlux.tutorial

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.luamuniz.dinlux.R

/**
 * Tela de slides deslizáveis explicando uma seção do Tutorial em detalhe
 */
class TutorialSlideActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SECTION_KEY = "section_key"
        const val EXTRA_SECTION_TITLE = "section_title"
        private const val TAMANHO_BOLINHA_DP = 8
        private const val TAMANHO_BOLINHA_ATIVA_DP = 10
        private const val ESPACO_ENTRE_BOLINHAS_DP = 4
    }

    private lateinit var buttonToBack: ImageButton
    private lateinit var title: TextView
    private lateinit var pager: ViewPager2
    private lateinit var containerDots: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutorial_slide)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        title = findViewById(R.id.title_tutorial_slide)
        pager = findViewById(R.id.pager_tutorial_slides)
        containerDots = findViewById(R.id.container_tutorial_dots)

        buttonToBack.setOnClickListener { finish() }

        val sectionKey = intent.getStringExtra(EXTRA_SECTION_KEY)
        val sectionTitle = intent.getStringExtra(EXTRA_SECTION_TITLE) ?: ""
        title.text = sectionTitle

        val slides = TutorialContent.SECOES[sectionKey].orEmpty()
        pager.adapter = TutorialSlideAdapter(slides)

        montarBolinhas(slides.size)
        atualizarBolinhaAtiva(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                atualizarBolinhaAtiva(position)
            }
        })
    }

    private fun montarBolinhas(quantidade: Int) {
        containerDots.removeAllViews()
        if (quantidade <= 1) {
            containerDots.visibility = View.GONE
            return
        }
        containerDots.visibility = View.VISIBLE
        val espaco = dpParaPx(ESPACO_ENTRE_BOLINHAS_DP)
        repeat(quantidade) {
            val bolinha = View(this)
            val params = LinearLayout.LayoutParams(dpParaPx(TAMANHO_BOLINHA_DP), dpParaPx(TAMANHO_BOLINHA_DP))
            params.marginStart = espaco
            params.marginEnd = espaco
            bolinha.layoutParams = params
            containerDots.addView(bolinha)
        }
    }

    private fun atualizarBolinhaAtiva(posicaoAtiva: Int) {
        for (i in 0 until containerDots.childCount) {
            val bolinha = containerDots.getChildAt(i)
            val ativa = i == posicaoAtiva
            val tamanho = dpParaPx(if (ativa) TAMANHO_BOLINHA_ATIVA_DP else TAMANHO_BOLINHA_DP)
            val params = bolinha.layoutParams
            params.width = tamanho
            params.height = tamanho
            bolinha.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(getColor(if (ativa) R.color.cyan else R.color.dashboard_track_bg))
            bolinha.background = drawable
        }
    }

    private fun dpParaPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
