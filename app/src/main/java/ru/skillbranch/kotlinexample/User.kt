package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*


class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        @SuppressLint("DefaultLocale")
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.trim().toLowerCase(Locale.getDefault())
        }
        get() = _login!!

    private val salt: String by lazy {
        ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    }
    private var restoredSalt: String? = null

    private lateinit var passwordHash: String

    // Это поле нам понадобится только для тестов. В продакшн-байткоде его не будет
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String, // в видео было String?
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary (email) constructor was called")
        passwordHash = encrypt(password)
    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary (phone) constructor was called")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    // for restore from dsv data
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        rawPhone: String?,
        salt: String,
        hash: String
    ) : this(
        firstName, lastName, email = email, rawPhone = rawPhone,
        meta = mapOf("src" to "csv")
    ) {
        println("Secondary (dsv) constructor was called")
        restoredSalt = salt
        passwordHash = hash
    }


    init {
        println("First init block, primary constructor was called")
        check(firstName.isNotBlank()) { "First name must be not blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {
            "Email or phone must be not null and not blank"
        }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()

    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash
        .also { println("Checking password hash is $passwordHash") }

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Password $oldPass has been changed on new p/w $newPass")
        }
        else throw IllegalArgumentException(
            "The entered password does not match the current password"
        )
    }

    // private fun encrypt(password: String) = password.md5() // Do not do that !!!
    private fun encrypt(password: String): String {
        val realSalt = if (restoredSalt == null) salt else restoredSalt
        println("Salt while encrypt: $realSalt")
        return realSalt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 bytes
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }



    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("...sending access code: $code on phone: $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            phone: String? = null,
            password: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() ->
                    User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException(
                    "Email or phone must not be null or blank"
                )
            }
        }

        fun makeUser(
            fullName: String,
            email: String? = null,
            phone: String? = null,
            salt: String,
            hash: String
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return User(firstName, lastName, email, phone, salt, hash)
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
            split(" ").filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "Full name must contain " +
                                    "only first/last name. Current value $this"
                        )
                    }
                }
    }
}

fun generateAccessCode(): String {
    val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return StringBuilder().apply {
        repeat(6) {
            (possible.indices).random().also { index ->
                append(possible[index])
            }
        }
    }.toString()
}
