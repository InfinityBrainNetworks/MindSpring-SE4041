package com.mindspring.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindspring.app.data.repository.AuthRepository
import com.mindspring.app.data.repository.AuthResult
import com.mindspring.app.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val agreed: Boolean = false,
    val showErrors: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
) {
    val nameError get() = Validators.nameError(name)
    val emailError get() = Validators.emailError(email)
    val passwordError get() = Validators.passwordError(password)
    val confirmError get() = Validators.confirmError(password, confirm)
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, error = null) }
    fun onAgreed(v: Boolean) = _state.update { it.copy(agreed = v, error = null) }

    fun fillDemo() = _state.update { it.copy(email = "asan@mindspring.app", password = "mindspring", error = null) }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return
        }
        submit { auth.login(s.email, s.password) }
    }

    fun register() {
        val s = _state.value
        val invalid = listOf(s.nameError, s.emailError, s.passwordError, s.confirmError).any { it != null }
        if (invalid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        if (!s.agreed) {
            _state.update { it.copy(error = "Please agree to the privacy policy to continue.") }
            return
        }
        submit { auth.register(s.name, s.email, s.password) }
    }

    private fun submit(call: suspend () -> AuthResult) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is AuthResult.Success -> _state.update { it.copy(loading = false, signedIn = true) }
                is AuthResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
