package com.example.filmverse.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filmverse.R
import com.example.filmverse.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        InitView()
        LoggedIn()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.movieCountry)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.textView8.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.loginBtn.setOnClickListener {
            val username = binding.editTextText.text.toString()
            val pass = binding.editTextPassword.text.toString()

            if (username.isNotEmpty() && pass.isNotEmpty()) {
                database.reference.child("users").child(username).get().addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val storedPassword = dataSnapshot.child("password").getValue(String::class.java)

                        if (storedPassword != null && storedPassword == pass) {
                            val storedEmail = dataSnapshot.child("email").getValue(String::class.java)

                            saveUserSession(username, storedEmail ?: "")

                            if (storedEmail != null) {
                                firebaseAuth.signInWithEmailAndPassword(storedEmail, pass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = firebaseAuth.currentUser
                                            user?.getIdToken(true)?.addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val token = task.result!!.token
                                                    // Сохраняем токен в локальном хранилище
                                                    if (token != null) {
                                                        saveToken(token)
                                                    }
                                                } else {
                                                  Log.e("LoginActivity", "Ошибка")
                                                }
                                            }
                                        } else {
                                            Log.e("LoginActivity", "Ошибка")
                                        }
                                    }
                            }

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()

                        } else {
                            Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка получения данных: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Поля не заполнены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun LoggedIn(){
        if (isUserLoggedIn()) {
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val username = sharedPreferences.getString("username", "")
            val email = sharedPreferences.getString("email", "")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("EXTRA_USERNAME", username)
            intent.putExtra("EXTRA_EMAIL", email)
            startActivity(intent)
            finish()
        }
    }
    private fun InitView(){

        database = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }

    private fun saveUserSession(username: String, email: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.putString("email", email)
        editor.apply()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null) != null && sharedPreferences.getString("email", null) != null
    }
    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}