package com.kalsys.inlocker

object PlaceholderTextHelper {

    fun getPlaceholderText(appName: String?): String {
        return if (appName.isNullOrEmpty()) {
            "Enter password"
        } else {
            val simpleAppName = extractSimpleAppName(appName)
            "Enter password for: $simpleAppName"
        }
    }

    fun getPlaceholderTextOnCreatePassword(appName: String?): String {
        return if (appName.isNullOrEmpty()) {
            "Enter new password"
        } else {
            val simpleAppName = extractSimpleAppName(appName)
            "Enter new password for: $simpleAppName"
        }
    }

    private fun extractSimpleAppName(appName: String): String {
        val parts = appName.split('.')
        return when {
            parts.size >= 3 -> parts.subList(1, parts.size).joinToString(".")
            else -> appName
        }
    }
}
