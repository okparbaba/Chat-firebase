package com.softwarefactory.chat.ynl.model

import java.util.ArrayList


class ListFriend {
    var listFriend: ArrayList<Friend>? = null

    init {
        listFriend = ArrayList()
    }

    fun getAvataById(id: String): String? {
        for (friend in listFriend!!) {
            if (id == friend.id) {
                return friend.avata
            }
        }
        return ""
    }
}
