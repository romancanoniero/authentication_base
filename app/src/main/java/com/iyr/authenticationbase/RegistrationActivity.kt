package com.iyr.authenticationbase

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.iyr.authenticationbase.databinding.ActivityRegistrationBinding
import com.iyr.authenticationbase.models.User

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)

        val bundle = intent.extras
        val userObject: String? = bundle?.getString("user_object")
        val user = Gson().fromJson<User>(userObject, User::class.java)
        val email: String? = user.email?:""
        val provider: String? = user.provider?:""
        val senderId: String? = user.recomender_id?:""
        setup(user)


    }

    private fun setup(user : User) {

        title = "Registration"
        binding.emailTextView.text = user.email
        binding.providerTextView.text = user.provider
        binding.senderIdTextView.text = user.recomender_id
        binding.displayNameEditText.setText(user.display_name)

        binding.saveAndContinue.setOnClickListener {


            if (binding.displayNameEditText.text.isEmpty()) {
                return@setOnClickListener
            }
            user.display_name = binding.displayNameEditText.text.toString()
         //   val data = HashMap<String, Any>()


            FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().uid.toString())
                .setValue(user)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                    {
                        // va a la pantalla principal
                        var userAsJson = Gson().toJson(user, User::class.java)

                        val homeIntent =
                            Intent(this@RegistrationActivity, HomeActivity::class.java).apply {
                                putExtra("user_object", userAsJson)
                            }

                        startActivity(homeIntent)
                    }
                    else
                    {
                       //
                        Utils.instance.showAlert(this, "Registration - Error", it.exception?.message?:"")
                    }
                }

        }
    }
}