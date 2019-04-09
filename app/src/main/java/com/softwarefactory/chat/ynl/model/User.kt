package com.softwarefactory.chat.ynl.model


open class User {
    var name: String? = null
    var email: String? = null
    var avata: String? = null
    var status: Status
    var message: Message

    init {
        status = Status()
        message = Message()
        status.isOnline = false
        status.timestamp = 0
        message.idReceiver = "0"
        message.idSender = "0"
        message.text = ""
        message.timestamp = 0
    }
}
