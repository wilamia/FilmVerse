package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.filmverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {
    private lateinit var userId: String
    private lateinit var profileImg: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        profileImg = findViewById(R.id.profileImg)
        loadProfileImage()
        val usernameFromIntent = intent.getStringExtra("EXTRA_USERNAME")
        val emailFromIntent = intent.getStringExtra("EXTRA_EMAIL")

        Log.d("SettingsActivity", "Username from intent: $usernameFromIntent")
        Log.d("SettingsActivity", "Email from intent: $emailFromIntent")

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("SettingsActivity", "Current user: ${currentUser?.email}")

        findViewById<ImageView>(R.id.backImage).setOnClickListener{
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            finish()
        }
        if (usernameFromIntent != null) {
            val usernameEditText = findViewById<EditText>(R.id.editText3)
            usernameEditText.setText(currentUser?.displayName ?: usernameFromIntent)
            userId = usernameFromIntent
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            finish() // Завершить активность, если пользователь не найден
        }

        findViewById<Button>(R.id.changePassword).setOnClickListener {
            showChangePasswordDialog()
        }
        findViewById<Button>(R.id.signUp).setOnClickListener {
            showChangeDataDialog()
        }
    }
    private fun loadProfileImage() {
        Glide.with(this)
            .load(R.drawable.ic_profile)
            .circleCrop()
            .into(profileImg)
    }
    @SuppressLint("MissingInflatedId")
    private fun showChangeDataDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_data, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.button_confirm)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)

        val dialog = builder.create() // Создаем диалог до установки слушателей

        // Устанавливаем слушатель на кнопку "Подтвердить"
        confirmButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString().trim()

            // Валидация данных
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUserData(currentPassword)
            findViewById<EditText>(R.id.editText3).setText("")
            Toast.makeText(this, "Имя пользователя успешно изменено", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)
        val newPasswordEditText2 = dialogView.findViewById<EditText>(R.id.newPasswordEditText2)
        val confirmButton = dialogView.findViewById<Button>(R.id.button_confirm)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)

        val dialog = builder.create()

        confirmButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val newPassword2 = newPasswordEditText2.text.toString().trim()

            // Валидация данных
            if (currentPassword.isEmpty() || newPassword.isEmpty() || newPassword2.isEmpty()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass the newPasswordEditText to the updateUserPassword function
            updateUserPassword(currentPassword, newPasswordEditText)
            findViewById<EditText>(R.id.editText3).setText("")
            Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun updateUserPassword(currentPassword: String, newPasswordEditText: EditText) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.reference.child("users").child(userId)

        // Get the new password from the passed EditText
        val newPassword = newPasswordEditText.text.toString().trim()

        userRef.updateChildren(mapOf("password" to newPassword))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (userRef != null) {
                        updatePasswordInDatabase(currentPassword, newPassword, userRef.toString())
                    }
                } else {
                    Log.e("SettingsActivity", "Error updating username: ${task.exception?.message}")
                    Toast.makeText(this, "Ошибка обновления имени пользователя", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun updateUserData(currentPassword: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.reference.child("users").child(userId)

        // Update username directly
        val newUsername = findViewById<EditText>(R.id.editText3).text.toString().trim()

        userRef.updateChildren(mapOf("username" to newUsername))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateUserInDatabase(userId, newUsername)
                } else {
                    Log.e("SettingsActivity", "Error updating username: ${task.exception?.message}")
                    Toast.makeText(this, "Ошибка обновления имени пользователя", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun updatePasswordInDatabase(oldPassword: String, newPassword: String, username: String) {
        val database = FirebaseDatabase.getInstance()

        if (oldPassword != newPassword) {
            val userRefOld = database.reference.child("users").child(username)
            userRefOld.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val email = dataSnapshot.child("email").getValue(String::class.java)
                    val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                    sharedPreferences.edit().putString("password", newPassword).apply()

                    val userRefNew = database.reference.child("users").child(username) // Исправлено имя переменной
                    userRefNew.setValue(mapOf("username" to username, "email" to email, "password" to newPassword)).addOnCompleteListener { newTask ->
                        if (newTask.isSuccessful) {

                            userRefOld.removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    Log.d("SettingsActivity", "Successfully updated password for user: $username")
                                    val intent = Intent("UPDATE_USER_DATA")
                                    intent.putExtra("newPassword", newPassword)
                                    sendBroadcast(intent)

                                    if (email != null && oldPassword != null) {
                                        signInUser(email, newPassword) {}
                                    }
                                } else {
                                    Log.e("SettingsActivity", "Error deleting old user data: ${removeTask.exception?.message}")
                                }
                            }
                        } else {
                            Log.e("SettingsActivity", "Error updating data in Realtime Database for user: ${newTask.exception?.message}")
                        }
                    }
                } else {
                    Log.e("SettingsActivity", "Old user data not found in database")
                }
            }.addOnFailureListener { error ->
                Log.e("SettingsActivity", "Error retrieving old user data: ${error.message}")
            }
        } else {
            Log.e("SettingsActivity", "Old password must not match new password")
        }
    }

    private fun updateUserInDatabase(oldUsername: String, newUsername: String) {
        val database = FirebaseDatabase.getInstance()
        val userRefOld = database.reference.child("users").child(oldUsername)

        if (oldUsername != newUsername) {
            userRefOld.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val email = dataSnapshot.child("email").getValue(String::class.java)
                    val password = dataSnapshot.child("password").getValue(String::class.java)
                    val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                    sharedPreferences.edit().putString("username", newUsername).apply()

                    val userRefNew = database.reference.child("users").child(newUsername)
                    userRefNew.setValue(mapOf("username" to newUsername, "email" to email, "password" to password)).addOnCompleteListener { newTask ->
                        if (newTask.isSuccessful) {
                            userRefOld.removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    Log.d("SettingsActivity", "Successfully updated username from $oldUsername to $newUsername")
                                    val intent = Intent("UPDATE_USER_DATA")
                                    intent.putExtra("newUsername", newUsername)
                                    sendBroadcast(intent)

                                    if (email != null && password != null) {
                                        signInUser(email, password) {}
                                    }
                                } else {
                                    Log.e("SettingsActivity", "Error deleting old user data: ${removeTask.exception?.message}")
                                }
                            }
                        } else {
                            Log.e("SettingsActivity", "Error updating data in Realtime Database for new user: ${newTask.exception?.message}")
                        }
                    }
                } else {
                    Log.e("SettingsActivity", "Old username data not found in database")
                }
            }.addOnFailureListener { error ->
                Log.e("SettingsActivity", "Error retrieving old user data: ${error.message}")
            }
        }
    }

    private fun signInUser(email: String, password: String, onSuccess: () -> Unit) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SettingsActivity", "User signed in successfully after username update.")
                    onSuccess()  // Call the callback function on success
                } else {
                    Log.e("SettingsActivity", "Error signing in: ${task.exception?.message}")
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
        finish()
    }
}