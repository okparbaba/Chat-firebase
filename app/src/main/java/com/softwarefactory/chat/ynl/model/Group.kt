package com.softwarefactory.chat.ynl.model


class Group : Room() {
    var id: String? = null
    var listFriend: ListFriend

    init {
        listFriend = ListFriend()
    }
}
