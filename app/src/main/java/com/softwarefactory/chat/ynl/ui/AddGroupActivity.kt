package com.softwarefactory.chat.ynl.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase

import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.FriendDB
import com.softwarefactory.chat.ynl.data.GroupDB
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Group
import com.softwarefactory.chat.ynl.model.ListFriend
import com.softwarefactory.chat.ynl.model.Room
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog

import java.util.HashSet

import de.hdodenhof.circleimageview.CircleImageView


class AddGroupActivity : AppCompatActivity() {

    private var recyclerListFriend: RecyclerView? = null
    private var adapter: ListPeopleAdapter? = null
    private var listFriend: ListFriend? = null
    private var btnAddGroup: LinearLayout? = null
    private var listIDChoose: MutableSet<String>? = null
    private var listIDRemove: MutableSet<String>? = null
    private var editTextGroupName: EditText? = null
    private var txtGroupIcon: TextView? = null
    private var txtActionName: TextView? = null
    private var dialogWait: LovelyProgressDialog? = null
    private var isEditGroup: Boolean = false
    private var groupEdit: Group? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_group)

        val intentData = intent
        txtActionName = findViewById<View>(R.id.txtActionName) as TextView
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listFriend = FriendDB.getInstance(this).listFriend
        listIDChoose = HashSet()
        listIDRemove = HashSet()
        listIDChoose!!.add(StaticConfig.UID)
        btnAddGroup = findViewById<View>(R.id.btnAddGroup) as LinearLayout
        editTextGroupName = findViewById<View>(R.id.editGroupName) as EditText
        txtGroupIcon = findViewById<View>(R.id.icon_group) as TextView
        dialogWait = LovelyProgressDialog(this).setCancelable(false)
        editTextGroupName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.length >= 1) {
                    txtGroupIcon!!.text = (charSequence[0] + "").toUpperCase()
                } else {
                    txtGroupIcon!!.text = "R"
                }
            }

            override fun afterTextChanged(editable: Editable) {

            }
        })

        btnAddGroup!!.setOnClickListener {
            if (listIDChoose!!.size < 3) {
                Toast.makeText(this@AddGroupActivity, resources.getString(R.string.add_a_lease), Toast.LENGTH_SHORT)
                    .show()
            } else {
                if (editTextGroupName!!.text.length == 0) {
                    Toast.makeText(
                        this@AddGroupActivity,
                        resources.getString(R.string.enter_group_name),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (isEditGroup) {
                        editGroup()
                    } else {
                        createGroup()
                    }
                }
            }
        }

        if (intentData.getStringExtra("groupId") != null) {
            isEditGroup = true
            val idGroup = intentData.getStringExtra("groupId")
            txtActionName!!.text = resources.getString(R.string.save)
            btnAddGroup!!.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            groupEdit = GroupDB.getInstance(this).getGroup(idGroup)
            editTextGroupName!!.setText(groupEdit!!.groupInfo["name"])
        } else {
            isEditGroup = false
        }

        recyclerListFriend = findViewById<View>(R.id.recycleListFriend) as RecyclerView
        recyclerListFriend!!.layoutManager = linearLayoutManager
        adapter = ListPeopleAdapter(this, listFriend!!,
            btnAddGroup!!, listIDChoose as HashSet<String>, listIDRemove as HashSet<String>, isEditGroup, groupEdit)
        recyclerListFriend!!.adapter = adapter


    }

    private fun editGroup() {
        //Show dialog wait
        dialogWait!!.setIcon(R.drawable.ic_add_group_dialog)
            .setTitle(resources.getString(R.string.editing))
            .setTopColorRes(R.color.colorPrimary)
            .show()
        //Delete group
        val idGroup = groupEdit!!.id
        val room = Room()
        for (id in listIDChoose!!) {
            room.member.add(id)
        }
        room.groupInfo["name"] == editTextGroupName!!.text.toString()
        room.groupInfo["admin"] == StaticConfig.UID
        FirebaseDatabase.getInstance().reference.child("group/" + idGroup!!).setValue(room)
            .addOnCompleteListener { addRoomForUser(idGroup, 0) }
            .addOnFailureListener {
                dialogWait!!.dismiss()
                object : LovelyInfoDialog(this@AddGroupActivity) {
                    override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                        findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                        return super.setConfirmButtonText(text)
                    }
                }
                    .setTopColorRes(R.color.colorAccent)
                    .setIcon(R.drawable.ic_add_group_dialog)
                    .setTitle(resources.getString(R.string.fal))
                    .setMessage(resources.getString(R.string.cannot_connect_database))
                    .setCancelable(false)
                    .setConfirmButtonText(resources.getString(R.string.ok))
                    .show()
            }
    }

    private fun createGroup() {
        //Show dialog wait
        dialogWait!!.setIcon(R.drawable.ic_add_group_dialog)
            .setTitle(resources.getString(R.string.registering))
            .setTopColorRes(R.color.colorPrimary)
            .show()

        val idGroup = (StaticConfig.UID + System.currentTimeMillis()).hashCode().toString() + ""
        val room = Room()
        for (id in listIDChoose!!) {
            room.member.add(id)
        }
        room.groupInfo["name"] == editTextGroupName!!.text.toString()
        room.groupInfo["admin"] == StaticConfig.UID
        FirebaseDatabase.getInstance().reference.child("group/$idGroup").setValue(room)
            .addOnCompleteListener { addRoomForUser(idGroup, 0) }
    }

    private fun deleteRoomForUser(roomId: String?, userIndex: Int) {
        if (userIndex == listIDRemove!!.size) {
            dialogWait!!.dismiss()
            Toast.makeText(this, resources.getString(R.string.edit_group_success), Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, null)
            this@AddGroupActivity.finish()
        } else {
            FirebaseDatabase.getInstance()
                .reference.child("user/" + listIDRemove!!.toTypedArray()[userIndex] + "/group/" + roomId).removeValue()
                .addOnCompleteListener { deleteRoomForUser(roomId, userIndex + 1) }
                .addOnFailureListener {
                    dialogWait!!.dismiss()
                    object : LovelyInfoDialog(this@AddGroupActivity) {
                        override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                            findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                            return super.setConfirmButtonText(text)
                        }
                    }
                        .setTopColorRes(R.color.colorAccent)
                        .setIcon(R.drawable.ic_add_group_dialog)
                        .setTitle(resources.getString(R.string.fal))
                        .setMessage(resources.getString(R.string.cannot_connect_database))
                        .setCancelable(false)
                        .setConfirmButtonText(resources.getString(R.string.ok))
                        .show()
                }
        }
    }

    private fun addRoomForUser(roomId: String?, userIndex: Int) {
        if (userIndex == listIDChoose!!.size) {
            if (!isEditGroup) {
                dialogWait!!.dismiss()
                Toast.makeText(this, resources.getString(R.string.create_group_success), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK, null)
                this@AddGroupActivity.finish()
            } else {
                deleteRoomForUser(roomId, 0)
            }
        } else {
            FirebaseDatabase.getInstance()
                .reference.child("user/" + listIDChoose!!.toTypedArray()[userIndex] + "/group/" + roomId)
                .setValue(roomId).addOnCompleteListener { addRoomForUser(roomId, userIndex + 1) }.addOnFailureListener {
                dialogWait!!.dismiss()
                object : LovelyInfoDialog(this@AddGroupActivity) {
                    override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                        findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                        return super.setConfirmButtonText(text)
                    }
                }
                    .setTopColorRes(R.color.colorAccent)
                    .setIcon(R.drawable.ic_add_group_dialog)
                    .setTitle(resources.getString(R.string.fal))
                    .setMessage(resources.getString(R.string.create_group_false))
                    .setCancelable(false)
                    .setConfirmButtonText(resources.getString(R.string.ok))
                    .show()
            }
        }
    }
}

