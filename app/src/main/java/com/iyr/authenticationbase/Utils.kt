package com.iyr.authenticationbase

import android.app.AlertDialog
import android.content.Context


public class Utils {


    companion object {
        var instance = Utils()


    }

    fun getInstance(): Utils {
        if (Companion.instance == null)
            Companion.instance = Utils()
        return Companion.instance
    }

    fun showAlert(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog = builder.create()
        dialog.show()

    }


}