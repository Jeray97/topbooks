package com.example.topbooks.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    RegisterContent(
        isLoading = uiState.isAuthenticating,
        onRegisterClick = { name, email, pass ->
            viewModel.register(name, email, pass) {
                Toast.makeText(context, context.getString(R.string.register_toast_success), Toast.LENGTH_LONG).show()
                onRegisterSuccess()
            }
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
fun RegisterContent(
    isLoading: Boolean,
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val hasUpperCase = remember(password) { password.any { it.isUpperCase() } }
    val hasNumber = remember(password) { password.any { it.isDigit() } }
    val hasSpecialChar = remember(password) { password.any { !it.isLetterOrDigit() } }
    val isLengthValid = remember(password) { password.length >= 6 }
    val isPasswordValid = hasUpperCase && hasNumber && hasSpecialChar && isLengthValid

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = LoginColors.Surface,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, LoginColors.OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                    lineHeight = 31.sp,
                    color = LoginColors.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.register_title),
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    color = LoginColors.Primary
                )

                Spacer(modifier = Modifier.height(64.dp))

                Text(
                    text = stringResource(R.string.register_field_name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginColors.OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Arthur Conan Doyle", color = LoginColors.Outline) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = LoginColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.SecondaryContainer,
                        unfocusedBorderColor = LoginColors.OutlineVariant,
                        focusedContainerColor = LoginColors.SurfaceContainerLow,
                        unfocusedContainerColor = LoginColors.SurfaceContainerLow
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.register_field_email),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginColors.OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("arthur@example.com", color = LoginColors.Outline) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = LoginColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.SecondaryContainer,
                        unfocusedBorderColor = LoginColors.OutlineVariant,
                        focusedContainerColor = LoginColors.SurfaceContainerLow,
                        unfocusedContainerColor = LoginColors.SurfaceContainerLow
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.register_field_password),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginColors.OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••••", color = LoginColors.Outline) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LoginColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = LoginColors.Outline, modifier = Modifier.size(20.dp))
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.SecondaryContainer,
                        unfocusedBorderColor = LoginColors.OutlineVariant,
                        focusedContainerColor = LoginColors.SurfaceContainerLow,
                        unfocusedContainerColor = LoginColors.SurfaceContainerLow
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.register_field_confirm_password),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginColors.OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••••", color = LoginColors.Outline) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LoginColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = LoginColors.Outline, modifier = Modifier.size(20.dp))
                        }
                    },
                    singleLine = true,
                    isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginColors.SecondaryContainer,
                        unfocusedBorderColor = LoginColors.OutlineVariant,
                        focusedContainerColor = LoginColors.SurfaceContainerLow,
                        unfocusedContainerColor = LoginColors.SurfaceContainerLow
                    )
                )

                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LoginColors.SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.register_pwd_req_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LoginColors.Primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            PasswordReqItem(text = stringResource(R.string.register_pwd_req_length), isMet = isLengthValid)
                            PasswordReqItem(text = stringResource(R.string.register_pwd_req_uppercase), isMet = hasUpperCase)
                            PasswordReqItem(text = stringResource(R.string.register_pwd_req_number), isMet = hasNumber)
                            PasswordReqItem(text = stringResource(R.string.register_pwd_req_special), isMet = hasSpecialChar)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = LoginColors.Primary,
                        strokeWidth = 4.dp
                    )
                } else {
                    Button(
                        onClick = { onRegisterClick(name, email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword && isPasswordValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginColors.Primary,
                            contentColor = LoginColors.OnPrimary
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(stringResource(R.string.register_btn_submit), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = LoginColors.OutlineVariant)

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.register_already_have_account), color = LoginColors.OnSurfaceVariant, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.register_login_here),
                        color = LoginColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordReqItem(text: String, isMet: Boolean) {
    val color = if (isMet) LoginColors.SecondaryContainer else LoginColors.OnSurfaceVariant
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}
