package com.mindspring.app.domain

/** Form validation rules, kept free of Android types so they can be unit tested. */
object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 6

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun nameError(name: String): String? = if (name.isBlank()) "Please enter your name." else null

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Please enter your email."
        !isValidEmail(email) -> "That doesn't look like a valid email."
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters."
        else -> null
    }

    fun confirmError(password: String, confirm: String): String? =
        if (password != confirm) "Passwords don't match." else null
}
