package com.luamuniz.dinlux.finance

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.FirestoreCollections
import java.util.Locale

/**
 * Tela única para adicionar um cartão novo a um banco já existente
 * ou editar um cartão já cadastrado
 *
 * O modo é definido pela presença do extra EXTRA_CARD_ID:
 * - ausente  -> modo "adicionar"
 * - presente -> modo "editar" (campos vêm pré-preenchidos via Intent)
 */
class InsertCard : AppCompatActivity() {

    companion object {
        const val EXTRA_BANK_ID = "bankId"
        const val EXTRA_CARD_ID = "cardId"
        const val EXTRA_CARD_LABEL = "cardLabel"
        const val EXTRA_CARD_LIMIT = "cardLimit"
        const val EXTRA_CARD_CLOSING_DAY = "cardClosingDay"
        const val EXTRA_CARD_DUE_DAY = "cardDueDay"
        const val EXTRA_CARD_HAS_INTEREST = "cardHasInterest"
        const val EXTRA_CARD_INTEREST_RATE = "cardInterestRate"

        private const val OUTRO = "Outro"
    }

    private lateinit var buttonToBack: ImageButton
    private lateinit var title: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var spinnerBrand: Spinner
    private lateinit var editOtherBrand: EditText
    private lateinit var editLimit: EditText
    private lateinit var editClosingDay: EditText
    private lateinit var editDueDay: EditText
    private lateinit var switchHasInterest: SwitchMaterial
    private lateinit var editInterestRate: EditText
    private lateinit var buttonSaveCard: AppCompatButton

    private val brands = listOf("Visa", "Mastercard", "Elo", "American Express", "Hipercard", OUTRO)
    private val db = FirebaseFirestore.getInstance()

