package com.kalsys.inlocker


import android.accounts.AccountManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import com.kalsys.inlocker.ui.components.CustomButton
import com.kalsys.inlocker.ui.theme.InLockerTheme



class CriticalSettingsActivity : AppCompatActivity() {

    private lateinit var passwordDao: PasswordDao
    private lateinit var emailDao: EmailDao
    private lateinit var passwordChecker: PasswordChecker
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private val _isAdminActive = mutableStateOf(false)
    private val isAdminActive: State<Boolean> get() = _isAdminActive

    companion object {
        const val RESULT_ENABLE = 1
        const val REQUEST_CODE_LOCK_SCREEN = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = PasswordDatabase.getInstance(applicationContext)
        passwordDao = database.passwordDao()
        emailDao = database.emailDao()

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        passwordChecker = PasswordCheckerImplementation(passwordDao, devicePolicyManager, compName)
        _isAdminActive.value = devicePolicyManager.isAdminActive(compName)



        setContent {
            InLockerTheme {
                LaunchedEffect(Unit) {
                    _isAdminActive.value = devicePolicyManager.isAdminActive(compName)
                }
                CriticalOptionsScreen(
                    onDeletePasswords = { showDeleteConfirmationDialog() },
                    onToggleUninstallProtection = { toggleUninstallProtection() },
                    isAdminActive = isAdminActive.value,
                    onSetDefaultPasswords = { setDefaultPassword() },
                    recoverySettings = { startEmailSettingsActivity() }
                )
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete All Passwords")
            .setMessage("Are you sure you want to delete all passwords?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    passwordChecker.checkAndRequestPassword(
                        this@CriticalSettingsActivity,
                        "delete_all_passwords",
                        { deletePasswords() },
                        {
                            Toast.makeText(this@CriticalSettingsActivity, "Password verification failed", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
            .setNegativeButton(android.R.string.no) { dialog, _ -> dialog.cancel() }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deletePasswords() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    passwordDao.deleteAllPasswords()
                }
                Toast.makeText(this@CriticalSettingsActivity, "All passwords were deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@CriticalSettingsActivity, "Error: Unable to delete passwords", Toast.LENGTH_SHORT).show()
                Log.e("CriticalSettingsActivity", "Deletion failed: ${e.message}")
            }
        }
    }

    private fun toggleUninstallProtection() {
        Log.d("CriticalSettingsActivity", "toggleUninstallProtection called")
        if (devicePolicyManager.isAdminActive(compName)) {
            Log.d("CriticalSettingsActivity", "Device admin is active, requesting password")
            lifecycleScope.launch {
                try {
                    passwordChecker.checkAndRequestPassword(
                        this@CriticalSettingsActivity,
                        "uninstall_protection",
                        {
                            Log.d("CriticalSettingsActivity", "Password verified, disabling device admin")
                            disableDeviceAdmin()
                            Log.d("CriticalSettingsActivity", "Device admin disabled")
                        },
                        {
                            Log.d("CriticalSettingsActivity", "Password verification failed")
                            Toast.makeText(this@CriticalSettingsActivity, "Password verification failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("CriticalSettingsActivity", "Error checking password: ${e.message}", e)
                    Toast.makeText(this@CriticalSettingsActivity, "Error checking password", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("CriticalSettingsActivity", "Device admin is not active, enabling device admin")
            enableDeviceAdmin()
        }
    }


    private fun enableDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You need to enable this to prevent uninstallation.")
        }
        startActivityForResult(intent, RESULT_ENABLE)
    }

    private fun disableDeviceAdmin() {
        try {
            Log.d("CriticalSettingsActivity", "Attempting to remove device admin")
            devicePolicyManager.removeActiveAdmin(compName)
            _isAdminActive.value = false
            Log.d("CriticalSettingsActivity", "Device admin removed successfully")
            Toast.makeText(this, "Uninstall protection disabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("CriticalSettingsActivity", "Error disabling uninstall protection: ${e.message}")
            Toast.makeText(this, "Error disabling uninstall protection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setDefaultPassword() {
        val intent = Intent(this, CreatePasswordActivity::class.java).apply {
            putExtra("setDefaultPassword", true)
        }
        startActivity(intent)
    }

    private fun startEmailSettingsActivity() {
        startActivity(Intent(this, EmailSettingsActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("CriticalSettingsActivity", "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")
        when (requestCode) {
            RESULT_ENABLE -> {
                if (resultCode == RESULT_OK) {
                    Log.d("CriticalSettingsActivity", "RESULT_ENABLE returned RESULT_OK")
                    Toast.makeText(this, "Uninstall protection enabled", Toast.LENGTH_SHORT).show()
                    _isAdminActive.value = true
                } else {
                    Toast.makeText(this, "Failed to enable uninstall protection", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_LOCK_SCREEN -> {
                if (resultCode == RESULT_OK) {
                    Log.d("CriticalSettingsActivity", "REQUEST_CODE_LOCK_SCREEN returned RESULT_OK")

                    val passwordType = data?.getStringExtra("chosenApp")
                    if (passwordType != null) {
                        Log.d("CriticalSettingsActivity", "Password type: $passwordType")
                        when (passwordType) {
                            "uninstall_protection" -> {
                                Log.d("CriticalSettingsActivity", "Calling disableDeviceAdmin()")
                                disableDeviceAdmin()
                            }
                            "delete_all_passwords" -> {
                                Log.d("CriticalSettingsActivity", "Calling deletePasswords()")
                                deletePasswords()
                            }
                        }
                    } else {
                        Log.d("CriticalSettingsActivity", "Password type is null")
                    }
                } else {
                    Log.d("CriticalSettingsActivity", "REQUEST_CODE_LOCK_SCREEN returned result code: $resultCode")
                    Toast.makeText(this, "Password verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    @Composable
    fun BoxWithLayout(content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,

        ) {
            content()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CriticalOptionsScreen(
        onDeletePasswords: () -> Unit,
        onToggleUninstallProtection: () -> Unit,
        isAdminActive: Boolean,
        onSetDefaultPasswords: () -> Unit,
        recoverySettings: () -> Unit
    ) {
        var showToastMessage by remember { mutableStateOf<String?>(null) }


        Text(
            "Critical Settings",
            fontSize = 34.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )

        BoxWithLayout {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {


                Text(
                    "Set a default password for the critical functionalities below",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(240.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(3.dp))
                CustomButton(
                    text = "Set Options Password",
                    onClick = onSetDefaultPasswords,
                    modifier = Modifier
                        .height(56.dp)
                        .width(142.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Enable to protect InLocker from being uninstalled",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(240.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(3.dp))
                CustomButton(
                    text = if (isAdminActive) "Disable Uninstall Protection" else "Enable Uninstall Protection",
                    onClick = onToggleUninstallProtection,
                    modifier = Modifier
                        .height(56.dp)
                        .width(152.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Delete all passwords, this will clean the system from all passwords",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(240.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(3.dp))
                CustomButton(
                    text = "Delete All Passwords",
                    onClick = onDeletePasswords,
                    modifier = Modifier
                        .height(56.dp)
                        .width(152.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Set an email address for password recovery and theft protection",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(240.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(3.dp))
                CustomButton(
                    text = "EmailSettings",
                    onClick = recoverySettings,
                    modifier = Modifier
                        .height(56.dp)
                        .width(152.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            showToastMessage?.let { message ->
                ShowToast(message)
                showToastMessage = null
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun CriticalOptionsScreenPreview() {
        InLockerTheme {
            CriticalOptionsScreen(
                onDeletePasswords = { },
                onToggleUninstallProtection = { },
                isAdminActive = false,
                onSetDefaultPasswords = { },
                recoverySettings = {}
            )
        }
    }
}
