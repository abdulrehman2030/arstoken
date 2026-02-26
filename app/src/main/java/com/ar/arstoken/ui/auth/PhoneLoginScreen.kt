package com.ar.arstoken.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    onSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }

    var countryCode by rememberSaveable { mutableStateOf("+91") }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { onSignedIn() }
                    .addOnFailureListener { errorMessage = it.message }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                errorMessage = e.localizedMessage ?: "Verification failed."
                isSending = false
                isVerifying = false
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = id
                isSending = false
                statusMessage = "Code sent. Please enter the OTP."
            }
        }
    }

    fun sendCode() {
        errorMessage = null
        statusMessage = null
        if (activity == null) {
            errorMessage = "Activity not available."
            return
        }
        val fullNumber = "${countryCode.trim()}${phone.trim()}"
        if (!countryCode.trim().startsWith("+") || phone.trim().length < 6) {
            errorMessage = "Enter a valid phone number."
            return
        }
        isSending = true
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode() {
        errorMessage = null
        statusMessage = null
        val id = verificationId
        if (id.isNullOrBlank()) {
            errorMessage = "Please request the OTP first."
            return
        }
        if (code.trim().length < 4) {
            errorMessage = "Enter the OTP sent to your phone."
            return
        }
        isVerifying = true
        val credential = PhoneAuthProvider.getCredential(id, code.trim())
        auth.signInWithCredential(credential)
            .addOnSuccessListener { onSignedIn() }
            .addOnFailureListener {
                isVerifying = false
                errorMessage = it.message ?: "Verification failed."
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Login with phone number",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it },
                    label = { Text("Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 72.dp, max = 96.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = { sendCode() },
                enabled = !isSending,
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Text(if (isSending) "Sending..." else "Send OTP")
            }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { verifyCode() },
                enabled = !isVerifying,
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Text(if (isVerifying) "Verifying..." else "Verify & Continue")
            }

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
