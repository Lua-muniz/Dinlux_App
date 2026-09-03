package com.luamuniz.dinlux.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
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
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.home.Home

class AlterPassword : AppCompatActivity() {
    private lateinit var buttonToBack: ImageButton
    private lateinit var currentPassword: TextInputEditText
    private lateinit var newPassword1: TextInputEditText
    private lateinit var newPassword2: TextInputEditText
    private lateinit var buttonSaveNewPass: AppCompatButton

    // Instância do Firebase Auth
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alter_passward)

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

        buttonSaveNewPass.setOnClickListener {
            validateAndSavePassword()
        }
    }

    private fun validateAndSavePassword() {
        val currentPass = currentPassword.text.toString().trim()
        val newPass1 = newPassword1.text.toString().trim()
        val newPass2 = newPassword2.text.toString().trim()

        // Verificações de campos vazios
        if (currentPass.isEmpty()) {
            currentPassword.error = "Digite sua senha atual"
            return
        }

        if (newPass1.isEmpty()) {
            newPassword1.error = "Digite a nova senha"
            return
        }

        // O Firebase exige que a senha tenha pelo menos 6 caracteres
        if (newPass1.length < 6) {
            newPassword1.error = "A senha deve ter pelo menos 6 caracteres"
            return
        }

        if (newPass2.isEmpty()) {
            newPassword2.error = "Confirme a nova senha"
            return
        }

        // Verificação se as novas senhas coincidem
        if (newPass1 != newPass2) {
            newPassword2.error = "Senhas diferentes"
            return
        }

        // Verificação: Impedir que a nova senha seja igual à antiga
        if (currentPass == newPass1) {
            newPassword1.error = "A nova senha deve ser diferente da atual"
            return
        }

        // Obtém o usuário atual logado
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.email != null) {
            // Chama a função de reautenticação e atualização
            reauthenticateAndUpdatePassword(currentUser, currentUser.email!!, currentPass, newPass1)
        } else {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reauthenticateAndUpdatePassword(user: FirebaseUser, email: String, currentPass: String, newPass: String) {
        // Cria a credencial com o e-mail do usuário e a senha arual fornecida
        val credential = EmailAuthProvider.getCredential(email, currentPass)

        // Tenta reautenticar o usuário
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Se a reautenticação der certo, atualiza a senha para a senha
                user.updatePassword(newPass)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Senha alterada com sucesso!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Retorna para a tela anterior (Home)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao alterar a senha: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                // Se a reautenticação falhar
                currentPassword.error = "Senha atual incorreta"
                Toast.makeText(this, "Senha atual incorreta. Verifique e tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        currentPassword = findViewById(R.id.current_password)
        newPassword1 = findViewById(R.id.new_password_1)
        newPassword2 = findViewById(R.id.new_password_2)
        buttonSaveNewPass = findViewById(R.id.button_save_password)
    }
}
