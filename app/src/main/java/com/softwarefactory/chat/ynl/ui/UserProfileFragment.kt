package com.softwarefactory.chat.ynl.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.FriendDB
import com.softwarefactory.chat.ynl.data.GroupDB
import com.softwarefactory.chat.ynl.data.SharedPreferenceHelper
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Configuration
import com.softwarefactory.chat.ynl.model.User
import com.softwarefactory.chat.ynl.service.ServiceUtils
import com.softwarefactory.chat.ynl.util.ImageUtils
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog
import java.io.FileNotFoundException
import java.util.*


class UserProfileFragment : Fragment() {
    internal var tvUserName: TextView? = null
    internal lateinit var avatar: ImageView

    private val listConfig = ArrayList<Configuration>()
    private var recyclerView: RecyclerView? = null
    private var infoAdapter: UserInfoAdapter? = null
    private var waitingDialog: LovelyProgressDialog? = null

    private var userDB: DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    private var myAccount: User? = null

    @SuppressLint("StaticFieldLeak")
    internal var context: Context? = null



    private val userListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            //Lấy thông tin của user về và cập nhật lên giao diện
            listConfig.clear()
            myAccount = dataSnapshot.getValue(User::class.java)

            setupArrayListInfo(myAccount)
            if (infoAdapter != null) {
                infoAdapter!!.notifyDataSetChanged()
            }

            if (tvUserName != null) {
                tvUserName!!.text = myAccount!!.name
            }

