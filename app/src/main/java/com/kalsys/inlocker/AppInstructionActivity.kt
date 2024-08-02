package com.kalsys.inlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kalsys.inlocker.ui.theme.InLockerTheme

class AppInstructionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InLockerTheme {
                AppInstructionScreens()
            }
        }

    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AppInstructionScreens() {
        val pagerState = rememberPagerState(pageCount = { 1 })

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
                        "App Passwords Help",
                        "Press this button to set a password for its respective app on the list.",
                        "Easy to use, the default password will be applied to all apps." +
                        " NOTE: setting a default password will override any individual app password set, and vice-versa.",
                        "",
                        "",
                        image1 = R.drawable.app_item,
                        image2 =  R.drawable.set_default_password,
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
            AppInstructionScreens()
        }
    }
}