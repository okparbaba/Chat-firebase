package com.softwarefactory.chat.ynl.model

import java.util.ArrayList
import java.util.HashMap


open class Room {
    var member: ArrayList<String>
    var groupInfo: Map<String, String>

    init {
        member = ArrayList()
        groupInfo = HashMap()
    }
}
