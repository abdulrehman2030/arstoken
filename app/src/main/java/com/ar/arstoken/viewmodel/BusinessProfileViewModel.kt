package com.ar.arstoken.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.repository.BusinessProfileRepository
import com.ar.arstoken.model.BusinessProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.google.firebase.firestore.ListenerRegistration

class BusinessProfileViewModel(
    private val uid: String,
    private val repository: BusinessProfileRepository
) : ViewModel() {

    private var listener: ListenerRegistration? = null

    val profile: StateFlow<BusinessProfile?> = repository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startSync(onError: (String) -> Unit) {
        if (listener != null) return
        listener = repository.startRemoteSync(uid, onError)
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }

    fun saveProfile(
        businessName: String,
        logoUrl: String?,
        phone: String?,
        onError: (String) -> Unit
    ) {
        val profile = BusinessProfile(
            businessName = businessName.trim(),
            logoUrl = logoUrl?.takeIf { it.isNotBlank() },
            phone = phone,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveProfile(uid, profile, onError)
    }

    fun uploadLogo(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.uploadLogo(context, uid, uri, onSuccess, onError)
    }
}
