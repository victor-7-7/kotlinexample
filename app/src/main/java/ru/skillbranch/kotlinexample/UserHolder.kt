package ru.skillbranch.kotlinexample

@ExperimentalStdlibApi
object UserHolder {

    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email = email, password = password)
        .also { user -> map[user.login] = user }

    fun loginUser(login: String, pass: String): String? =
        map[login.trim()]?.run {
            if (checkPassword(pass)) userInfo
            else null
        }

}