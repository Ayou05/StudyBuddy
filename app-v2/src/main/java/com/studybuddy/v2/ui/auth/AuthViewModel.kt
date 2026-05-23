package com.studybuddy.v2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.pb.PbException
import com.studybuddy.v2.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null
)

enum class AuthMode { LOGIN, REGISTER }

sealed class AuthEvent {
    object LoggedIn : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun setMode(mode: AuthMode) = _state.update { it.copy(mode = mode, errorMessage = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v) }
    fun onNicknameChange(v: String) = _state.update { it.copy(nickname = v) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(errorMessage = "请填写邮箱和密码") }
            return
        }
        if (s.password.length < 8) {
            _state.update { it.copy(errorMessage = "密码至少 8 位") }
            return
        }
        if (s.mode == AuthMode.REGISTER && s.nickname.isBlank()) {
            _state.update { it.copy(errorMessage = "请填写昵称") }
            return
        }
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (s.mode == AuthMode.LOGIN) {
                    authRepo.login(s.email, s.password)
                } else {
                    authRepo.register(s.email, s.password, s.nickname)
                }
                _events.tryEmit(AuthEvent.LoggedIn)
            } catch (e: PbException) {
                val msg = when {
                    e.status == 400 && s.mode == AuthMode.LOGIN -> "邮箱或密码错误"
                    e.status == 400 -> "注册失败：${e.pbMessage}"
                    e.status == 401 -> "邮箱或密码错误"
                    else -> e.pbMessage.ifBlank { "登录失败 (${e.status})" }
                }
                _state.update { it.copy(loading = false, errorMessage = msg) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = "网络异常：${e.message ?: "未知错误"}")
                }
            }
            _state.update { it.copy(loading = false) }
        }
    }
}
