package com.kalsys.inlocker


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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
    private lateinit var passwordChecker: PasswordChecker
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private val _isAdminActive = mutableStateOf(false)
    private val isAdminActive: State<Boolean> get() = _isAdminActive

    companion object {
        const val RESULT_ENABLE = 1
        const val REQUEST_CODE_PICK_URI = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = PasswordDatabase.getInstance(applicationContext)
        passwordDao = database.passwordDao()

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        passwordChecker = PasswordCheckerImplementation(passwordDao)
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
                    onEmailService = { handleEmailService()},
                    onSelectUri = { selectUri() }
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
                    deletePasswords()
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
                    Log.d("CriticalSettingsActivity", "Password verified, disabling device admin")
                    disableDeviceAdmin()
                    Log.d("CriticalSettingsActivity", "Device admin disabled")
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
        Toast.makeText(this, "Uninstall protection enabled", Toast.LENGTH_SHORT).show()
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

    private fun handleEmailService() {
        startActivity(Intent(this@CriticalSettingsActivity, EmailSettingsActivity::class.java))
    }

    private fun selectUri() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_URI)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_URI && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                Log.d("CriticalSettingsActivity", "Selected URI: $uri")
                Toast.makeText(this, "Selected URI: $uri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CriticalOptionsScreen(
        onDeletePasswords: () -> Unit,
        onToggleUninstallProtection: () -> Unit,
        isAdminActive: Boolean,
        onSetDefaultPasswords: () -> Unit,
        onEmailService: () -> Unit,
        onSelectUri: () -> Unit
    ) {
        var showToastMessage by remember { mutableStateOf<String?>(null) }
        val context = this@CriticalSettingsActivity

            Row(
                modifier = Modifier.padding(bottom = 10.dp)
            ){
                Text(
                    "Critical Settings",
                    fontSize = 34.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val intent = Intent(context, CriticalInstructionActivity::class.java)
                        startActivity(intent)
                    },
                    modifier = Modifier
                        .padding(end = 12.dp, top = 20.dp)
                        .size(40.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp) // Remove default padding
                ) {
                    Text(
                        text = "?",
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Set a default password for the critical functionalities: Service Switch," +
                                    "Critical Settings",
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
                        
                    }
                }
            }
            showToastMessage?.let { message ->
                ShowToast(message)
                showToastMessage = null
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
                onEmailService = {},
                onSelectUri = {}

            )
        }
    }
}
