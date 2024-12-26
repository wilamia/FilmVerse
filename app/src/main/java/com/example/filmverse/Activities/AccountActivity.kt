package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.filmverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class AccountActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var profileImg: ImageView
    private lateinit var favourite: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        firebaseAuth = FirebaseAuth.getInstance()
        setupWindowInsets()
        profileImg = findViewById(R.id.profileImg)
        loadProfileImage()

        val currentUser = firebaseAuth.currentUser
        val username = getUserSession()
        val email = currentUser?.email
        favourite = findViewById(R.id.button2)
        favourite.setOnClickListener{
            val intent = Intent(this, FavouriteActivity::class.java)
            startActivity(intent)
            finish()
        }
        Log.d("AccountActivity", "Retrieved username: $username")

        if (username != null) {
            loadUserData()
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<ImageView>(R.id.backImage).setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("EXTRA_USERNAME", username)
                putExtra("EXTRA_EMAIL", email)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.logOutBtn).setOnClickListener { logout() }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadProfileImage() {
        Glide.with(this)
            .load(R.drawable.ic_profile)
            .circleCrop()
            .into(profileImg)
    }

    private fun loadUserData() {
        val username = getUserSession()
        Log.d("AccountActivity", "Current username: $username") // Лог для проверки

        if (username != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.reference.child("users").child(username)

            userRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val email = dataSnapshot.child("email").getValue(String::class.java)
                    val usernameLoaded = dataSnapshot.child("username").getValue(String::class.java)

                    findViewById<TextView>(R.id.textView21).text = usernameLoaded ?: "Имя не найдено"
                    findViewById<TextView>(R.id.textViewEmail).text = email ?: "Email не найден"
                } else {
                    Toast.makeText(this, "Данные пользователя не найдены в базе данных", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Имя пользователя не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.button_confirm).setOnClickListener {
            firebaseAuth.signOut()
            clearUserSession()
            Log.d("AccountActivity", "User successfully logged out")
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private val updateUserDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "UPDATE_USER_DATA") {
                val newUsername = intent.getStringExtra("newUsername")
                if (newUsername != null) {
                    // Update the displayed username
                    findViewById<TextView>(R.id.textView21).text = newUsername
                    Toast.makeText(context, "Username updated to $newUsername", Toast.LENGTH_SHORT).show()

                    // Optionally refresh other user data
                    loadUserData()
                }
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("UPDATE_USER_DATA")
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUserDataReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateUserDataReceiver)
    }

    private fun getUserSession(): String? {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }

    private fun clearUserSession() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}