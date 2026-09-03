package com.luamuniz.dinlux.tutorial

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

class Tutorial : AppCompatActivity() {

    private lateinit var buttonToBack: ImageButton
    private lateinit var recyclerSections: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutorial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        recyclerSections = findViewById(R.id.recycler_tutorial_sections)

        buttonToBack.setOnClickListener { finish() }

        val secoes = listOf(
            TutorialSection("Menu de Perfil", R.drawable.ic_tutorial_menu_perfil, "menu_perfil"),
            TutorialSection("Listas", R.drawable.ic_tutorial_listas, "listas"),
            TutorialSection("Importar Extrato", R.drawable.ic_tutorial_importar_extrato, "importar_extrato"),
            TutorialSection("Finanças", R.drawable.ic_tutorial_financas, "financas"),
            TutorialSection("Simulações", R.drawable.ic_tutorial_simulacoes, "simulacoes"),
            TutorialSection("Avisos", R.drawable.ic_tutorial_avisos, "avisos"),
            TutorialSection("Gráficos", R.drawable.ic_tutorial_graficos, "graficos")
        )

        recyclerSections.layoutManager = LinearLayoutManager(this)
        recyclerSections.adapter = TutorialSectionsAdapter(secoes) { secao ->
            if (TutorialContent.SECOES.containsKey(secao.key)) {
                val intent = Intent(this, TutorialSlideActivity::class.java)
                intent.putExtra(TutorialSlideActivity.EXTRA_SECTION_KEY, secao.key)
                intent.putExtra(TutorialSlideActivity.EXTRA_SECTION_TITLE, secao.title)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Em breve: explicação de \"${secao.title}\"", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
