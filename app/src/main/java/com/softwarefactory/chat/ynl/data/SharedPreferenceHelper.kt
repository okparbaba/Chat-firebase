package com.softwarefactory.chat.ynl.data

import android.content.Context
import android.content.SharedPreferences
import com.softwarefactory.chat.ynl.model.User


class SharedPreferenceHelper private constructor() {

    val userInfo: User
        get() {
            val userName = preferences!!.getString(SHARE_KEY_NAME, "")
            val email = preferences!!.getString(SHARE_KEY_EMAIL, "")
            val avatar = preferences!!.getString(SHARE_KEY_AVATA, "default")

            val user = User()
            user.name = userName
            user.email = email
            user.avata = avatar

            return user
        }

    val uid: String?
        get() = preferences!!.getString(SHARE_KEY_UID, "")

    fun saveUserInfo(user: User) {
        editor!!.putString(SHARE_KEY_NAME, user.name)
        editor!!.putString(SHARE_KEY_EMAIL, user.email)
        editor!!.putString(SHARE_KEY_AVATA, user.avata)
        editor!!.putString(SHARE_KEY_UID, StaticConfig.UID)
        editor!!.apply()
    }

    companion object {
        private var instance: SharedPreferenceHelper? = null
        private var preferences: SharedPreferences? = null
        private var editor: SharedPreferences.Editor? = null
        private val SHARE_USER_INFO = "userinfo"
        private val SHARE_KEY_NAME = "name"
        private val SHARE_KEY_EMAIL = "email"
        private val SHARE_KEY_AVATA = "avata"
        private val SHARE_KEY_UID = "uid"

        fun getInstance(context: Context): SharedPreferenceHelper {
            if (instance == null) {
                instance = SharedPreferenceHelper()
                preferences = context.getSharedPreferences(SHARE_USER_INFO, Context.MODE_PRIVATE)
                editor = preferences!!.edit()
            }
            return instance as SharedPreferenceHelper
        }
    }

}
