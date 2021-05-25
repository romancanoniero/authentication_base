package com.iyr.authenticationbase


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.gson.Gson
import com.iyr.authenticationbase.databinding.ActivityAuthBinding
import com.iyr.authenticationbase.models.User
import java.util.*


enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class AuthActivity : AppCompatActivity() {

    private val callbackManager = CallbackManager.Factory.create()

    private val GOOGLE_SIGN_IN: Int = 100
    private val FACEBOOK_SIGN_IN: Int = 64206
    private var mAuth: FirebaseAuth? = null
    private lateinit var binding: ActivityAuthBinding
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        mAuth = FirebaseAuth.getInstance();

        //    getDynamicLink()
        setup()
        if (FirebaseAuth.getInstance().currentUser != null) {
            session()
        }
    }


    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)

        if (prefs.getString("email", null) != null &&
            prefs.getString("provider", null) != null
        ) {
            binding.authLayout.visibility = View.GONE
            binding.loadingLayout.visibility = View.VISIBLE

            var user = User()
            user.email = prefs.getString("email", null)
            user.provider = prefs.getString("provider", null)

            showHome(user)
        } else {
            binding.authLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
        }
    }

    private fun setup() {
        title = "Autenticacion"

        //  signUpButton.

        binding.signUpButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()) {

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    binding.emailEditText.text.toString(),
                    binding.passwordEditText.text.toString()
                )
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            var user = User()
                            user.email = it.result?.user?.email.toString()
                            user.provider = ProviderType.BASIC.name

                            showHome(user)
                        } else {

                            Utils.instance.showAlert(
                                this,
                                "Signup Error",
                                it?.exception?.message ?: ""
                            )


                        }
                    }
            }
        }

        binding.loginButton.setOnClickListener {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                binding.emailEditText.text.toString(),
                binding.passwordEditText.text.toString()
            )
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        var user = User()
                        user.email = it.result?.user?.email.toString()
                        user.provider = ProviderType.BASIC.name

                        showHome(user)
                    } else {

                        Utils.instance.showAlert(this, "Login Error", it?.exception?.message ?: "")
                    }

                }

        }

        binding.googleButton.setOnClickListener {

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)
            client.signOut()
            startActivityForResult(client.signInIntent, GOOGLE_SIGN_IN)

        }

        binding.facebookButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult?) {
                        result?.let {
                            val token = it.accessToken
                            var credential = FacebookAuthProvider.getCredential(token.token)
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {

                                        var user = User()
                                        user.email = it?.result?.user?.email ?: ""
                                        user.provider = ProviderType.FACEBOOK.name

                                        showHome(user)

                                    } else {

                                        Utils.instance.showAlert(
                                            this@AuthActivity,
                                            "Login Error",
                                            it?.exception?.message ?: ""
                                        )
                                    }
                                }

                        }
                    }

                    override fun onCancel() {
                        //                TODO("Not yet implemented")
                    }

                    override fun onError(error: FacebookException?) {
                        Utils.instance.showAlert(
                            this@AuthActivity,
                            "Login Error",
                            error?.message ?: ""
                        )

                    }
                })
        }


    }

    //   private fun showHome(email: String, provider: ProviderType) {
    private fun showHome(user: User) {

        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        usersRef.child(FirebaseAuth.getInstance().uid.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        var user: User? = snapshot.getValue(User::class.java)
                        var userAsJson = Gson().toJson(user, User::class.java)

                        if (minimumUserReqsCompleted(snapshot)) {
                            val homeIntent =
                                Intent(this@AuthActivity, HomeActivity::class.java).apply {
                                    putExtra("user_object", userAsJson)

                                }

                            startActivity(homeIntent)
                        } else {

                            val profileSetupIntent =
                                Intent(this@AuthActivity, RegistrationActivity::class.java).apply {
                                    putExtra("user_object", userAsJson)
                                }
                            startActivity(profileSetupIntent)
                        }
                    } else {
                        /*
                        var user = User()
                        user.email = email
                        user.provider = provider.name.toString()

*/
                        val profileSetupIntent =
                            Intent(this@AuthActivity, RegistrationActivity::class.java)

                        FirebaseDynamicLinks.getInstance()
                            .getDynamicLink(intent)
                            .addOnSuccessListener(
                                this@AuthActivity
                            ) { pendingDynamicLinkData ->
                                // Get deep link from result (may be null if no link is found)
                                var deepLink: Uri? = null
                                if (pendingDynamicLinkData != null) {
                                    deepLink = pendingDynamicLinkData.link
                                    val senderId =
                                        deepLink?.getQueryParameter("sender_id") ?: ""


                                    user.recomender_id = senderId

                                    Toast.makeText(
                                        this@AuthActivity,
                                        "Sender ID=" + senderId,
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.d("depplink", deepLink.toString())
                                    // profileSetupIntent.putExtra("sender_id", senderId)
                                }
                                var userAsJson = Gson().toJson(user, User::class.java)
                                profileSetupIntent.putExtra("user_object", userAsJson)
                                startActivity(profileSetupIntent)

                            }
                            .addOnFailureListener(
                                this@AuthActivity
                            ) { e -> Log.w("DynamicLinks", "getDynamicLink:onFailure", e) }


                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Utils.instance.showAlert(
                        this@AuthActivity,
                        "User Query - Error",
                        error.message ?: ""
                    )

                }
            })

    }

    private fun minimumUserReqsCompleted(data: DataSnapshot): Boolean {
        if (!data.hasChild("display_name")) {
            return false
        }
        /*
        if (data.hasChild("birth_date")) {
            return false
        }
*/
        return true;

    }

    override fun onStart() {
        super.onStart()

    }


    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser == null) {
            binding.authLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
        } else {
            binding.authLayout.visibility = View.GONE
            binding.loadingLayout.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {

            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)



                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                    if (it.isSuccessful) {
                        var user = User()
                        user.email = account?.email ?: ""
                        user.provider = ProviderType.GOOGLE.name
                        showHome(user)
                    } else {

                        Utils.instance.showAlert(this, "Login Error", it?.exception?.message ?: "")
                    }


                }

            } catch (ex: ApiException) {
                Utils.instance.showAlert(this, "Login Error", ex.message ?: "")

            }

        } else
            if (requestCode == FACEBOOK_SIGN_IN) {
                // Pass the activity result back to the Facebook SDK
                callbackManager.onActivityResult(requestCode, resultCode, data); }
    }


}