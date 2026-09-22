package com.luamuniz.dinlux.finance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.R

class InsertBank : AppCompatActivity() {

    private lateinit var editBankName: EditText
    private lateinit var editBankDebit: EditText
    private lateinit var containerCards: LinearLayout
    private lateinit var buttonAddCard: View
    private lateinit var buttonSaveBank: View
    private lateinit var buttonToBack: ImageButton
    private lateinit var scrollView: ScrollView

    private val brands = listOf("Visa", "Mastercard", "Elo", "American Express", "Hipercard", "Outro")
    private val outro = "Outro"

    private data class CardRow(
        val view: View,
        val spinnerBrand: Spinner,
        val editOtherBrand: EditText,
        val editLimit: EditText,
        val editClosingDay: EditText,
        val editDueDay: EditText,
        val switchHasInterest: android.widget.CompoundButton,
        val editInterestRate: EditText
    )

    private val cardRows = mutableListOf<CardRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insert_bank)

        editBankName = findViewById(R.id.edit_bank_name)
        editBankDebit = findViewById(R.id.edit_bank_debit)
        containerCards = findViewById(R.id.container_cards)
        buttonAddCard = findViewById(R.id.button_add_card)
        buttonSaveBank = findViewById(R.id.button_save_bank)
        buttonToBack = findViewById(R.id.button_toBack)
        scrollView = findViewById(R.id.scrollView)

        // Ajusta o padding inferior do ScrollView conforme o teclado (ime) e as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            scrollView.setPadding(0, 0, 0, maxOf(keyboard.bottom, systemBars.bottom))
            insets
        }

        buttonToBack.setOnClickListener { finish() }
        buttonAddCard.setOnClickListener { adicionarLinhaCartao() }
        buttonSaveBank.setOnClickListener { salvarBanco() }
    }

    private fun adicionarLinhaCartao() {
        val rowView = LayoutInflater.from(this)
            .inflate(R.layout.item_card_input, containerCards, false)

        val spinner = rowView.findViewById<Spinner>(R.id.spinner_brand)
        val editOtherBrand = rowView.findViewById<EditText>(R.id.edit_card_other_brand)
        val editLimit = rowView.findViewById<EditText>(R.id.edit_card_limit)
        val editClosingDay = rowView.findViewById<EditText>(R.id.edit_card_closing_day)
        val editDueDay = rowView.findViewById<EditText>(R.id.edit_card_due_day)
        val buttonRemove = rowView.findViewById<ImageButton>(R.id.button_remove_card)

        val switchInterest = rowView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_has_interest)
        val editInterestRate = rowView.findViewById<EditText>(R.id.edit_card_interest_rate)

        switchInterest.setOnCheckedChangeListener { _, isChecked ->
            editInterestRate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) editInterestRate.setText("")
        }

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brands)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isOutro = brands.getOrNull(position) == outro
                editOtherBrand.visibility = if (isOutro) View.VISIBLE else View.GONE
                if (!isOutro) editOtherBrand.setText("")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val row = CardRow(rowView, spinner, editOtherBrand, editLimit, editClosingDay, editDueDay, switchInterest, editInterestRate)
        cardRows.add(row)
        containerCards.addView(rowView)

        buttonRemove.setOnClickListener {
            containerCards.removeView(rowView)
            cardRows.remove(row)
        }
    }

    private fun salvarBanco() {
        val nome = editBankName.text.toString().trim()
        val debitoTexto = editBankDebit.text.toString().replace(",", ".")

        if (nome.isEmpty()) {
            Toast.makeText(this, "Informe o nome do banco", Toast.LENGTH_SHORT).show()
            return
        }
        val debito = debitoTexto.toDoubleOrNull()
        if (debito == null) {
            Toast.makeText(this, "Informe um débito válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Monta a lista de cartões, validando cada linha preenchida
        val cards = mutableListOf<Card>()
        val contadorPorMarca = mutableMapOf<String, Int>()

        for (row in cardRows) {
            val marcaSelecionada = row.spinnerBrand.selectedItem.toString()
            val limiteTexto = row.editLimit.text.toString().replace(",", ".")
            val closingTexto = row.editClosingDay.text.toString()
            val dueTexto = row.editDueDay.text.toString()

            // Se o usuário adicionou a linha mas não preencheu nada, ignora
            if (limiteTexto.isEmpty() && closingTexto.isEmpty() && dueTexto.isEmpty()) continue

            val marca = if (marcaSelecionada == outro) {
                row.editOtherBrand.text.toString().trim()
            } else {
                marcaSelecionada
            }

            if (marca.isEmpty()) {
                Toast.makeText(this, "Digite o nome da bandeira do cartão", Toast.LENGTH_SHORT).show()
                return
            }

            val limite = limiteTexto.toDoubleOrNull()
            val closingDay = closingTexto.toIntOrNull()
            val dueDay = dueTexto.toIntOrNull()

            if (limite == null || closingDay == null || dueDay == null ||
                closingDay !in 1..31 || dueDay !in 1..31) {
                Toast.makeText(this, "Preencha corretamente todos os campos do cartão $marca", Toast.LENGTH_SHORT).show()
                return
            }

            val temJuros = row.switchHasInterest.isChecked
            val taxaTexto = row.editInterestRate.text.toString().replace(",", ".")
            val taxaJuros = if (temJuros) taxaTexto.toDoubleOrNull() else 0.0

            if (temJuros && (taxaJuros == null || taxaJuros <= 0.0)) {
                Toast.makeText(this, "Informe a taxa de juros do cartão $marca", Toast.LENGTH_SHORT).show()
                return
            }

            val numero = (contadorPorMarca[marca] ?: 0) + 1
            contadorPorMarca[marca] = numero
            val label = if (numero > 1) "$marca $numero" else marca

            cards.add(
                Card(
                    label = label,
                    limit = limite,
                    closingDay = closingDay,
                    dueDay = dueDay,
                    hasInterest = temJuros,
                    interestRate = taxaJuros ?: 0.0
                )
            )
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val bank = Bank(name = nome, debit = debito, cards = cards)

        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("banks")
            .add(bank)
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar banco", Toast.LENGTH_SHORT).show()
            }
        // A escrita já entrou no cache local (offline ou não) e sincroniza sozinha
        // quando conectar, não precisa esperar confirmação do servidor pra fechar a tela
        Toast.makeText(this, "Banco salvo com sucesso", Toast.LENGTH_SHORT).show()
        finish()
    }
}