internal class ListPeopleAdapter(
    private val context: Context,
    private val listFriend: ListFriend,
    private val btnAddGroup: LinearLayout,
    private val listIDChoose: MutableSet<String>,
    private val listIDRemove: MutableSet<String>,
    private val isEdit: Boolean,
    private val editGroup: Group?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.rc_item_add_friend, parent, false)
        return ItemFriendHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ItemFriendHolder).txtName.text = listFriend.listFriend!![position].name
        holder.txtEmail.text = listFriend.listFriend!![position].email
        val avata = listFriend.listFriend!![position].avata
        val id = listFriend.listFriend!![position].id
        if (avata != StaticConfig.STR_DEFAULT_BASE64) {
            val decodedString = Base64.decode(avata, Base64.DEFAULT)
            holder.avata.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size))
        } else {
            holder.avata.setImageBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.default_avata))
        }
        holder.checkBox.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                if (id != null) {
                    listIDChoose.add(id)
                }
                listIDRemove.remove(id)
            } else {
                if (id != null) {
                    listIDRemove.add(id)
                }
                listIDChoose.remove(id)
            }
            if (listIDChoose.size >= 3) {
                btnAddGroup.setBackgroundColor(context.resources.getColor(R.color.colorPrimary))
            } else {
                btnAddGroup.setBackgroundColor(context.resources.getColor(R.color.grey_500))
            }
        }
        if (isEdit && editGroup!!.member.contains(id)) {
            holder.checkBox.isChecked = true
        } else if (editGroup != null && !editGroup.member.contains(id)) {
            holder.checkBox.isChecked = false
        }
    }

    override fun getItemCount(): Int {
        return listFriend.listFriend!!.size
    }
}

internal class ItemFriendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var txtName: TextView
    var txtEmail: TextView
    var avata: CircleImageView
    var checkBox: CheckBox

    init {
        txtName = itemView.findViewById<View>(R.id.txtName) as TextView
        txtEmail = itemView.findViewById<View>(R.id.txtEmail) as TextView
        avata = itemView.findViewById<View>(R.id.icon_avata) as CircleImageView
        checkBox = itemView.findViewById<View>(R.id.checkAddPeople) as CheckBox
    }
}

