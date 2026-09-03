package com.luamuniz.dinlux.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.home.Home

class Login : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonEnter: TextView
    private lateinit var buttonCreateUser: TextView
    private lateinit var buttonForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()
        observeUiState()

        buttonEnter.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(email, password)
            }
        }

        buttonCreateUser.setOnClickListener {
            startActivity(Intent(this, CreateUser::class.java))
        }

        buttonForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Loading -> progressBar.visibility = View.VISIBLE
                is AuthUiState.Success -> openHomeActivity()
                is AuthUiState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthUiState.Idle -> progressBar.visibility = View.GONE
            }
        }
    }

    private fun openHomeActivity() {
        startActivity(Intent(this, Home::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.isUserLoggedIn()) {
            openHomeActivity()
        }
    }

    private fun startComponents() {
        editEmail = findViewById(R.id.edit_email)
        editPassword = findViewById(R.id.edit_password)
        progressBar = findViewById(R.id.progressBar)
        buttonEnter = findViewById(R.id.button_enter)
        buttonCreateUser = findViewById(R.id.button_createUser)
        buttonForgotPassword = findViewById(R.id.button_forgotPassword)
    }
}
