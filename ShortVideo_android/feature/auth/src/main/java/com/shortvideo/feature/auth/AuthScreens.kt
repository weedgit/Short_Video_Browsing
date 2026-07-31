package com.shortvideo.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPasswordReset: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.clearSuccess()
            onLoginSuccess()
        }
    }

    AuthFormScaffold(
        title = "Sign in",
        subtitle = "Use your ShortVideo account to continue.",
        errorMessage = uiState.errorMessage,
        isLoading = uiState.isLoading,
        primaryButtonText = "Sign in",
        onPrimaryClick = viewModel::login,
        secondaryActionText = "Create account",
        onSecondaryClick = onNavigateToRegister,
        tertiaryActionText = "Forgot password?",
        onTertiaryClick = onNavigateToPasswordReset,
    ) {
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !uiState.isLoading,
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !uiState.isLoading,
        )
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.clearSuccess()
            onRegisterSuccess()
        }
    }

    AuthFormScaffold(
        title = "Create account",
        subtitle = "Register to upload and sync your profile.",
        errorMessage = uiState.errorMessage,
        isLoading = uiState.isLoading,
        primaryButtonText = "Register",
        onPrimaryClick = viewModel::register,
        secondaryActionText = "Already have an account?",
        onSecondaryClick = onNavigateToLogin,
    ) {
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !uiState.isLoading,
        )
        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            enabled = !uiState.isLoading,
        )
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = viewModel::onDisplayNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Display name") },
            singleLine = true,
            enabled = !uiState.isLoading,
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !uiState.isLoading,
        )
    }
}

@Composable
private fun AuthFormScaffold(
    title: String,
    subtitle: String,
    errorMessage: String?,
    isLoading: Boolean,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryActionText: String,
    onSecondaryClick: () -> Unit,
    tertiaryActionText: String? = null,
    onTertiaryClick: (() -> Unit)? = null,
    fields: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        fields()
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(primaryButtonText)
            }
        }
        TextButton(
            onClick = onSecondaryClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !isLoading,
        ) {
            Text(secondaryActionText)
        }
        if (tertiaryActionText != null && onTertiaryClick != null) {
            TextButton(
                onClick = onTertiaryClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !isLoading,
            ) {
                Text(tertiaryActionText)
            }
        }
    }
}
