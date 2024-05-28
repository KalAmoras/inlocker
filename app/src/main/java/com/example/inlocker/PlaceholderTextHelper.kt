package com.example.inlocker

object PlaceholderTextHelper {

    fun getPlaceholderText(appName: String?): String {
        return if (appName.isNullOrEmpty()) {
            "Enter password"
        } else {
            val simpleAppName = appName.split('.').lastOrNull() ?: appName

            "Enter password for: $simpleAppName"
        }
    }

    fun getPlaceholderTextOnCreatePassword(appName: String?): String {
        return if (appName.isNullOrEmpty()) {
            "Enter new password"
        } else {
            val simpleAppName = appName.split('.').lastOrNull() ?: appName

            "Enter new password for: $simpleAppName"
        }
    }
}
