package com.softwarefactory.chat.ynl.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


import java.util.ArrayList
import java.util.HashMap

import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.SharedPreferenceHelper
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Consersation
import com.softwarefactory.chat.ynl.model.Message
import de.hdodenhof.circleimageview.CircleImageView


class ChatActivity : AppCompatActivity(), View.OnClickListener {
    private var recyclerChat: RecyclerView? = null
    private var adapter: ListMessageAdapter? = null
    private var roomId: String? = null
    private var idFriend: ArrayList<CharSequence>? = null
    private var consersation: Consersation? = null
    private var btnSend: ImageButton? = null
    private var editWriteMessage: EditText? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    var bitmapAvataUser: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val intentData = intent
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID)
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID)
        val nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND)

        consersation = Consersation()
        btnSend = findViewById<View>(R.id.btnSend) as ImageButton
        btnSend!!.setOnClickListener(this)

        val base64AvataUser = SharedPreferenceHelper.getInstance(this).userInfo.avata
        if (base64AvataUser != StaticConfig.STR_DEFAULT_BASE64) {
            val decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT)
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } else {
            bitmapAvataUser = null
        }

        editWriteMessage = findViewById<View>(R.id.editWriteMessage) as EditText
        if (idFriend != null && nameFriend != null) {
            supportActionBar!!.setTitle(nameFriend)
            linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            recyclerChat = findViewById<View>(R.id.recyclerChat) as RecyclerView
            recyclerChat!!.layoutManager = linearLayoutManager
            adapter = bitmapAvataFriend?.let { ListMessageAdapter(this, consersation!!, it, bitmapAvataUser) }
            FirebaseDatabase.getInstance().reference.child("message/" + roomId!!)
                .addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                        if (dataSnapshot.value != null) {
                            val mapMessage = dataSnapshot.value as HashMap<*, *>?
                            val newMessage = Message()
                            newMessage.idSender = mapMessage!!["idSender"] as String?
                            newMessage.idReceiver = mapMessage["idReceiver"] as String?
                            newMessage.text = mapMessage["text"] as String?
                            newMessage.timestamp = mapMessage["timestamp"] as Long
                            consersation!!.listMessageData.add(newMessage)
                            adapter!!.notifyDataSetChanged()
                            linearLayoutManager!!.scrollToPosition(consersation!!.listMessageData.size - 1)
                        }
                    }

                    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                    }

                    override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                    }

                    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
            recyclerChat!!.adapter = adapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val result = Intent()
            result.putExtra("idFriend", idFriend!![0])
            setResult(RESULT_OK, result)
            this.finish()
        }
        return true
    }

    override fun onBackPressed() {
        val result = Intent()
        result.putExtra("idFriend", idFriend!![0])
        setResult(RESULT_OK, result)
        overridePendingTransition(R.anim.exit_to_bottom, R.anim.exit_to_bottom)
        this.finish()

    }

    override fun onClick(view: View) {
        if (view.id == R.id.btnSend) {
            val content = editWriteMessage!!.text.toString().trim { it <= ' ' }
            if (content.length > 0) {
                editWriteMessage!!.setText("")
                val newMessage = Message()
                newMessage.text = content
                newMessage.idSender = StaticConfig.UID
                newMessage.idReceiver = roomId
                newMessage.timestamp = System.currentTimeMillis()
                FirebaseDatabase.getInstance().reference.child("message/" + roomId!!).push().setValue(newMessage)
            }
        }
    }

    companion object {
        val VIEW_TYPE_USER_MESSAGE = 0
        val VIEW_TYPE_FRIEND_MESSAGE = 1
        var bitmapAvataFriend: HashMap<String, Bitmap>? = null
    }
}

internal class ListMessageAdapter(
    private val context: Context,
    private val consersation: Consersation,
    private val bitmapAvata: HashMap<String, Bitmap>,
    private val bitmapAvataUser: Bitmap?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val bitmapAvataDB: HashMap<String, DatabaseReference> = HashMap()

    override fun onCreateViewHolder( parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            val view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false)
            return ItemMessageFriendHolder(view)
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            val view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false)
            return ItemMessageUserHolder(view)
        }
        val view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false)
        return ItemMessageUserHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemMessageFriendHolder) {
            holder.txtContent.text = consersation.listMessageData[position].text
            val currentAvata = bitmapAvata[consersation.listMessageData[position].idSender]
            if (currentAvata != null) {
                holder.avata.setImageBitmap(currentAvata)
            } else {
                val id = consersation.listMessageData[position].idSender
                if (bitmapAvataDB[id] == null) {
                    bitmapAvataDB[id] == FirebaseDatabase.getInstance().reference.child("user/$id/avata")
                    bitmapAvataDB[id]?.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.value != null) {
                                val avataStr = dataSnapshot.value as String?
                                if (avataStr != StaticConfig.STR_DEFAULT_BASE64) {
                                    val decodedString = Base64.decode(avataStr, Base64.DEFAULT)
                                    ChatActivity.bitmapAvataFriend!![id] ==
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                } else {
                                    ChatActivity.bitmapAvataFriend!![id] ==
                                            BitmapFactory.decodeResource(context.resources, R.drawable.default_avata)
                                }
                                notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                }
            }
        } else if (holder is ItemMessageUserHolder) {
            holder.txtContent.text = consersation.listMessageData[position].text
            if (bitmapAvataUser != null) {
                holder.avata.setImageBitmap(bitmapAvataUser)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (consersation.listMessageData[position].idSender == StaticConfig.UID) ChatActivity.VIEW_TYPE_USER_MESSAGE else ChatActivity.VIEW_TYPE_FRIEND_MESSAGE
    }

    override fun getItemCount(): Int {
        return consersation.listMessageData.size
    }
}

internal class ItemMessageUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var txtContent: TextView = itemView.findViewById<View>(R.id.textContentUser) as TextView
    var avata: CircleImageView = itemView.findViewById<View>(R.id.imageView2) as CircleImageView

}

internal class ItemMessageFriendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var txtContent: TextView = itemView.findViewById<View>(R.id.textContentFriend) as TextView
    var avata: CircleImageView = itemView.findViewById<View>(R.id.imageView3) as CircleImageView

}
