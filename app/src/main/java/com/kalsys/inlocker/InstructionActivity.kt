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

class InstructionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InLockerTheme {
                InstructionScreens()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun InstructionScreens() {
        val pagerState = rememberPagerState(pageCount = { 4 })
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
                        "Initial configuration",
                        "For this app to work properly, some permissions need to be enabled.",
                        "This app will work on the background and will always be active when enabled." +
                                " For security reasons, some devices won't allow the automatic authorization requiring the user to allow them manually.",
                        "These first steps will guarantee the proper and secure app's functioning.",
                        "If you need this instruction again, just click the '?' button on the top-right corner.",
                        image1 = null,
                        image2 = null,
                    )
                    1 -> InstructionPage(
                        "Allow Accessibility",
                        "In Accessibility, enter Downloaded Services:",
                        "In Downloaded Services, choose InLocker and activate it",
                        "",
                        "",
                        image1 = R.drawable.accessibility,
                        image2 = R.drawable.accessibility2

                    )
                    2 -> InstructionPage(
                        "Allow Pop-up Windows",
                        "On App Management -> Other Permissions, Allow Display pop-up window and pop-up windows while running on background",
                        "On App Management, don't allow Pause App Activity if unused",
                        "",
                        "",
                        image1 = R.drawable.overlay,
                        image2 = R.drawable.permissions
                    )
                    3 -> InstructionPage(
                        "Battery saving configuration",
                        "Some operational systems sometimes kill the app because of battery saving restrictions",
                        "If your app is crashing or turning off after some time, consider turning battery saving off",
                        "",
                        "",
                        image1 = R.drawable.battery1,
                        image2 = null
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

                if (pagerState.currentPage < 3) {
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
            InstructionScreens()
        }
    }
}

