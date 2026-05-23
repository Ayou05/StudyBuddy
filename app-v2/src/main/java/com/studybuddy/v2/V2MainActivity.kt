package com.studybuddy.v2

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.theme.StudyBuddyV2Theme
import com.studybuddy.v2.ui.component.SaddleSplashOverlay
import com.studybuddy.v2.ui.nav.V2NavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class V2MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            val token by prefs.pbToken.collectAsState(initial = null)
            val dark by prefs.darkTheme.collectAsState(initial = false)
            val unlockedSaddle by prefs.unlockedSaddleCat.collectAsState(initial = false)
            var splashDone by remember { mutableStateOf(false) }
            StudyBuddyV2Theme(darkTheme = dark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    V2NavGraph(isLoggedIn = token != null)
                    SaddleSplashOverlay(
                        visible = unlockedSaddle && !splashDone,
                        onFinish = { splashDone = true }
                    )
                }
            }
        }
    }
}
