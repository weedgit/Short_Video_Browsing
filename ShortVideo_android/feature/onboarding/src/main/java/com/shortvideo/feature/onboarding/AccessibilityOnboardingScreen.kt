package com.shortvideo.feature.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shortvideo.core.DestinationRoute
import com.shortvideo.theme.ShortVideoTheme

@Composable
fun AccessibilityOnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: AccessibilityOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus(onCompleted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ShortVideoTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Accessibility service notice",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "This app requires the ShortVideo Assist accessibility service to run.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Bullet("Purpose: improve in-app navigation and assistive playback controls.")
            Bullet("Accessible data: on-screen text labels and tap events within this app.")
            Bullet("Server transfer: accessibility data is not sent to our servers.")
            Bullet("Storage: not stored on device beyond active session processing.")
            Bullet("Retention: no long-term retention of accessibility event content.")
            Bullet("You can disable the service anytime in Android Settings.")
            Bullet("Enable \"ShortVideo Assist\" in Android accessibility settings.")
            Bullet("The app cannot start until the service is enabled.")

            if (uiState.hasOpenedSettings) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current status: ${uiState.serviceStatus.toLabel()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.serviceStatus != AccessibilityServiceStatus.Enabled) {
                    Text(
                        text = "ShortVideo Assist must be enabled before you can continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.hasOpenedSettings) {
                Button(
                    onClick = {
                        viewModel.onSettingsOpened()
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Agree and open settings")
                }
            } else {
                if (uiState.serviceStatus != AccessibilityServiceStatus.Enabled) {
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open settings again")
                    }
                }

                Button(
                    onClick = { viewModel.completeOnboarding(onCompleted) },
                    enabled = uiState.serviceStatus == AccessibilityServiceStatus.Enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue to app")
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun AccessibilityServiceStatus.toLabel(): String = when (this) {
    AccessibilityServiceStatus.Enabled -> "Enabled"
    AccessibilityServiceStatus.Disabled -> "Disabled"
    AccessibilityServiceStatus.Unknown -> "Unknown"
}

fun NavGraphBuilder.accessibilityOnboardingNavGraph(
    onCompleted: () -> Unit,
) {
    composable(DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE) {
        AccessibilityOnboardingScreen(onCompleted = onCompleted)
    }
}
