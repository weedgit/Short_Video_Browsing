package com.shortvideo.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WireframeScreen(
    title: String,
    sections: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        sections.forEach { section ->
            Text(
                text = "• $section",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SearchWireframeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Discover", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search videos, hashtags, creators") },
            readOnly = true,
        )
        Text(text = "Trending hashtags", style = MaterialTheme.typography.titleMedium)
        listOf("#travel", "#food", "#daily").forEach { tag ->
            Text(text = tag, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
