package com.citruschat.citrusmobile.navigation

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Home = "home"
    const val Chat = "chat/{chatId}"

    fun chat(chatId: Long) = "chat/$chatId"
}
