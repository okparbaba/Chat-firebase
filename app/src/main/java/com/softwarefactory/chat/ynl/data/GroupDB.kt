package com.softwarefactory.chat.ynl.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.softwarefactory.chat.ynl.model.Group


import java.util.ArrayList
import java.util.HashMap

class GroupDB// To prevent someone from accidentally instantiating the contract class,
// make the constructor private.
private constructor() {

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    val listGroups: ArrayList<Group>
        get() {
            val mapGroup = HashMap<String, Group>()
            val listKey = ArrayList<String>()
            val db = mDbHelper!!.readableDatabase
            try {
                val cursor = db.rawQuery("select * from " + FeedEntry.TABLE_NAME, null)
                while (cursor.moveToNext()) {
                    val idGroup = cursor.getString(0)
                    val nameGroup = cursor.getString(1)
                    val admin = cursor.getString(2)
                    val member = cursor.getString(3)
                    if (!listKey.contains(idGroup)) {
                        val newGroup = Group()
                        newGroup.id = idGroup
                        newGroup.groupInfo["name"]==nameGroup
                        newGroup.groupInfo["admin"] == admin
                        newGroup.member.add(member)
                        listKey.add(idGroup)
                        mapGroup[idGroup] = newGroup
                    } else {
                        mapGroup[idGroup]!!.member.add(member)
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                return ArrayList()
            }

            val listGroup = ArrayList<Group>()
            for (key in listKey) {
                listGroup.add(mapGroup[key]!!)
            }

            return listGroup
        }

    fun addGroup(group: Group) {
        val db = mDbHelper!!.writableDatabase
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(FeedEntry.COLUMN_GROUP_ID, group.id)
        values.put(FeedEntry.COLUMN_GROUP_NAME, group.groupInfo["name"])
        values.put(FeedEntry.COLUMN_GROUP_ADMIN, group.groupInfo["admin"])

        for (idMenber in group.member) {
            values.put(FeedEntry.COLUMN_GROUP_MEMBER, idMenber)
            // Insert the new row, returning the primary key value of the new row
            db.insert(FeedEntry.TABLE_NAME, null, values)
        }
    }

    fun deleteGroup(idGroup: String) {
        val db = mDbHelper!!.writableDatabase
        db.delete(FeedEntry.TABLE_NAME, FeedEntry.COLUMN_GROUP_ID + " = " + idGroup, null)
    }


    fun addListGroup(listGroup: ArrayList<Group>) {
        for (group in listGroup) {
            addGroup(group)
        }
    }

    fun getGroup(id: String): Group {
        val db = mDbHelper!!.readableDatabase
        val cursor = db.rawQuery(
            "select * from " + FeedEntry.TABLE_NAME + " where " + FeedEntry.COLUMN_GROUP_ID + " = " + id,
            null
        )
        val newGroup = Group()
        while (cursor.moveToNext()) {
            val idGroup = cursor.getString(0)
            val nameGroup = cursor.getString(1)
            val admin = cursor.getString(2)
            val member = cursor.getString(3)
            newGroup.id = idGroup
            newGroup.groupInfo["name"] == nameGroup
            newGroup.groupInfo["admin"] == admin
            newGroup.member.add(member)
        }
        return newGroup
    }

    fun dropDB() {
        val db = mDbHelper!!.writableDatabase
        db.execSQL(SQL_DELETE_ENTRIES)
        db.execSQL(SQL_CREATE_ENTRIES)
    }


    class FeedEntry : BaseColumns {
        companion object {
            internal val TABLE_NAME = "groups"
            internal val COLUMN_GROUP_ID = "groupID"
            internal val COLUMN_GROUP_NAME = "name"
            internal val COLUMN_GROUP_ADMIN = "admin"
            internal val COLUMN_GROUP_MEMBER = "memberID"
        }
    }


    private class GroupDBHelper internal constructor(context: Context) :
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
            internal val DATABASE_NAME = "GroupChat.db"
        }
    }

    companion object {
        private var mDbHelper: GroupDBHelper? = null

        private var instance: GroupDB? = null

        fun getInstance(context: Context): GroupDB {
            if (instance == null) {
                instance = GroupDB()
                mDbHelper = GroupDBHelper(context)
            }
            return instance as GroupDB
        }

        private val TEXT_TYPE = " TEXT"
        private val COMMA_SEP = ","
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                FeedEntry.COLUMN_GROUP_ID + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_GROUP_NAME + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_GROUP_ADMIN + TEXT_TYPE + COMMA_SEP +
                FeedEntry.COLUMN_GROUP_MEMBER + TEXT_TYPE + COMMA_SEP +
                "PRIMARY KEY (" + FeedEntry.COLUMN_GROUP_ID + COMMA_SEP +
                FeedEntry.COLUMN_GROUP_MEMBER + "))"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME
    }
}
