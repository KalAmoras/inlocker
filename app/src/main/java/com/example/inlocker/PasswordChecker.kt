package com.example.inlocker

import android.content.Context

interface PasswordChecker {
    suspend fun checkAndRequestPassword(
        context: Context,
        passwordType: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    )

}