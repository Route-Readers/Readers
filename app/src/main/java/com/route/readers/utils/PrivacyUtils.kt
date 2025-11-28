package com.route.readers.utils

import java.security.MessageDigest

object PrivacyUtils {

    fun normalizePhoneNumber(phoneNumber: String): String {
        // 1. Remove all non-digit characters
        val digits = phoneNumber.filter { it.isDigit() }

        // 2. Handle country code (South Korea specific logic for this project)
        // If it starts with '010', replace with '+8210'
        // If it already starts with '82', assume it's correct or prepend '+'
        // For simplicity and the guide's context, we'll focus on 010 -> +8210
        return if (digits.startsWith("010")) {
            "+82${digits.substring(1)}"
        } else {
            // Fallback: just return the digits if it doesn't match the pattern,
            // or potentially other patterns can be added here.
            digits
        }
    }

    fun hashPhoneNumber(phoneNumber: String): String {
        val normalized = normalizePhoneNumber(phoneNumber)
        val bytes = normalized.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
