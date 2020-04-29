package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.util.*

@ExperimentalStdlibApi
object UserHolder {

    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        val key = email.trim().toLowerCase(Locale.getDefault())
        if (map.containsKey(key)) throw IllegalArgumentException(
            "A user with this email already exists"
        )
        return User.makeUser(fullName, email = email, password = password)
        .also { user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String): String? {
        var key = login.trim().toLowerCase(Locale.getDefault())
        if (!map.containsKey(key))
            key = key.replace("[^+\\d]".toRegex(), "")
        return map[key]?.run {
            if (checkPassword(password)) userInfo
            else null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() = map.clear()

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        val key = rawPhone.replace("[^+\\d]".toRegex(), "")
        if (key.first() != '+' || key.length != 12) throw
        IllegalArgumentException(
            "Enter a valid phone number starting with" +
                    "a + and containing 11 digits"
        )

        if (map.containsKey(key)) throw IllegalArgumentException(
            "A user with this phone already exists"
        )

        return User.makeUser(fullName, phone = key)
            .also { user -> map[user.login] = user }
    }

    fun requestAccessCode(phone: String) {
        val user = map[phone.replace("[^+\\d]".toRegex(), "")]
            ?: throw IllegalArgumentException(
                "A user with this phone has not registered"
            )
        val newCode = generateAccessCode()
        user.changePassword(user.accessCode!!, newCode)
    }

    fun importUsers(dsvData: List<String>): List<User> {
        val users = mutableListOf<User>()
        var fullName = ""
        var salt = ""
        var hash = ""
        var email: String? = null
        var phone: String? = null

        dsvData.forEach { userData ->
            val list = userData.split(";")
            list.forEach { elem ->
                val elemValue = elem.trim()
                when (list.indexOf(elem)) {
                    0 -> fullName = elemValue
                    1 -> email = if (elemValue.isEmpty()) null else elemValue
                    2 -> {
                        salt = elemValue.split(":").first().trim()
                        hash = elemValue.split(":").last().trim()
                    }
                    3 -> phone = if (elemValue.isEmpty()) null else elemValue
                }
            }
            users.add(User.makeUser(fullName, email, phone, salt, hash))
        }
        return users
    }
}

