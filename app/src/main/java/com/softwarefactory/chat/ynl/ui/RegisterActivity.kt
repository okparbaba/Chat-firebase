package com.softwarefactory.chat.ynl.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.Toast
import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.StaticConfig


import java.util.regex.Matcher
import java.util.regex.Pattern


class RegisterActivity : AppCompatActivity() {
    internal lateinit var fab: FloatingActionButton
    internal lateinit var cvAdd: CardView
    private val VALID_EMAIL_ADDRESS_REGEX =
        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)
    private var editTextUsername: EditText? = null
    private var editTextPassword: EditText? = null
    private var editTextRepeatPassword: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        cvAdd = findViewById<View>(R.id.cv_add) as CardView
        editTextUsername = findViewById<View>(R.id.et_username) as EditText
        editTextPassword = findViewById<View>(R.id.et_password) as EditText
        editTextRepeatPassword = findViewById<View>(R.id.et_repeatpassword) as EditText
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ShowEnterAnimation()
        }
        fab.setOnClickListener { animateRevealClose() }
    }

    private fun ShowEnterAnimation() {
        val transition = TransitionInflater.from(this).inflateTransition(R.transition.fabtransition)
        window.sharedElementEnterTransition = transition

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                cvAdd.visibility = View.GONE
            }

            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                animateRevealShow()
            }

            override fun onTransitionCancel(transition: Transition) {

            }

            override fun onTransitionPause(transition: Transition) {

            }

            override fun onTransitionResume(transition: Transition) {

            }


        })
    }

    fun animateRevealShow() {
        val mAnimator = ViewAnimationUtils.createCircularReveal(
            cvAdd,
            cvAdd.width / 2,
            0,
            (fab.width / 2).toFloat(),
            cvAdd.height.toFloat()
        )
        mAnimator.duration = 500
        mAnimator.interpolator = AccelerateInterpolator()
        mAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
            }

            override fun onAnimationStart(animation: Animator) {
                cvAdd.visibility = View.VISIBLE
                super.onAnimationStart(animation)
            }
        })
        mAnimator.start()
    }

    fun animateRevealClose() {
        val mAnimator = ViewAnimationUtils.createCircularReveal(
            cvAdd,
            cvAdd.width / 2,
            0,
            cvAdd.height.toFloat(),
            (fab.width / 2).toFloat()
        )
        mAnimator.duration = 500
        mAnimator.interpolator = AccelerateInterpolator()
        mAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                cvAdd.visibility = View.INVISIBLE
                super.onAnimationEnd(animation)
                fab.setImageResource(R.drawable.ic_signup)
                super@RegisterActivity.onBackPressed()
            }

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
            }
        })
        mAnimator.start()
    }

    override fun onBackPressed() {
        animateRevealClose()
    }

    fun clickRegister(view: View) {
        val username = editTextUsername!!.text.toString()
        val password = editTextPassword!!.text.toString()
        val repeatPassword = editTextRepeatPassword!!.text.toString()
        if (validate(username, password, repeatPassword)) {
            val data = Intent()
            data.putExtra(StaticConfig.STR_EXTRA_USERNAME, username)
            data.putExtra(StaticConfig.STR_EXTRA_PASSWORD, password)
            data.putExtra(StaticConfig.STR_EXTRA_ACTION, STR_EXTRA_ACTION_REGISTER)
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            Toast.makeText(this, "Invalid email or not match password", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validate email, pass == re_pass
     * @param emailStr
     * @param password
     * @return
     */
    private fun validate(emailStr: String, password: String, repeatPassword: String): Boolean {
        val matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr)
        return password.length > 0 && repeatPassword == password && matcher.find()
    }

    companion object {
        var STR_EXTRA_ACTION_REGISTER = "register"
    }
}
