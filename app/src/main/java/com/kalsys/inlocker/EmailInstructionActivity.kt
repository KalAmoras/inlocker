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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.ui.theme.InLockerTheme
import kotlinx.coroutines.launch

class EmailInstructionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InLockerTheme {
                EmailInstructionScreens()
            }
        }

    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun EmailInstructionScreens() {
        val pagerState = rememberPagerState(pageCount = { 1 })
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
                        "Email Settings Help",
                        "First, select an email address to receive the data this app will send you." +
                                " This email will be also the sender. Your passwords, location and pictures taken will be sent to this email.",
                        "If you wish to test your email, press this button.",
                        "If you forgot your passwords, clicking this button will send a list with all your set passwords.",
                        "",
                        image1 = R.drawable.set_recovery_email,
                        image2 =  R.drawable.send_test_email,
                        image3 = R.drawable.send_passwords,
                        )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    finish()
                }) {
                    Text("Got it")
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
        image3: Int?,
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
            image3?.let { painterResource(id = it) }?.let {
                Image(
                    painter = it,
                    contentDescription = "Instruction Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
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
            EmailInstructionScreens()
        }
    }
}

