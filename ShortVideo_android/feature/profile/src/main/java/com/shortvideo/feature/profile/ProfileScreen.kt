package com.shortvideo.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)
        listOf(
            "Avatar, display name, bio",
            "My uploaded videos grid",
            "Edit / delete / visibility (Phase 5)",
        ).forEach { section ->
            Text(text = "• $section", style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Account settings")
        }
        Text(
            text = "Logout and account delete live in Settings.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
