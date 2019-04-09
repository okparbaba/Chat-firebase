package com.softwarefactory.chat.ynl.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.FriendDB
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Friend
import com.softwarefactory.chat.ynl.model.ListFriend
import com.softwarefactory.chat.ynl.service.ServiceUtils
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog
import com.yarolegovich.lovelydialog.LovelyTextInputDialog

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

import de.hdodenhof.circleimageview.CircleImageView

class FriendsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var recyclerListFrends: RecyclerView? = null
    private var adapter: ListFriendsAdapter? = null
    var onClickFloatButton: FragFriendClickFloatButton
    private var dataListFriend: ListFriend? = null
    private var listFriendID: ArrayList<String>? = null
    private var dialogFindAllFriend: LovelyProgressDialog? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var detectFriendOnline: CountDownTimer? = null

    private var deleteFriendReceiver: BroadcastReceiver? = null

    init {
        onClickFloatButton = FragFriendClickFloatButton()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        detectFriendOnline = object : CountDownTimer(System.currentTimeMillis(), StaticConfig.TIME_TO_REFRESH) {
            override fun onTick(l: Long) {
                context?.let { dataListFriend?.let { it1 -> ServiceUtils.updateFriendStatus(it, it1) } }
                context?.let { ServiceUtils.updateUserStatus(it) }
            }

            override fun onFinish() {

            }
        }
        if (dataListFriend == null) {
            dataListFriend = context?.let { FriendDB.getInstance(it).listFriend }
            if (dataListFriend!!.listFriend!!.size > 0) {
                listFriendID = ArrayList()
                for (friend in dataListFriend!!.listFriend!!) {
                    listFriendID!!.add(friend.id!!)
                }
                detectFriendOnline!!.start()
            }
        }
        val layout = inflater.inflate(R.layout.fragment_people, container, false)
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerListFrends = layout.findViewById<View>(R.id.recycleListFriend) as RecyclerView
        recyclerListFrends!!.layoutManager = linearLayoutManager
        mSwipeRefreshLayout = layout.findViewById<View>(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        mSwipeRefreshLayout!!.setOnRefreshListener(this)
        adapter = context?.let { ListFriendsAdapter(it, dataListFriend!!, this) }
        recyclerListFrends!!.adapter = adapter
        dialogFindAllFriend = LovelyProgressDialog(context)
        if (listFriendID == null) {
            listFriendID = ArrayList()
            dialogFindAllFriend!!.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(resources.getString(R.string.get_all_friends))
                .setTopColorRes(R.color.colorPrimary)
                .show()
            getListFriendUId()
        }

        deleteFriendReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val idDeleted = intent.extras!!.getString("idFriend")
                for (friend in dataListFriend!!.listFriend!!) {
                    if (idDeleted == friend.id) {
                        val friends = dataListFriend!!.listFriend
                        friends!!.remove(friend)
                        break
                    }
                }
                adapter!!.notifyDataSetChanged()
            }
        }

        val intentFilter = IntentFilter(ACTION_DELETE_FRIEND)
        context!!.registerReceiver(deleteFriendReceiver, intentFilter)

        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context!!.unregisterReceiver(deleteFriendReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (ACTION_START_CHAT == requestCode && data != null && ListFriendsAdapter.mapMark != null) {
            ListFriendsAdapter.mapMark!![data.getStringExtra("idFriend")] = false
        }
    }

    override fun onRefresh() {
        listFriendID!!.clear()
        dataListFriend!!.listFriend!!.clear()
        adapter!!.notifyDataSetChanged()
        context?.let { FriendDB.getInstance(it).dropDB() }
        detectFriendOnline!!.cancel()
        getListFriendUId()
    }

    inner class FragFriendClickFloatButton : View.OnClickListener {
        internal lateinit var context: Context
        internal lateinit var dialogWait: LovelyProgressDialog

        fun getInstance(context: Context): FragFriendClickFloatButton {
            this.context = context
            dialogWait = LovelyProgressDialog(context)
            return this
        }

        override fun onClick(view: View) {
            LovelyTextInputDialog(view.context, R.style.EditTextTintTheme)
                .setTopColorRes(R.color.colorPrimary)
                .setTitle(resources.getString(R.string.get_friend))
                .setMessage(resources.getString(R.string.enter_friend_email))
                .setIcon(R.drawable.ic_add_friend)
                .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .setInputFilter(resources.getString(R.string.email_not_found)) { text ->
                    val VALID_EMAIL_ADDRESS_REGEX =
                        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)
                    val matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text)
                    matcher.find()
                }
                .setConfirmButton(android.R.string.ok) { text ->
                    //Tim id user id
                    findIDEmail(text)
                    //Check xem da ton tai ban ghi friend chua
                    //Ghi them 1 ban ghi
                }
                .show()
        }

        /**
         * TIm id cua email tren server
         *
         * @param email
         */
        private fun findIDEmail(email: String) {
            dialogWait.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(resources.getString(R.string.finding_friend))
                .setTopColorRes(R.color.colorPrimary)
                .show()
            FirebaseDatabase.getInstance().reference.child("user").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        dialogWait.dismiss()
                        if (dataSnapshot.value == null) {
                            //email not found
                            LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle(resources.getString(R.string.fail))
                                .setMessage(resources.getString(R.string.email_not_found))
                                .show()
                        } else {
                            val id = (dataSnapshot.value as HashMap<*, *>).keys.iterator().next().toString()
                            if (id == StaticConfig.UID) {
                                LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle(resources.getString(R.string.fail))
                                    .setMessage(resources.getString(R.string.email_not_valid))
                                    .show()
                            } else {
                                val userMap = (dataSnapshot.value as HashMap<*, *>)[id] as HashMap<*, *>?
                                val user = Friend()
                                user.name = userMap!!["name"] as String?
                                user.email = userMap["email"] as String?
                                user.avata = userMap["avata"] as String?
                                user.id = id
                                user.idRoom =
                                    if (id.compareTo(StaticConfig.UID) > 0) (StaticConfig.UID + id).hashCode().toString() + "" else "" + (id + StaticConfig.UID).hashCode()
                                checkBeforAddFriend(id, user)
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        }


        private fun checkBeforAddFriend(idFriend: String, userInfo: Friend) {
            dialogWait.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(resources.getString(R.string.add_friend))
                .setTopColorRes(R.color.colorPrimary)
                .show()

            //Check xem da ton tai id trong danh sach id chua
            if (listFriendID!!.contains(idFriend)) {
                dialogWait.dismiss()
                LovelyInfoDialog(context)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(resources.getString(R.string.firend))
                    .setMessage(resources.getString(R.string.user) + userInfo.email + resources.getString(R.string.has_been_friend))
                    .show()
            } else {
                addFriend(idFriend, true)
                listFriendID!!.add(idFriend)
                dataListFriend!!.listFriend!!.add(userInfo)
                getContext()?.let { FriendDB.getInstance(it).addFriend(userInfo) }
                adapter!!.notifyDataSetChanged()
            }
        }

        /**
         * Add friend
         *
         * @param idFriend
         */
        private fun addFriend(idFriend: String?, isIdFriend: Boolean) {
            if (idFriend != null) {
                if (isIdFriend) {
                    FirebaseDatabase.getInstance().reference.child("friend/" + StaticConfig.UID).push()
                        .setValue(idFriend)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                addFriend(idFriend, false)
                            }
                        }
                        .addOnFailureListener {
                            dialogWait.dismiss()
                            LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle(resources.getString(R.string.fal))
                                .setMessage(resources.getString(R.string.false_to_add_fri))
                                .show()
                        }
                } else {
                    FirebaseDatabase.getInstance().reference.child("friend/$idFriend").push().setValue(StaticConfig.UID)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                addFriend(null, false)
                            }
                        }
                        .addOnFailureListener {
                            dialogWait.dismiss()
                            LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle(resources.getString(R.string.fal))
                                .setMessage(resources.getString(R.string.fal))
                                .show()
                        }
                }
            } else {
                dialogWait.dismiss()
                LovelyInfoDialog(context)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(resources.getString(R.string.success))
                    .setMessage(resources.getString(R.string.add_fri_success))
                    .show()
            }
        }


    }


    private fun getListFriendUId() {
        FirebaseDatabase.getInstance().reference.child("friend/" + StaticConfig.UID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {
                        val mapRecord = dataSnapshot.value as HashMap<*, *>?
                        val listKey = mapRecord!!.keys.iterator()
                        while (listKey.hasNext()) {
                            val key = listKey.next().toString()
                            listFriendID!!.add(mapRecord[key]!!.toString())
                        }
                        getAllFriendInfo(0)
                    } else {
                        dialogFindAllFriend!!.dismiss()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }


    private fun getAllFriendInfo(index: Int) {
        if (index == listFriendID!!.size) {
            //save list friend
            adapter!!.notifyDataSetChanged()
            dialogFindAllFriend!!.dismiss()
            mSwipeRefreshLayout!!.isRefreshing = false
            detectFriendOnline!!.start()
        } else {
            val id = listFriendID!![index]
            FirebaseDatabase.getInstance().reference.child("user/$id")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {
                            val user = Friend()
                            val mapUserInfo = dataSnapshot.value as HashMap<*, *>?
                            user.name = mapUserInfo!!["name"] as String?
                            user.email = mapUserInfo["email"] as String?
                            user.avata = mapUserInfo["avata"] as String?
                            user.id = id
                            user.idRoom =
                                if (id.compareTo(StaticConfig.UID) > 0) (StaticConfig.UID + id).hashCode().toString() + "" else "" + (id + StaticConfig.UID).hashCode()
                            dataListFriend!!.listFriend!!.add(user)
                            context?.let { FriendDB.getInstance(it).addFriend(user) }
                        }
                        getAllFriendInfo(index + 1)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        }
    }

    companion object {
        var ACTION_START_CHAT = 1

        val ACTION_DELETE_FRIEND = "com.android.rivchat.DELETE_FRIEND"
    }
}

internal class ListFriendsAdapter(
    private val context: Context,
    private val listFriend: ListFriend,
    private val fragment: FriendsFragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dialogWaitDeleting: LovelyProgressDialog

    init {
        mapQuery = HashMap()
        mapChildListener = HashMap()
        mapMark = HashMap()
        mapChildListenerOnline = HashMap()
        mapQueryOnline = HashMap()
        dialogWaitDeleting = LovelyProgressDialog(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false)
        return ItemFriendViewHolder(context, view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val name = listFriend.listFriend!![position].name
        val id = listFriend.listFriend!![position].id
        val idRoom = listFriend.listFriend!![position].idRoom
        val avata = listFriend.listFriend!![position].avata
        (holder as ItemFriendViewHolder).txtName.text = name

        (holder.txtName.parent.parent.parent as View)
            .setOnClickListener {
                holder.txtMessage.typeface = Typeface.DEFAULT
                holder.txtName.typeface = Typeface.DEFAULT
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name)
                val idFriend = ArrayList<CharSequence>()
                if (id != null) {
                    idFriend.add(id)
                }
                intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend)
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom)
                ChatActivity.bitmapAvataFriend = HashMap()
                if (avata != StaticConfig.STR_DEFAULT_BASE64) {
                    val decodedString = Base64.decode(avata, Base64.DEFAULT)
                    ChatActivity.bitmapAvataFriend!![id] ==
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                } else {
                    ChatActivity.bitmapAvataFriend!![id] ==
                        BitmapFactory.decodeResource(context.resources, R.drawable.default_avata)
                }

                mapMark?.get(id) == null
                fragment.startActivityForResult(intent, FriendsFragment.ACTION_START_CHAT)
                fragment.activity!!.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

        (holder.txtName.parent.parent.parent as View)
            .setOnLongClickListener {
                val friendName = holder.txtName.text as String

                AlertDialog.Builder(context)
                    .setTitle("Delete Friend")
                    .setMessage("Are you sure want to delete $friendName?")
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                        dialogInterface.dismiss()
                        val idFriendRemoval = listFriend.listFriend!![position].id
                        dialogWaitDeleting.setTitle("Deleting...")
                            .setCancelable(false)
                            .setTopColorRes(R.color.colorAccent)
                            .show()
                        deleteFriend(idFriendRemoval)
                    }
                    .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }
                    .show()

                true
            }


        if (listFriend.listFriend!![position].message.text!!.length > 0) {
            holder.txtMessage.visibility = View.VISIBLE
            holder.txtTime.visibility = View.VISIBLE
            if (!listFriend.listFriend!![position].message.text!!.startsWith(id!!)) {
                holder.txtMessage.text = listFriend.listFriend!![position].message.text
                holder.txtMessage.typeface = Typeface.DEFAULT
                holder.txtName.typeface = Typeface.DEFAULT
            } else {
                holder.txtMessage.text =
                    listFriend.listFriend!![position].message.text!!.substring((id + "").length)
                holder.txtMessage.typeface = Typeface.DEFAULT_BOLD
                holder.txtName.typeface = Typeface.DEFAULT_BOLD
            }
            val time =
                SimpleDateFormat("EEE, d MMM yyyy").format(Date(listFriend.listFriend!![position].message.timestamp))
            val today = SimpleDateFormat("EEE, d MMM yyyy").format(Date(System.currentTimeMillis()))
            if (today == time) {
                holder.txtTime.text =
                    SimpleDateFormat("HH:mm").format(Date(listFriend.listFriend!![position].message.timestamp))
            } else {
                holder.txtTime.text =
                    SimpleDateFormat("MMM d").format(Date(listFriend.listFriend!![position].message.timestamp))
            }
        } else {
            holder.txtMessage.visibility = View.GONE
            holder.txtTime.visibility = View.GONE
            if (mapQuery[id] == null && mapChildListener[id] == null) {
                mapQuery[id!!] = FirebaseDatabase.getInstance().reference.child("message/" + idRoom!!).limitToLast(1)
                mapChildListener[id] = object : ChildEventListener {
                    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                        val mapMessage = dataSnapshot.value as HashMap<*, *>?
                        if (mapMark!![id] != null) {
                            if ((mapMark!![id])!!) {
                                listFriend.listFriend!![position].message.text = id + mapMessage!!["text"]!!
                            } else {
                                listFriend.listFriend!![position].message.text = mapMessage!!["text"] as String?
                            }
                            notifyDataSetChanged()
                            mapMark!![id] = false
                        } else {
                            listFriend.listFriend!![position].message.text = mapMessage!!["text"] as String?
                            notifyDataSetChanged()
                        }
                        listFriend.listFriend!![position].message.timestamp = mapMessage["timestamp"] as Long
                    }

                    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                    }

                    override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                    }

                    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                }
                mapQuery[id]!!.addChildEventListener(mapChildListener[id]!!)
                mapMark!![id] = true
            } else {
                mapQuery[id]!!.removeEventListener(mapChildListener[id]!!)
                mapQuery[id]!!.addChildEventListener(mapChildListener[id]!!)
                mapMark!![id!!] = true
            }
        }
        if (listFriend.listFriend!![position].avata == StaticConfig.STR_DEFAULT_BASE64) {
            holder.avata.setImageResource(R.drawable.default_avata)
        } else {
            val decodedString = Base64.decode(listFriend.listFriend!![position].avata, Base64.DEFAULT)
            val src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            holder.avata.setImageBitmap(src)
        }


        if (mapQueryOnline[id] == null && mapChildListenerOnline[id] == null) {
            mapQueryOnline[id] = FirebaseDatabase.getInstance().reference.child("user/$id/status")
            mapChildListenerOnline[id] = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.value != null && dataSnapshot.key == "isOnline") {
                        Log.d("FriendsFragment add $id", (dataSnapshot.value as Boolean).toString() + "")
                        listFriend.listFriend!![position].status.isOnline = dataSnapshot.value as Boolean
                        notifyDataSetChanged()
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.value != null && dataSnapshot.key == "isOnline") {
                        Log.d("FriendsFragment change $id", (dataSnapshot.value as Boolean).toString() + "")
                        listFriend.listFriend!![position].status.isOnline = dataSnapshot.value as Boolean
                        notifyDataSetChanged()
                    }
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            }
            mapQueryOnline[id]!!.addChildEventListener(mapChildListenerOnline[id]!!)
        }

        if (listFriend.listFriend!![position].status.isOnline) {
            holder.avata.borderWidth = 10
        } else {
            holder.avata.borderWidth = 0
        }
    }

    override fun getItemCount(): Int {
        return if (listFriend.listFriend != null) listFriend.listFriend!!.size else 0
    }

    /**
     * Delete friend
     *
     * @param idFriend
     */
    private fun deleteFriend(idFriend: String?) {
        if (idFriend != null) {
            FirebaseDatabase.getInstance().reference.child("friend").child(StaticConfig.UID)
                .orderByValue().equalTo(idFriend).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        if (dataSnapshot.value == null) {
                            //email not found
                            dialogWaitDeleting.dismiss()
                            LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setTitle("Error")
                                .setMessage("Error occurred during deleting friend")
                                .show()
                        } else {
                            val idRemoval = (dataSnapshot.value as HashMap<*, *>).keys.iterator().next().toString()
                            FirebaseDatabase.getInstance().reference.child("friend")
                                .child(StaticConfig.UID).child(idRemoval).removeValue()
                                .addOnCompleteListener {
                                    dialogWaitDeleting.dismiss()

                                    LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setTitle("Success")
                                        .setMessage("Friend deleting successfully")
                                        .show()

                                    val intentDeleted = Intent(FriendsFragment.ACTION_DELETE_FRIEND)
                                    intentDeleted.putExtra("idFriend", idFriend)
                                    context.sendBroadcast(intentDeleted)
                                }
                                .addOnFailureListener {
                                    dialogWaitDeleting.dismiss()
                                    LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setTitle("Error")
                                        .setMessage("Error occurred during deleting friend")
                                        .show()
                                }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        } else {
            dialogWaitDeleting.dismiss()
            LovelyInfoDialog(context)
                .setTopColorRes(R.color.colorPrimary)
                .setTitle("Error")
                .setMessage("Error occurred during deleting friend")
                .show()
        }
    }

    companion object {
        lateinit var mapQuery: MutableMap<String, Query>
        lateinit var mapQueryOnline: MutableMap<String, DatabaseReference>
        lateinit var mapChildListener: MutableMap<String, ChildEventListener>
        lateinit var mapChildListenerOnline: MutableMap<String, ChildEventListener>
        var mapMark: MutableMap<String, Boolean>? = null
    }
}

internal class ItemFriendViewHolder(private val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
    var avata: CircleImageView
    var txtName: TextView
    var txtTime: TextView
    var txtMessage: TextView

    init {
        avata = itemView.findViewById<View>(R.id.icon_avata) as CircleImageView
        txtName = itemView.findViewById<View>(R.id.txtName) as TextView
        txtTime = itemView.findViewById<View>(R.id.txtTime) as TextView
        txtMessage = itemView.findViewById<View>(R.id.txtMessage) as TextView
    }
}

