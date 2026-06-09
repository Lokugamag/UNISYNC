package com.example.unisync.ui.auth

/**
 * Validates password rules:
 * - Minimum 8 characters.
 * - At least one uppercase letter.
 * - At least one lowercase letter.
 * - At least one digit.
 * - At least one special character.
 * Returns null if valid, or a descriptive error message if invalid.
 */
fun validatePasswordRules(password: String): String? {
    if (password.length < 8) {
        return "Password must be at least 8 characters long."
    }
    if (!password.any { it.isUpperCase() }) {
        return "Password must contain at least one uppercase letter."
    }
    if (!password.any { it.isLowerCase() }) {
        return "Password must contain at least one lowercase letter."
    }
    if (!password.any { it.isDigit() }) {
        return "Password must contain at least one digit."
    }
    val specialChars = "!@#$%^&*(),.?\":{}|<>"
    if (!password.any { specialChars.contains(it) }) {
        return "Password must contain at least one special character."
    }
    return null
}
