package com.luamuniz.dinlux.authentication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.luamuniz.dinlux.R

class ForgotPassword : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var editEmail: TextInputEditText
    private lateinit var buttonSendResetLink: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()
        observeUiState()

        buttonSendResetLink.setOnClickListener {
            val email = editEmail.text.toString().trim()

            if (email.isEmpty()) {
                editEmail.error = getString(R.string.error_enter_email)
            } else {
                viewModel.sendPasswordReset(email)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Success -> {
                    Toast.makeText(this, getString(R.string.success_reset_link_sent), Toast.LENGTH_LONG).show()
                    finish()
                }
                is AuthUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    private fun startComponents() {
        editEmail = findViewById(R.id.edit_email)
        buttonSendResetLink = findViewById(R.id.button_send_reset_link)
    }
}