    private lateinit var bankId: String
    private var cardId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insert_card)

        startComponents()

        // Ajusta o padding inferior do ScrollView conforme o teclado (ime) e as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            scrollView.setPadding(0, 0, 0, maxOf(keyboard.bottom, systemBars.bottom))
            insets
        }

        val extraBankId = intent.getStringExtra(EXTRA_BANK_ID)
        if (extraBankId == null) {
            Toast.makeText(this, "Banco não informado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        bankId = extraBankId
        cardId = intent.getStringExtra(EXTRA_CARD_ID)

        spinnerBrand.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brands)

        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isOutro = brands.getOrNull(position) == OUTRO
                editOtherBrand.visibility = if (isOutro) View.VISIBLE else View.GONE
                if (!isOutro) editOtherBrand.setText("")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchHasInterest.setOnCheckedChangeListener { _, isChecked ->
            editInterestRate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) editInterestRate.setText("")
        }

        if (cardId != null) {
            title.text = "Editar Cartão"
            preencherCamposParaEdicao()
        } else {
            title.text = "Novo Cartão"
        }

        buttonToBack.setOnClickListener { finish() }
        buttonSaveCard.setOnClickListener { salvarCartao() }
    }

    private fun preencherCamposParaEdicao() {
        val originalLabel = intent.getStringExtra(EXTRA_CARD_LABEL) ?: ""
        val knownBrand = brands.dropLast(1).firstOrNull { originalLabel.startsWith(it) }
        val brand = knownBrand ?: OUTRO
        spinnerBrand.setSelection(brands.indexOf(brand).coerceAtLeast(0))
        if (brand == OUTRO) {
            editOtherBrand.setText(originalLabel)
        }

        editLimit.setText(String.format(Locale("pt", "BR"), "%.2f", intent.getDoubleExtra(EXTRA_CARD_LIMIT, 0.0)))
        editClosingDay.setText(intent.getIntExtra(EXTRA_CARD_CLOSING_DAY, 0).toString())
        editDueDay.setText(intent.getIntExtra(EXTRA_CARD_DUE_DAY, 0).toString())
        val hasInterest = intent.getBooleanExtra(EXTRA_CARD_HAS_INTEREST, false)
        switchHasInterest.isChecked = hasInterest
        editInterestRate.visibility = if (hasInterest) View.VISIBLE else View.GONE

        if (hasInterest) {
            editInterestRate.setText(
                String.format(Locale("pt", "BR"), "%.2f", intent.getDoubleExtra(EXTRA_CARD_INTEREST_RATE, 0.0))
            )
        }
    }

    private fun salvarCartao() {
        val marcaSelecionada = spinnerBrand.selectedItem.toString()
        val marca = if (marcaSelecionada == OUTRO) {
            editOtherBrand.text.toString().trim()
        } else {
            marcaSelecionada
        }

        if (marca.isEmpty()) {
            Toast.makeText(this, "Digite o nome da bandeira do cartão", Toast.LENGTH_SHORT).show()
            return
        }

        val limiteTexto = editLimit.text.toString().replace(",", ".")
        val closingTexto = editClosingDay.text.toString()
        val dueTexto = editDueDay.text.toString()

        val limite = limiteTexto.toDoubleOrNull()
        val closingDay = closingTexto.toIntOrNull()
        val dueDay = dueTexto.toIntOrNull()

        if (limite == null || closingDay == null || dueDay == null ||
            closingDay !in 1..31 || dueDay !in 1..31
        ) {
            Toast.makeText(this, "Preencha corretamente todos os campos do cartão", Toast.LENGTH_SHORT).show()
            return
        }

        val temJuros = switchHasInterest.isChecked
        val taxaTexto = editInterestRate.text.toString().replace(",", ".")
        val taxaJuros = if (temJuros) taxaTexto.toDoubleOrNull() else 0.0

        if (temJuros && (taxaJuros == null || taxaJuros <= 0.0)) {
            Toast.makeText(this, "Informe a taxa de juros do cartão", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val bankRef = db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)

        bankRef.get().addOnSuccessListener { snapshot ->
            val bank = snapshot.toObject(Bank::class.java)
            if (bank == null) {
                Toast.makeText(this, "Banco não encontrado", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val existingId = cardId
            val label = computeLabel(bank.cards, marca, excludingCardId = existingId)

            val updatedCards = if (existingId != null) {
                bank.cards.map { card ->
                    if (card.id == existingId) {
                        card.copy(
                            label = label,
                            limit = limite,
                            closingDay = closingDay,
                            dueDay = dueDay,
                            hasInterest = temJuros,
                            interestRate = taxaJuros ?: 0.0
                        )
                    } else card
                }
            } else {
                bank.cards + Card(
                    label = label,
                    limit = limite,
                    closingDay = closingDay,
                    dueDay = dueDay,
                    hasInterest = temJuros,
                    interestRate = taxaJuros ?: 0.0
                )
            }

            bankRef.update("cards", updatedCards)
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar cartão", Toast.LENGTH_SHORT).show()
                }
            // A escrita já entrou no cache local (offline ou não) e sincroniza sozinha
            // quando conectar, não precisa esperar confirmação do servidor pra fechar a tela
            Toast.makeText(this, "Cartão salvo com sucesso", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao carregar banco", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Reaproveita o padrão de nomenclatura do InsertBank: o primeiro cartão de uma bandeira
     * fica só com o nome da bandeira, os seguintes ganham um número
     * Funciona igual para bandeiras digitadas em "Outro"
     * Ignora o próprio cartão sendo editado na contagem, para não duplicar sufixo nele mesmo
     */
    private fun computeLabel(cards: List<Card>, brand: String, excludingCardId: String?): String {
        val sameBrandCount = cards.count { it.label.startsWith(brand) && it.id != excludingCardId }
        return if (sameBrandCount > 0) "$brand ${sameBrandCount + 1}" else brand
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        title = findViewById(R.id.title_insert_card)
        scrollView = findViewById(R.id.scrollView)
        spinnerBrand = findViewById(R.id.spinner_brand)
        editOtherBrand = findViewById(R.id.edit_card_other_brand)
        editLimit = findViewById(R.id.edit_card_limit)
        editClosingDay = findViewById(R.id.edit_card_closing_day)
        editDueDay = findViewById(R.id.edit_card_due_day)
        switchHasInterest = findViewById(R.id.switch_has_interest)
        editInterestRate = findViewById(R.id.edit_card_interest_rate)
        buttonSaveCard = findViewById(R.id.button_save_card)
    }
}
