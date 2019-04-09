package com.softwarefactory.chat.ynl.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.softwarefactory.chat.ynl.model.Friend
import com.softwarefactory.chat.ynl.model.ListFriend


class FriendDB// To prevent someone from accidentally instantiating the contract class,
// make the constructor private.
private constructor() {

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    val listFriend: ListFriend
        get() {
            val listFriend = ListFriend()
            val db = mDbHelper!!.readableDatabase
            try {
                val cursor = db.rawQuery("select * from " + FeedEntry.TABLE_NAME, null)
                while (cursor.moveToNext()) {
                    val friend = Friend()
                    friend.id = cursor.getString(0)
                    friend.name = cursor.getString(1)
                    friend.email = cursor.getString(2)
                    friend.idRoom = cursor.getString(3)
                    friend.avata = cursor.getString(4)
                    listFriend.listFriend!!.add(friend)
                }
                cursor.close()
            } catch (e: Exception) {
                return ListFriend()
            }

            return listFriend
        }


    fun addFriend(friend: Friend): Long {
        val db = mDbHelper!!.writableDatabase
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(FeedEntry.COLUMN_NAME_ID, friend.id)
        values.put(FeedEntry.COLUMN_NAME_NAME, friend.name)
        values.put(FeedEntry.COLUMN_NAME_EMAIL, friend.email)
        values.put(FeedEntry.COLUMN_NAME_ID_ROOM, friend.idRoom)
        values.put(FeedEntry.COLUMN_NAME_AVATA, friend.avata)
        // Insert the new row, returning the primary key value of the new row
        return db.insert(FeedEntry.TABLE_NAME, null, values)
    }


    fun addListFriend(listFriend: ListFriend) {
        for (friend in listFriend.listFriend!!) {
            addFriend(friend)
        }
    }

    fun dropDB() {
        val db = mDbHelper!!.writableDatabase
        db.execSQL(SQL_DELETE_ENTRIES)
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    /* Inner class that defines the table contents */
    class FeedEntry : BaseColumns {
        companion object {
            internal val TABLE_NAME = "friend"
            internal val COLUMN_NAME_ID = "friendID"
            internal val COLUMN_NAME_NAME = "name"
            internal val COLUMN_NAME_EMAIL = "email"
            internal val COLUMN_NAME_ID_ROOM = "idRoom"
            internal val COLUMN_NAME_AVATA = "avata"
        }
    }


    private class FriendDBHelper internal constructor(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

        companion object {
            // If you change the database schema, you must increment the database version.
            internal val DATABASE_VERSION = 1
            internal val DATABASE_NAME = "FriendChat.db"
        }
    }

    companion object {
        private var mDbHelper: FriendDBHelper? = null

        private var instance: FriendDB? = null

        fun getInstance(context: Context): FriendDB {
            if (instance == null) {
                instance = FriendDB()
                mDbHelper = FriendDBHelper(context)
            }
            return instance as FriendDB
        }

        private val TEXT_TYPE = " TEXT"
        private val COMMA_SEP = ","
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                FeedEntry.COLUMN_NAME_ID + " TEXT PRIMARY KEY," +
                FeedEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_NAME_ID_ROOM + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_NAME_AVATA + TEXT_TYPE + " )"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME
    }
}
