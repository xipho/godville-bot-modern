package ru.xipho.godvillebotmodern.bot.notifications.sms

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class SmscApi(private val login: String, private val password: String) {
    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun sendSms(phone: String, message: String): String {
        val url = (BASE_URL + "send.php").toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("login", login)
            .addQueryParameter("psw", password)
            .addQueryParameter("phones", phone)
            .addQueryParameter("mes", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body!!.string()
        }
    }

    companion object {
        private const val BASE_URL = "https://smsc.ru/sys/"
    }
}