package com.softwarefactory.chat.ynl.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.FriendDB
import com.softwarefactory.chat.ynl.data.GroupDB
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Group
import com.softwarefactory.chat.ynl.model.ListFriend
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog

import java.util.ArrayList
import java.util.HashMap


class GroupFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var recyclerListGroups: RecyclerView? = null
    lateinit var onClickFloatButton: FragGroupClickFloatButton
    private var listGroup: ArrayList<Group>? = null
    private var adapter: ListGroupsAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    internal lateinit var progressDialog: LovelyProgressDialog
    internal lateinit var waitingLeavingGroup: LovelyProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_group, container, false)

        listGroup = context?.let { GroupDB.getInstance(it).listGroups }
        recyclerListGroups = layout.findViewById<View>(R.id.recycleListGroup) as RecyclerView
        mSwipeRefreshLayout = layout.findViewById<View>(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        mSwipeRefreshLayout!!.setOnRefreshListener(this)
        val layoutManager = GridLayoutManager(context, 2)
        recyclerListGroups!!.layoutManager = layoutManager
        adapter = ListGroupsAdapter(context!!,listGroup!!)
        recyclerListGroups!!.adapter = adapter
        onClickFloatButton = FragGroupClickFloatButton()
        progressDialog = LovelyProgressDialog(context)
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_delete_group)
            .setTitle(resources.getString(R.string.deleting))
            .setTopColorRes(R.color.colorAccent)

        waitingLeavingGroup = LovelyProgressDialog(context)
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_delete_group)
            .setTitle(resources.getString(R.string.group_leaving))
            .setTopColorRes(R.color.colorAccent)

        if (listGroup!!.size == 0) {
            //Ket noi server hien thi group
            mSwipeRefreshLayout!!.isRefreshing = true
            getListGroup()
        }
        return layout
    }

    private fun getListGroup() {
        FirebaseDatabase.getInstance().reference.child("user/" + StaticConfig.UID + "/group")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {
                        val mapListGroup = dataSnapshot.value as HashMap<*, *>?
                        val iterator = mapListGroup!!.keys.iterator()
                        while (iterator.hasNext()) {
                            val idGroup = mapListGroup[iterator.next().toString()] as String?
                            val newGroup = Group()
                            newGroup.id = idGroup
                            listGroup!!.add(newGroup)
                        }
                        getGroupInfo(0)
                    } else {
                        mSwipeRefreshLayout!!.isRefreshing = false
                        adapter!!.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    mSwipeRefreshLayout!!.isRefreshing = false
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_GROUP && resultCode == Activity.RESULT_OK) {
            listGroup!!.clear()
            ListGroupsAdapter.listFriend = null
            context?.let { GroupDB.getInstance(it).dropDB() }
            getListGroup()
        }
    }

    private fun getGroupInfo(indexGroup: Int) {
        if (indexGroup == listGroup!!.size) {
            adapter!!.notifyDataSetChanged()
            mSwipeRefreshLayout!!.isRefreshing = false
        } else {
            FirebaseDatabase.getInstance().reference.child("group/" + listGroup!![indexGroup].id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {
                            val mapGroup = dataSnapshot.value as HashMap<*, *>?
                            val member = mapGroup!!["member"] as ArrayList<String>?
                            val mapGroupInfo = mapGroup["groupInfo"] as HashMap<*, *>?
                            for (idMember in member!!) {
                                listGroup!![indexGroup].member.add(idMember)
                            }
                            listGroup!![indexGroup].groupInfo["name"] == (mapGroupInfo?.get("name") as String?)
                            listGroup!![indexGroup].groupInfo["admin"] == (mapGroupInfo?.get("admin") as String?)
                        }
                        context?.let { GroupDB.getInstance(it).addGroup(listGroup!![indexGroup]) }
                        Log.d("GroupFragment", listGroup!![indexGroup].id + ": " + dataSnapshot.toString())
                        getGroupInfo(indexGroup + 1)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        }
    }

    override fun onRefresh() {
        listGroup!!.clear()
        ListGroupsAdapter.listFriend = null
        context?.let { GroupDB.getInstance(it).dropDB() }
        adapter!!.notifyDataSetChanged()
        getListGroup()
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            CONTEXT_MENU_DELETE -> {
                val posGroup = item.intent.getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1)
                if (listGroup!![posGroup].groupInfo["admin"] as String == StaticConfig.UID) {
                    val group = listGroup!![posGroup]
                    listGroup!!.removeAt(posGroup)
                    if (group != null) {
                        deleteGroup(group, 0)
                    }
                } else {
                    Toast.makeText(activity, "You are not admin", Toast.LENGTH_LONG).show()
                }
            }
            CONTEXT_MENU_EDIT -> {
                val posGroup1 = item.intent.getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1)
                if (listGroup!![posGroup1].groupInfo["admin"] as String == StaticConfig.UID) {
                    val intent = Intent(context, AddGroupActivity::class.java)
                    intent.putExtra("groupId", listGroup!![posGroup1].id)
                    startActivityForResult(intent, REQUEST_EDIT_GROUP)
                } else {
                    Toast.makeText(activity, "You are not admin", Toast.LENGTH_LONG).show()
                }
            }

            CONTEXT_MENU_LEAVE -> {
                val position = item.intent.getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1)
                if (listGroup!![position].groupInfo["admin"] as String == StaticConfig.UID) {
                    Toast.makeText(activity, "Admin cannot leave group", Toast.LENGTH_LONG).show()
                } else {
                    waitingLeavingGroup.show()
                    val groupLeaving = listGroup!![position]
                    leaveGroup(groupLeaving)
                }
            }
        }

        return super.onContextItemSelected(item)
    }

    fun deleteGroup(group: Group, index: Int) {
        if (index == group.member.size) {
            FirebaseDatabase.getInstance().reference.child("group/" + group.id!!).removeValue()
                .addOnCompleteListener {
                    progressDialog.dismiss()
                    context?.let { it1 -> GroupDB.getInstance(it1).deleteGroup(group.id!!) }
                    listGroup!!.remove(group)
                    adapter!!.notifyDataSetChanged()
                    Toast.makeText(context, "Deleted group", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorAccent)
                        .setIcon(R.drawable.ic_dialog_delete_group)
                        .setTitle(resources.getString(R.string.fal))
                        .setMessage(resources.getString(R.string.cannot_delete_group))
                        .setCancelable(false)
                        .setConfirmButtonText("Ok")
                        .show()
                }
        } else {
            FirebaseDatabase.getInstance().reference.child("user/" + group.member[index] + "/group/" + group.id)
                .removeValue()
                .addOnCompleteListener { deleteGroup(group, index + 1) }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorAccent)
                        .setIcon(R.drawable.ic_dialog_delete_group)
                        .setTitle(resources.getString(R.string.fal))
                        .setMessage(resources.getString(R.string.cannot_connect_server))
                        .setCancelable(false)
                        .setConfirmButtonText(resources.getString(R.string.ok))
                        .show()
                }
        }

    }

    fun leaveGroup(group: Group) {
        FirebaseDatabase.getInstance().reference.child("group/" + group.id + "/member")
            .orderByValue().equalTo(StaticConfig.UID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.value == null) {
                        //email not found
                        waitingLeavingGroup.dismiss()
                        LovelyInfoDialog(context)
                            .setTopColorRes(R.color.colorAccent)
                            .setTitle(resources.getString(R.string.error))
                            .setMessage(resources.getString(R.string.error_occur_leave_group))
                            .show()
                    } else {
                        var memberIndex = ""
                        val result = dataSnapshot.value as ArrayList<String>?
                        for (i in result!!.indices) {
                            if (result.get(i) != null) {
                                memberIndex = i.toString()
                            }
                        }

                        FirebaseDatabase.getInstance().reference.child("user").child(StaticConfig.UID)
                            .child("group").child(group.id!!).removeValue()
                        FirebaseDatabase.getInstance().reference.child("group/" + group.id + "/member")
                            .child(memberIndex).removeValue()
                            .addOnCompleteListener {
                                waitingLeavingGroup.dismiss()

                                listGroup!!.remove(group)
                                adapter!!.notifyDataSetChanged()
                                context?.let { it1 -> GroupDB.getInstance(it1).deleteGroup(group.id!!) }
                                LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle(resources.getString(R.string.success))
                                    .setMessage(resources.getString(R.string.group_leaving_success))
                                    .show()
                            }
                            .addOnFailureListener {
                                waitingLeavingGroup.dismiss()
                                LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle(resources.getString(R.string.error))
                                    .setMessage(resources.getString(R.string.error_occur_leave_group))
                                    .show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //email not found
                    waitingLeavingGroup.dismiss()
                    LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorAccent)
                        .setTitle(resources.getString(R.string.error))
                        .setMessage(resources.getString(R.string.error_occur_leave_group))
                        .show()
                }
            })

    }

    inner class FragGroupClickFloatButton : View.OnClickListener {

        internal lateinit var context: Context
        fun getInstance(context: Context): FragGroupClickFloatButton {
            this.context = context
            return this
        }

        override fun onClick(view: View) {
            startActivity(Intent(getContext(), AddGroupActivity::class.java))
        }
    }

    companion object {
        val CONTEXT_MENU_DELETE = 1
        val CONTEXT_MENU_EDIT = 2
        val CONTEXT_MENU_LEAVE = 3
        val REQUEST_EDIT_GROUP = 0
        val CONTEXT_MENU_KEY_INTENT_DATA_POS = "pos"
    }
}// Required empty public constructor

