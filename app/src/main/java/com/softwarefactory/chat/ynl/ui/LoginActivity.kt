package com.softwarefactory.chat.ynl.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.softwarefactory.chat.ynl.MainActivity
import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.SharedPreferenceHelper
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.User
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog

import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern


class LoginActivity : AppCompatActivity() {
    internal lateinit var fab: FloatingActionButton
    private val VALID_EMAIL_ADDRESS_REGEX =
        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)
    private var editTextUsername: EditText? = null
    private var editTextPassword: EditText? = null
    private var waitingDialog: LovelyProgressDialog? = null

    private var authUtils: AuthUtils? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var user: FirebaseUser? = null
    private var firstTimeAccess: Boolean = false

    override fun onStart() {
        super.onStart()
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        editTextUsername = findViewById<View>(R.id.et_username) as EditText
        editTextPassword = findViewById<View>(R.id.et_password) as EditText
        firstTimeAccess = true
        initFirebase()
    }


    /**
     * Khởi tạo các thành phần cần thiết cho việc quản lý đăng nhập
     */
    private fun initFirebase() {
        //Khoi tao thanh phan de dang nhap, dang ky
        mAuth = FirebaseAuth.getInstance()
        authUtils = AuthUtils()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                StaticConfig.UID = user!!.uid
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user!!.uid)
                if (firstTimeAccess) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    this@LoginActivity.finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            firstTimeAccess = false
        }

        //Khoi tao dialog waiting khi dang nhap
        waitingDialog = LovelyProgressDialog(this).setCancelable(false)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    fun clickRegisterLayout(view: View) {
        window.exitTransition = null
        window.enterTransition = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions.makeSceneTransitionAnimation(this, fab, fab.transitionName)
            startActivityForResult(
                Intent(this, RegisterActivity::class.java),
                StaticConfig.REQUEST_CODE_REGISTER,
                options.toBundle()
            )
        } else {
            startActivityForResult(Intent(this, RegisterActivity::class.java), StaticConfig.REQUEST_CODE_REGISTER)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StaticConfig.REQUEST_CODE_REGISTER && resultCode == Activity.RESULT_OK) {
            authUtils!!.createUser(
                data!!.getStringExtra(StaticConfig.STR_EXTRA_USERNAME),
                data.getStringExtra(StaticConfig.STR_EXTRA_PASSWORD)
            )
        }
    }

    fun clickLogin(view: View) {
        val username = editTextUsername!!.text.toString()
        val password = editTextPassword!!.text.toString()
        if (validate(username, password)) {
            authUtils!!.signIn(username, password)
        } else {
            Toast.makeText(this, "Invalid email or empty password", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED, null)
        finish()
    }

    private fun validate(emailStr: String, password: String): Boolean {
        val matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr)
        return (password.length > 0 || password == ";") && matcher.find()
    }

    fun clickResetPassword(view: View) {
        val username = editTextUsername!!.text.toString()
        if (validate(username, ";")) {
            authUtils!!.resetPassword(username)
        } else {
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dinh nghia cac ham tien ich cho quas trinhf dang nhap, dang ky,...
     */
    internal inner class AuthUtils {
        /**
         * Action register
         *
         * @param email
         * @param password
         */
        fun createUser(email: String, password: String) {
            waitingDialog!!.setIcon(R.drawable.ic_add_friend)
                .setTitle("Registering....")
                .setTopColorRes(R.color.colorPrimary)
                .show()
            mAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this@LoginActivity) { task ->
                    Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                    waitingDialog!!.dismiss()
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful) {
                        object : LovelyInfoDialog(this@LoginActivity) {
                            override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                                return super.setConfirmButtonText(text)
                            }
                        }
                            .setTopColorRes(R.color.colorAccent)
                            .setIcon(R.drawable.ic_add_friend)
                            .setTitle("Register false")
                            .setMessage("Email exist or weak password!")
                            .setConfirmButtonText("ok")
                            .setCancelable(false)
                            .show()
                    } else {
                        initNewUserInfo()
                        Toast.makeText(this@LoginActivity, "Register and Login success", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        this@LoginActivity.finish()
                    }
                }
                .addOnFailureListener { waitingDialog!!.dismiss() }
        }


        /**
         * Action Login
         *
         * @param email
         * @param password
         */
        fun signIn(email: String, password: String) {
            waitingDialog!!.setIcon(R.drawable.ic_person_low)
                .setTitle("Login....")
                .setTopColorRes(R.color.colorPrimary)
                .show()
            mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this@LoginActivity) { task ->
                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful)
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    waitingDialog!!.dismiss()
                    if (!task.isSuccessful) {
                        Log.w(TAG, "signInWithEmail:failed", task.exception)
                        object : LovelyInfoDialog(this@LoginActivity) {
                            override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                                return super.setConfirmButtonText(text)
                            }
                        }
                            .setTopColorRes(R.color.colorAccent)
                            .setIcon(R.drawable.ic_person_low)
                            .setTitle("Login false")
                            .setMessage("Email not exist or wrong password!")
                            .setCancelable(false)
                            .setConfirmButtonText("Ok")
                            .show()
                    } else {
                        saveUserInfo()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        this@LoginActivity.finish()
                    }
                }
                .addOnFailureListener { waitingDialog!!.dismiss() }
        }

        /**
         * Action reset password
         *
         * @param email
         */
        fun resetPassword(email: String) {
            mAuth!!.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    object : LovelyInfoDialog(this@LoginActivity) {
                        override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                            findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                            return super.setConfirmButtonText(text)
                        }
                    }
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_pass_reset)
                        .setTitle("Password Recovery")
                        .setMessage("Sent email to $email")
                        .setConfirmButtonText("Ok")
                        .show()
                }
                .addOnFailureListener {
                    object : LovelyInfoDialog(this@LoginActivity) {
                        override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                            findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                            return super.setConfirmButtonText(text)
                        }
                    }
                        .setTopColorRes(R.color.colorAccent)
                        .setIcon(R.drawable.ic_pass_reset)
                        .setTitle("False")
                        .setMessage("False to sent email to $email")
                        .setConfirmButtonText("Ok")
                        .show()
                }
        }

        /**
         * Luu thong tin user info cho nguoi dung dang nhap
         */
        fun saveUserInfo() {
            FirebaseDatabase.getInstance().reference.child("user/" + StaticConfig.UID)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        waitingDialog!!.dismiss()
                        val hashUser = dataSnapshot.value as HashMap<*, *>?
                        val userInfo = User()
                        userInfo.name = hashUser!!["name"] as String?
                        userInfo.email = hashUser["email"] as String?
                        userInfo.avata = hashUser["avata"] as String?
                        SharedPreferenceHelper.getInstance(this@LoginActivity).saveUserInfo(userInfo)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        }

        /**
         * Khoi tao thong tin mac dinh cho tai khoan moi
         */
        fun initNewUserInfo() {
            val newUser = User()
            newUser.email = user!!.email
            newUser.name = user!!.email!!.substring(0, user!!.email!!.indexOf("@"))
            newUser.avata = StaticConfig.STR_DEFAULT_BASE64
            FirebaseDatabase.getInstance().reference.child("user/" + user!!.uid).setValue(newUser)
        }
    }

    companion object {
        private val TAG = "LoginActivity"
    }
}
