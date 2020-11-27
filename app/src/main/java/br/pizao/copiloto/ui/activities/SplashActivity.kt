package br.pizao.copiloto.ui.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.R
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Glide.with(this)
            .load(R.drawable.splash_gif)
            .into(findViewById(R.id.gif))
        val providers = listOf(AuthUI.IdpConfig.GoogleBuilder().build())
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    companion object {
        const val RC_SIGN_IN = 105
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser?.displayName?.let {
                    MainApplication.username = it
                }
                MainScope().launch {
                    delay(2000)
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }

            } else {
                Toast.makeText(
                    this,
                    "Não foi possível fazer a autenticação tente novamente",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}