            setImageAvatar(context, myAccount!!.avata)
            val preferenceHelper = SharedPreferenceHelper.getInstance(context!!)
            preferenceHelper.saveUserInfo(myAccount!!)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e(UserProfileFragment::class.java.name, "loadPost:onCancelled", databaseError.toException())
        }
    }

    /**
     * Khi click vào avatar thì bắn intent mở trình xem ảnh mặc định để chọn ảnh
     */
    private val onAvatarClick = View.OnClickListener {
        AlertDialog.Builder(context!!)
            .setTitle("Avatar")
            .setMessage("Are you sure want to change avatar profile?")
            .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_PICK
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
                dialogInterface.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userDB = FirebaseDatabase.getInstance().reference.child("user").child(StaticConfig.UID)
        userDB!!.addListenerForSingleValueEvent(userListener)
        mAuth = FirebaseAuth.getInstance()

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        context = view.context
        avatar = view.findViewById<View>(R.id.img_avatar) as ImageView
        avatar.setOnClickListener(onAvatarClick)
        tvUserName = view.findViewById<View>(R.id.tv_username) as TextView

        val prefHelper = SharedPreferenceHelper.getInstance(context!!)
        myAccount = prefHelper.userInfo
        setupArrayListInfo(myAccount)
        setImageAvatar(context, myAccount!!.avata)
        tvUserName!!.text = myAccount!!.name

        recyclerView = view.findViewById<View>(R.id.info_recycler_view) as RecyclerView
        infoAdapter = UserInfoAdapter(listConfig)
        val layoutManager = LinearLayoutManager(view.context)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.adapter = infoAdapter

        waitingDialog = LovelyProgressDialog(context)
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(context, "", Toast.LENGTH_LONG).show()
                return
            }
            try {
                val inputStream = context!!.contentResolver.openInputStream(data.data!!)

                var imgBitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
                imgBitmap = ImageUtils.cropToSquare(imgBitmap!!)
                val `is` = ImageUtils.convertBitmapToInputStream(imgBitmap!!)
                val liteImage = ImageUtils.makeImageLite(
                    `is`,
                    imgBitmap.width, imgBitmap.height,
                    ImageUtils.AVATAR_WIDTH, ImageUtils.AVATAR_HEIGHT
                )

                val imageBase64 = ImageUtils.encodeBase64(liteImage!!)
                myAccount!!.avata = imageBase64

                waitingDialog!!.setCancelable(false)
                    .setTitle("Avatar updating....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()

                userDB!!.child("avata").setValue(imageBase64)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                            waitingDialog!!.dismiss()
                            val preferenceHelper = SharedPreferenceHelper.getInstance(context!!)
                            preferenceHelper.saveUserInfo(myAccount!!)
                            avatar.setImageDrawable(ImageUtils.roundedImage(context!!, liteImage))

                            LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorPrimary)
                                .setTitle("Success")
                                .setMessage("Update avatar successfully!")
                                .show()
                        }
                    }
                    .addOnFailureListener {
                        waitingDialog!!.dismiss()
                        Log.d("Update Avatar", "failed")
                        LovelyInfoDialog(context)
                            .setTopColorRes(R.color.colorAccent)
                            .setTitle("False")
                            .setMessage("False to update avatar")
                            .show()
                    }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Xóa list cũ và cập nhật lại list data mới
     * @param myAccount
     */
    fun setupArrayListInfo(myAccount: User?) {
        listConfig.clear()
        val userNameConfig = myAccount?.name?.let { Configuration(USERNAME_LABEL, it, R.mipmap.ic_account_box) }
        userNameConfig?.let { listConfig.add(it) }

        val emailConfig = Configuration(EMAIL_LABEL, myAccount?.email!!, R.mipmap.ic_email)
        listConfig.add(emailConfig)

        val resetPass = Configuration(RESETPASS_LABEL, "", R.mipmap.ic_restore)
        listConfig.add(resetPass)

        val signout = Configuration(SIGNOUT_LABEL, "", R.mipmap.ic_power_settings)
        listConfig.add(signout)
    }

    private fun setImageAvatar(context: Context?, imgBase64: String?) {
        try {
            val res = resources
            val src: Bitmap
            if (imgBase64 == "default") {
                src = BitmapFactory.decodeResource(res, R.drawable.default_avata)
            } else {
                val decodedString = Base64.decode(imgBase64, Base64.DEFAULT)
                src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            }

            avatar.setImageDrawable(ImageUtils.roundedImage(context!!, src))
        } catch (e: Exception) {
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class UserInfoAdapter(private val profileConfig: List<Configuration>) :
        RecyclerView.Adapter<UserInfoAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_info_item_layout, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val config = profileConfig[position]
            holder.label.text = config.label
            holder.value.text = config.value
            holder.icon.setImageResource(config.icon)
            (holder.label.parent as RelativeLayout).setOnClickListener {
                if (config.label == SIGNOUT_LABEL) {
                    FirebaseAuth.getInstance().signOut()
                    getContext()?.let { it1 -> FriendDB.getInstance(it1).dropDB() }
                    getContext()?.let { it1 -> GroupDB.getInstance(it1).dropDB() }
                    ServiceUtils.stopServiceFriendChat(getContext()!!.applicationContext, true)
                    activity!!.finish()
                }

                if (config.label == USERNAME_LABEL) {
                    val vewInflater = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_edit_username, view as ViewGroup?, false)
                    val input = vewInflater.findViewById<View>(R.id.edit_username) as EditText
                    input.setText(myAccount!!.name)
                    AlertDialog.Builder(context!!)
                        .setTitle("Edit username")
                        .setView(vewInflater)
                        .setPositiveButton("Save") { dialogInterface, i ->
                            val newName = input.text.toString()
                            if (myAccount!!.name != newName) {
                                changeUserName(newName)
                            }
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, i -> dialogInterface.dismiss() }.show()
                }

                if (config.label == RESETPASS_LABEL) {
                    AlertDialog.Builder(context!!)
                        .setTitle("Password")
                        .setMessage("Are you sure want to reset password?")
                        .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                            resetPassword(myAccount!!.email)
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }
                        .show()
                }
            }
        }

        private fun changeUserName(newName: String) {
            userDB!!.child("name").setValue(newName)


            myAccount!!.name = newName
            val prefHelper = SharedPreferenceHelper.getInstance(context!!)
            prefHelper.saveUserInfo(myAccount!!)

            tvUserName!!.text = newName
            setupArrayListInfo(myAccount)
        }

        internal fun resetPassword(email: String?) {
            mAuth!!.sendPasswordResetEmail(email!!)
                .addOnCompleteListener {
                    object : LovelyInfoDialog(context) {
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
                    object : LovelyInfoDialog(context) {
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

        override fun getItemCount(): Int {
            return profileConfig.size
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            // each data item is just a string in this case
            var label: TextView
            var value: TextView
            var icon: ImageView

            init {
                label = view.findViewById<View>(R.id.tv_title) as TextView
                value = view.findViewById<View>(R.id.tv_detail) as TextView
                icon = view.findViewById<View>(R.id.img_icon) as ImageView
            }
        }

    }

    companion object {

        private val USERNAME_LABEL = "Username"
        private val EMAIL_LABEL = "Email"
        private val SIGNOUT_LABEL = "Sign out"
        private val RESETPASS_LABEL = "Change Password"
        private val PICK_IMAGE = 1996
    }

}
