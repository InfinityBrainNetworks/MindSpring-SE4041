package com.mindspring.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindspring.app.ui.appViewModel
import com.mindspring.app.ui.components.GhostButton
import com.mindspring.app.ui.components.MsCard
import com.mindspring.app.ui.components.MsTextField
import com.mindspring.app.ui.components.PrimaryButton
import com.mindspring.app.ui.components.Wordmark
import com.mindspring.app.ui.theme.Dimens
import com.mindspring.app.ui.theme.MsTheme

@Composable
fun LoginScreen(onSignedIn: () -> Unit, onCreateAccount: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.auth) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors
    var showForgot by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.signedIn) { if (s.signedIn) onSignedIn() }

    AuthScaffold {
        Spacer(Modifier.height(Dimens.stackLg))
        Wordmark(style = MaterialTheme.typography.headlineLarge, markSize = 44.dp)
        Spacer(Modifier.height(Dimens.stackLg))

        MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackMd)) {
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.titleLarge,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            MsTextField(
                value = s.email,
                onValueChange = vm::onEmail,
                label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            MsTextField(
                value = s.password,
                onValueChange = vm::onPassword,
                label = "Password",
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Text(
                "Forgot password?",
                style = MaterialTheme.typography.labelMedium,
                color = c.tealInk,
                modifier = Modifier.align(Alignment.End).clickable { showForgot = true }.padding(4.dp),
            )
            ErrorText(s.error)
            SubmitButton("Log In", loading = s.loading, onClick = vm::login)
        }

        Spacer(Modifier.height(Dimens.stackLg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("New here?", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            GhostButton("Create account", onClick = onCreateAccount)
        }
        // UI-phase convenience: the in-memory store ships with a sample account.
        GhostButton("Use the demo account", onClick = vm::fillDemo, color = c.textTertiary)
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            confirmButton = { TextButton(onClick = { showForgot = false }) { Text("Got it") } },
            title = { Text("Forgot password?") },
            text = {
                Text(
                    "MindSpring keeps your account on this device only, so there is no online reset. " +
                        "If you can't sign in, you can create a new account.",
                )
            },
        )
    }
}

@Composable
fun RegisterScreen(onSignedIn: () -> Unit, onBack: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.auth) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = MsTheme.colors

    LaunchedEffect(s.signedIn) { if (s.signedIn) onSignedIn() }

    AuthScaffold {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.tealInk)
            }
        }
        Spacer(Modifier.height(Dimens.stackMd))
        Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = c.tealInk)
        Spacer(Modifier.height(Dimens.stackSm))
        Text("Join MindSpring and start your journey.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(Dimens.stackLg))

        MsCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.stackSm)) {
            val next = KeyboardOptions(imeAction = ImeAction.Next)
            MsTextField(s.name, vm::onName, label = "Full Name", isError = s.showErrors && s.nameError != null, errorText = s.nameError, keyboardOptions = next)
            MsTextField(
                s.email, vm::onEmail, label = "Email Address",
                isError = s.showErrors && s.emailError != null, errorText = s.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            MsTextField(s.password, vm::onPassword, label = "Password", isPassword = true, isError = s.showErrors && s.passwordError != null, errorText = s.passwordError, keyboardOptions = next)
            MsTextField(s.confirm, vm::onConfirm, label = "Confirm Password", isPassword = true, isError = s.showErrors && s.confirmError != null, errorText = s.confirmError)

            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                Checkbox(
                    checked = s.agreed,
                    onCheckedChange = vm::onAgreed,
                    colors = CheckboxDefaults.colors(checkedColor = c.tealInk, uncheckedColor = c.textTertiary),
                )
                Column(Modifier.padding(top = 12.dp)) {
                    Text("I agree to the privacy policy.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    Text(
                        "Note: All your data stays on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = c.tealInk,
                    )
                }
            }
            ErrorText(s.error)
            Spacer(Modifier.height(Dimens.stackSm))
            SubmitButton("Create Account", loading = s.loading, onClick = vm::register)
        }

        Spacer(Modifier.height(Dimens.stackMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            GhostButton("Sign In", onClick = onBack)
        }
    }
}

@Composable
private fun AuthScaffold(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MsTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screen, vertical = Dimens.stackMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(error, style = MaterialTheme.typography.bodySmall, color = MsTheme.colors.danger)
    }
}

@Composable
private fun SubmitButton(text: String, loading: Boolean, onClick: () -> Unit) {
    if (loading) {
        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = MsTheme.colors.amber, modifier = Modifier.size(28.dp))
        }
    } else {
        PrimaryButton(text, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}
