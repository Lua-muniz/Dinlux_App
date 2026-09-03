package com.luamuniz.dinlux.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.TermosDePrivacidade
import com.luamuniz.dinlux.home.Home

class CreateUser : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var editName: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var textTermsContent: TextView
    private lateinit var checkboxTermsAccept: CheckBox
    private lateinit var buttonCreateUser: AppCompatButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_user)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()
        observeUiState()

        textTermsContent.text = TermosDePrivacidade.TEXTO

        buttonCreateUser.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                }
                // Caixa de aceite dos Termos de Privacidade. Campo obrigatório sem marcar, a conta não é criada
                !checkboxTermsAccept.isChecked -> {
                    Toast.makeText(
                        this,
                        "Você precisa aceitar os Termos de Privacidade para criar sua conta.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    viewModel.createUser(name, email, password, termsAccepted = true)
                }
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Loading -> progressBar.visibility = View.VISIBLE
                is AuthUiState.Success -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.success_account_created), Toast.LENGTH_SHORT).show()
                    openHomeActivity()
                }
                is AuthUiState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthUiState.Idle -> progressBar.visibility = View.GONE
            }
        }
    }

    private fun openHomeActivity() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }

    private fun startComponents() {
        editName = findViewById(R.id.edit_name)
        editEmail = findViewById(R.id.edit_email)
        editPassword = findViewById(R.id.edit_password)
        textTermsContent = findViewById(R.id.text_terms_content)
        checkboxTermsAccept = findViewById(R.id.checkbox_terms_accept)
        progressBar = findViewById(R.id.progressBar)
        buttonCreateUser = findViewById(R.id.button_create)
    }
}
