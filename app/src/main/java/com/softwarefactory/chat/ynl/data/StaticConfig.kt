package com.softwarefactory.chat.ynl.data


object StaticConfig {
    var REQUEST_CODE_REGISTER = 2000
    var STR_EXTRA_ACTION_LOGIN = "login"
    var STR_EXTRA_ACTION_RESET = "resetpass"
    var STR_EXTRA_ACTION = "action"
    var STR_EXTRA_USERNAME = "username"
    var STR_EXTRA_PASSWORD = "password"
    var STR_DEFAULT_BASE64 = "default"
    var UID = ""
    //TODO only use this UID for debug mode
    //    public static String UID = "6kU0SbJPF5QJKZTfvW1BqKolrx22";
    var INTENT_KEY_CHAT_FRIEND = "friendname"
    var INTENT_KEY_CHAT_AVATA = "friendavata"
    var INTENT_KEY_CHAT_ID = "friendid"
    var INTENT_KEY_CHAT_ROOM_ID = "roomid"
    var TIME_TO_REFRESH = (10 * 1000).toLong()
    var TIME_TO_OFFLINE = (2 * 60 * 1000).toLong()


}
