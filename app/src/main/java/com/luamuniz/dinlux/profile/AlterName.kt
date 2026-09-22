package com.luamuniz.dinlux.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.AppCompatButton
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.home.Home

class AlterName : AppCompatActivity() {
    private lateinit var buttonToBack: ImageButton
    private lateinit var textNameUser: TextView
    private lateinit var editName: TextInputEditText
    private lateinit var btnSaveName: AppCompatButton

    private lateinit var db: FirebaseFirestore
    private lateinit var idUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alter_name)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()

        btnSaveName.setOnClickListener {
            saveNewName()
        }

        buttonToBack.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            idUser = currentUser.uid
            val dr: DocumentReference = db.collection(FirestoreCollections.USERS).document(idUser)

            dr.addSnapshotListener { documentSnapshot, error ->
                if (error != null) return@addSnapshotListener

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val nomeAtual = documentSnapshot.getString("nome") ?: ""
                    textNameUser.text = nomeAtual
                }
            }
        }
    }

    private fun saveNewName() {
        val newName = editName.text.toString().trim()

        if (newName.isEmpty()) {
            editName.error = "Digite um nome válido"
            return
        }

        val dr: DocumentReference = db.collection(FirestoreCollections.USERS).document(idUser)


        dr.set(mapOf("nome" to newName), SetOptions.merge())
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        // A escrita já entrou no cache local (offline ou não) e sincroniza sozinha
        // quando conectar, não precisa esperar confirmação do servidor pra fechar a tela
        Toast.makeText(this, "Nome alterado com sucesso", Toast.LENGTH_SHORT).show()
        finish() // Fecha a tela e volta para o perfil/home atualizado
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        textNameUser = findViewById(R.id.textNameUser)
        editName = findViewById(R.id.edit_name)
        btnSaveName = findViewById(R.id.btn_save_name)
    }
}
