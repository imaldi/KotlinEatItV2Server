package com.aim2u.kotlineatitv2server

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.aim2u.kotlineatitv2server.common.Common
import com.aim2u.kotlineatitv2server.model.ServerUserModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import dmax.dialog.SpotsDialog
import java.util.*

class MainActivity : AppCompatActivity() {
    private var firebaseAuth:FirebaseAuth? = null
    private var listener:FirebaseAuth.AuthStateListener? = null
    private var dialog: AlertDialog?=null
    private var serverRef:DatabaseReference?=null
    private var providers : List<AuthUI.IdpConfig>?=null

    companion object{
        private val APP_REQUEST_CODE = 7171
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth?.addAuthStateListener(listener!!)
    }

    override fun onStop() {
        firebaseAuth?.removeAuthStateListener(listener!!)
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        init()
    }

    private fun init() {
        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build())

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener = object : FirebaseAuth.AuthStateListener{
            override fun onAuthStateChanged(p0: FirebaseAuth) {
                val user = firebaseAuth?.currentUser
                if(user != null){
                    checkServerUserFromFirebase(user)
                } else {
                    phoneLogin()
                }
            }

        }
    }

    private fun checkServerUserFromFirebase(user: FirebaseUser) {
        dialog?.show()
        serverRef!!.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    dialog?.dismiss()
                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()){
                        val userModel = dataSnapshot.getValue(ServerUserModel::class.java)
                        if(userModel!!.isActive!!){
                            goToHomeActivity(userModel)
                        } else {
                            dialog?.dismiss()
                            Toast.makeText(this@MainActivity,"You must be allowed by Admin to access this app",Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        dialog?.dismiss()
                        showRegisterDialog(user)
                    }
                }
            })
    }

    private fun goToHomeActivity(userModel: ServerUserModel) {
        dialog?.dismiss()
        Common.currentServerUser = userModel
        startActivity(Intent(this,HomeActivity::class.java))
        finish()
    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Register")
        builder.setMessage("Please fill information \n Admin will accept your account late")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)
        val edt_name = itemView.findViewById<View>(R.id.edt_name) as EditText
        val edt_phone = itemView.findViewById<View>(R.id.edt_phone) as EditText

        //Set Date
        edt_phone.setText(user!!.phoneNumber)

        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("REGISTER") { _, _ ->
                if(TextUtils.isEmpty(edt_name.text)){
                    Toast.makeText(this,"Please enter your name",Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val serverUserModel = ServerUserModel()
                serverUserModel.uid = user.uid
                serverUserModel.name = edt_name.text.toString()
                serverUserModel.phone = edt_phone.text.toString()
                serverUserModel.isActive = false // Default fail

                dialog?.show()
                serverRef!!.child(serverUserModel.uid!!)
                    .setValue(serverUserModel)
                    .addOnFailureListener { e ->
                        dialog?.dismiss()
                        Toast.makeText(this,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener { _ ->
                        dialog?.dismiss()
                        Toast.makeText(this,"Register success ! Admin will check and activate user soon",Toast.LENGTH_SHORT).show()
                    }
            }

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }

    private fun phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers!!)
            .build(),APP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == APP_REQUEST_CODE){

            if(requestCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else{
                Toast.makeText(this,"Failed to sign in",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
