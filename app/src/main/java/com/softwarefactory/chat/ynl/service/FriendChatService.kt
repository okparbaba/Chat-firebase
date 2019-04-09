package com.softwarefactory.chat.ynl.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Base64
import android.util.Log

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.softwarefactory.chat.ynl.MainActivity
import com.softwarefactory.chat.ynl.R
import com.softwarefactory.chat.ynl.data.FriendDB
import com.softwarefactory.chat.ynl.data.GroupDB
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.model.Friend
import com.softwarefactory.chat.ynl.model.Group
import com.softwarefactory.chat.ynl.model.ListFriend

import java.util.ArrayList
import java.util.HashMap


class FriendChatService : Service() {
    // Binder given to clients
    val mBinder: IBinder = LocalBinder()
    lateinit var mapMark: MutableMap<String, Boolean>
    lateinit var mapQuery: MutableMap<String, Query>
    lateinit var mapChildEventListenerMap: MutableMap<String, ChildEventListener>
    lateinit var mapBitmap: MutableMap<String, Bitmap>
    lateinit var listKey: ArrayList<String>
    lateinit var listFriend: ListFriend
    lateinit var listGroup: ArrayList<Group>
    lateinit var updateOnline: CountDownTimer


    override fun onCreate() {
        super.onCreate()
        mapMark = HashMap()
        mapQuery = HashMap()
        mapChildEventListenerMap = HashMap()
        listFriend = FriendDB.getInstance(this).listFriend
        listGroup = GroupDB.getInstance(this).listGroups
        listKey = ArrayList()
        mapBitmap = HashMap()
        updateOnline = object : CountDownTimer(System.currentTimeMillis(), StaticConfig.TIME_TO_REFRESH) {
            override fun onTick(l: Long) {
                ServiceUtils.updateUserStatus(applicationContext)
            }

            override fun onFinish() {

            }
        }
        updateOnline.start()

        if (listFriend.listFriend!!.size > 0 || listGroup.size > 0) {
            for (friend in listFriend.listFriend!!) {
                if (!listKey.contains(friend.idRoom)) {
                    mapQuery[friend.idRoom!!] =
                        FirebaseDatabase.getInstance().reference.child("message/" + friend.idRoom!!).limitToLast(1)
                    mapChildEventListenerMap[friend.idRoom!!] = object : ChildEventListener {
                        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                            if (mapMark[friend.idRoom!!] != null && mapMark[friend.idRoom!!]!!) {
                                //                                Toast.makeText(FriendChatService.this, friend.name + ": " + ((HashMap)dataSnapshot.getValue()).get("text"), Toast.LENGTH_SHORT).show();
                                if (mapBitmap[friend.idRoom!!] == null) {
                                    if (friend.avata != StaticConfig.STR_DEFAULT_BASE64) {
                                        val decodedString = Base64.decode(friend.avata, Base64.DEFAULT)
                                        mapBitmap[friend.idRoom!!] =
                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                    } else {
                                        mapBitmap[friend.idRoom!!] =
                                            BitmapFactory.decodeResource(resources, R.drawable.default_avata)
                                    }
                                }
                                createNotify(
                                    friend.name,
                                    ((dataSnapshot.value as HashMap<*, *>)["text"] as String?)!!,
                                    friend.idRoom!!.hashCode(),
                                    mapBitmap[friend.idRoom!!],
                                    false
                                )

                            } else {
                                mapMark[friend.idRoom!!] = true
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
                    }
                    listKey.add(friend.idRoom!!)
                }
                mapQuery[friend.idRoom]!!.addChildEventListener(mapChildEventListenerMap[friend.idRoom]!!)
            }

            for (group in listGroup) {
                if (!listKey.contains(group.id)) {
                    mapQuery[group.id!!] =
                        FirebaseDatabase.getInstance().reference.child("message/" + group.id!!).limitToLast(1)
                    mapChildEventListenerMap[group.id!!] = object : ChildEventListener {
                        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                            if (mapMark[group.id!!] != null && mapMark[group.id!!]!!) {
                                if (mapBitmap[group.id!!] == null) {
                                    mapBitmap[group.id!!] =
                                        BitmapFactory.decodeResource(resources, R.drawable.ic_notify_group)
                                }
                                createNotify(
                                    group.groupInfo["name"],
                                    ((dataSnapshot.value as HashMap<*, *>)["text"] as String?)!!,
                                    group.id!!.hashCode(),
                                    mapBitmap[group.id!!],
                                    true
                                )
                            } else {
                                mapMark[group.id!!] = true
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
                    }
                    listKey.add(group.id!!)
                }
                mapQuery[group.id]!!.addChildEventListener(mapChildEventListenerMap[group.id]!!)
            }

        } else {
            stopSelf()
        }
    }

    fun stopNotify(id: String) {
        mapMark[id] = false
    }

    fun createNotify(name: String?, content: String, id: Int, icon: Bitmap?, isGroup: Boolean) {
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(this)
            .setLargeIcon(icon)
            .setContentTitle(name)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000))
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setAutoCancel(true)
        if (isGroup) {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_group)
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_person)
        }
        val notificationManager = this.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.cancel(id)
        notificationManager.notify(
            id,
            notificationBuilder.build()
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "OnStartService")
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "OnBindService")
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        for (id in listKey) {
            mapQuery[id]!!.removeEventListener(mapChildEventListenerMap[id]!!)
        }
        mapQuery.clear()
        mapChildEventListenerMap.clear()
        mapBitmap.clear()
        updateOnline.cancel()
        Log.d(TAG, "OnDestroyService")
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: FriendChatService
            get() = this@FriendChatService
    }

    companion object {
        private val TAG = "FriendChatService"
    }
}
