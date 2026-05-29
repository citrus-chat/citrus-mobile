package com.citruschat.citrusmobile.navigation

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Home = "home"

    const val Main = "main"
    const val Chat = "chat/{chatId}"

    const val Profile = "profile"

    fun chat(chatId: Long) = "chat/$chatId"
}
