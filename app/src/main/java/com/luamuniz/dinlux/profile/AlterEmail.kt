package com.luamuniz.dinlux.profile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.home.Home

class AlterEmail : AppCompatActivity() {
    private lateinit var buttonToBack: ImageButton
    private lateinit var textEmailUser: TextView
    private lateinit var editEmail: TextInputEditText
    private lateinit var currentPassword: TextInputEditText
    private lateinit var buttonSaveEmail: AppCompatButton
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var idUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alter_email)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()

        buttonToBack.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        buttonSaveEmail.setOnClickListener {
            validateAndSaveEmail()
        }
    }

    override fun onStart() {
        super.onStart()
        db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            idUser = currentUser.uid
            textEmailUser.text = "${currentUser.email ?: ""}"
        }
    }

    private fun validateAndSaveEmail() {
        val newEmail = editEmail.text.toString().trim()
        val password = currentPassword.text.toString().trim()

        if (newEmail.isEmpty()) {
            editEmail.error = "Digite o novo e-mail"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editEmail.error = "Formato de e-mail inválido"
            return
        }

        if (password.isEmpty()) {
            currentPassword.error = "Digite sua senha para confirmar"
            return
        }

        val currentUser = auth.currentUser ?: return

        if (newEmail == currentUser.email) {
            Toast.makeText(this, "Este já é o seu e-mail atual", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Checa se o e-mail já existe na coleção de usuários do Firestore
        db.collection(FirestoreCollections.USERS)
            .whereEqualTo("email", newEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    editEmail.error = "Este e-mail já está em uso"
                    Toast.makeText(this, "E-mail já cadastrado no sistema", Toast.LENGTH_SHORT).show()
                } else {
                    // 2. Reautentica e dispara a verificação do novo e-mail
                    reauthenticateAndUpdate(currentUser, newEmail, password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao verificar e-mail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reauthenticateAndUpdate(user: FirebaseUser, newEmail: String, password: String) {
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential).addOnSuccessListener {
                // Envia um link de verificação para o novo e-mail em vez de trocar diretamente
                user.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener {
                        // Atualiza também no Firestore o novo e-mail pendente/alterado
                        db.collection(FirestoreCollections.USERS).document(idUser)
                            .set(mapOf("email" to newEmail), SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "E-mail de confirmação enviado para o novo endereço! Verifique sua caixa de entrada.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao atualizar no banco: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao enviar verificação: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                currentPassword.error = "Senha incorreta"
                Toast.makeText(this, "Senha incorreta. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        textEmailUser = findViewById(R.id.textEmailUser)
        editEmail = findViewById(R.id.edit_email)
        currentPassword = findViewById(R.id.current_password)
        buttonSaveEmail = findViewById(R.id.button_save_email)
    }
}