internal class ListGroupsAdapter(private val context: Context, private val listGroup: ArrayList<Group>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.rc_item_group, parent, false)
        return ItemGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val groupName = listGroup[position].groupInfo["name"]
        if (!(groupName == null || groupName.isEmpty())) {
            (holder as ItemGroupViewHolder).txtGroupName.text = groupName
            holder.iconGroup.text = (groupName[0] + "").toUpperCase()
        }
        (holder as ItemGroupViewHolder).btnMore.setOnClickListener { view ->
            view.tag = arrayOf(groupName, position)
            view.parent.showContextMenuForChild(view)
        }
        (holder.txtGroupName.parent as RelativeLayout).setOnClickListener {
            if (listFriend == null) {
                listFriend = FriendDB.getInstance(context).listFriend
            }
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, groupName)
            val idFriend = ArrayList<CharSequence>()
            ChatActivity.bitmapAvataFriend = HashMap<String, Bitmap>()
            for (id in listGroup[position].member) {
                idFriend.add(id)
                val avata = listFriend!!.getAvataById(id)
                if (avata != StaticConfig.STR_DEFAULT_BASE64) {
                    val decodedString = Base64.decode(avata, Base64.DEFAULT)
                    ChatActivity.bitmapAvataFriend!![id] ==
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                } else if (avata == StaticConfig.STR_DEFAULT_BASE64) {
                    ChatActivity.bitmapAvataFriend!![id] ==
                        BitmapFactory.decodeResource(context.resources, R.drawable.default_avata)
                } else {
                    ChatActivity.bitmapAvataFriend!![id] == null
                }
            }
            intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend)
            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, listGroup[position].id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return listGroup.size
    }

    companion object {
        var listFriend: ListFriend? = null
    }
}

internal class ItemGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnCreateContextMenuListener {
    var iconGroup: TextView
    var txtGroupName: TextView
    var btnMore: ImageButton

    init {
        itemView.setOnCreateContextMenuListener(this)
        iconGroup = itemView.findViewById<View>(R.id.icon_group) as TextView
        txtGroupName = itemView.findViewById<View>(R.id.txtName) as TextView
        btnMore = itemView.findViewById<View>(R.id.btnMoreAction) as ImageButton
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, contextMenuInfo: ContextMenu.ContextMenuInfo) {
        menu.setHeaderTitle((btnMore.tag as Array<*>)[0] as String)
        val data = Intent()
        data.putExtra(GroupFragment.CONTEXT_MENU_KEY_INTENT_DATA_POS, (btnMore.tag as Array<*>)[1] as Int)
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_EDIT, Menu.NONE, "Edit group").intent = data
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_DELETE, Menu.NONE, "Delete group").intent = data
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_LEAVE, Menu.NONE, "Leave group").intent = data
    }
}