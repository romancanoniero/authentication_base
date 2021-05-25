package com.iyr.authenticationbase

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.iyr.authenticationbase.databinding.ActivityHomeBinding
import com.iyr.authenticationbase.models.User

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(getLayoutInflater())
        val view = binding.root
        setContentView(view)

        val bundle = intent.extras
        val user = Gson().fromJson<User>(bundle?.getString("user_object"), User::class.java)

        val email: String? = user.email
        val provider: String? = user.provider
        setup(email ?: "", provider ?: "")

        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()


    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"

   //     binding = ActivityHomeBinding.inflate(getLayoutInflater())
        binding.emailTextView.text = email
        binding.providerTextView.text = provider

     //   findViewById<Button>(R.id.logoutButton)

        binding.logoutButton.setOnClickListener {
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()
            if (provider == ProviderType.FACEBOOK.name)
            {
                LoginManager.getInstance().logOut()
            }

            FirebaseAuth.getInstance().signOut()
            onBackPressed()

        }
    }
}