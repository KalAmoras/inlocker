package com.kalsys.inlocker


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.launch

class CriticalInstructionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InLockerTheme {
                CriticalInstructionScreens()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun CriticalInstructionScreens() {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> InstructionPage(
                        "Critical Settings Help",
                        "First, you need to set a password for the critical settings functionalities. " +
                                "This button will set a default password for all the functionalities.",
                        "Malicious users might want to uninstall this app. If you enable this functionality," +
                                " the app will become permanent and won't uninstall. Don't forget to set a password for your device Settings (on Set Apps Password)",
                        "",
                        "",
                        image1 = R.drawable.set_passwords,
                        image2 =  R.drawable.uninstall_protection,
                        )
                    1 -> InstructionPage(
                        "Critical Settings Help",
                        "If you want to delete all the passwords, press this button." +
                                " This will delete every password set in this app (apps and functionalities)",
                        "Lastly, access email settings to configure your password recovery and theft protection.",
                        "",
                        "",
                        image1 = R.drawable.delete_passwords,
                        image2 = R.drawable.email_settings,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    Button(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                if (pagerState.currentPage < 1) {
                    Button(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = {
                        finish()
                    }) {
                        Text("Got it")
                    }
                }
            }
        }
    }

    @Composable
    fun InstructionPage(
        title: String,
        description1: String,
        description2: String,
        description3: String,
        description4: String,
        image1: Int?,
        image2: Int?,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = description1,
                modifier = Modifier.width(310.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                softWrap = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            image1?.let { painterResource(id = it) }?.let {
                Image(
                    painter = it,
                    contentDescription = "Instruction Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.width(310.dp),
                text = description2,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                softWrap = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            image2?.let { painterResource(id = it) }?.let {
                Image(
                    painter = it,
                    contentDescription = "Instruction Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.width(310.dp),
                text = description3,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                softWrap = true

            )
            Spacer(modifier = Modifier.height(180.dp))
            Text(
                modifier = Modifier.width(310.dp),
                text = description4,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                softWrap = true
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun InstructionScreensPreview() {
        InLockerTheme {
            CriticalInstructionScreens()
        }
    }
}

