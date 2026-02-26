package com.ar.arstoken.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.ar.arstoken.data.db.BusinessProfileDao
import com.ar.arstoken.data.db.BusinessProfileEntity
import com.ar.arstoken.model.BusinessProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class BusinessProfileRepository(
    private val dao: BusinessProfileDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private fun docRef(uid: String) =
        firestore.collection("users").document(uid).collection("profile").document("profile")

    fun observeProfile(): Flow<BusinessProfile?> =
        dao.observe().map { it?.toModel() }

    fun startRemoteSync(uid: String, onError: (String) -> Unit): ListenerRegistration {
        return docRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Failed to sync profile.")
                return@addSnapshotListener
            }
            val profile = snapshot?.toObject(BusinessProfile::class.java) ?: return@addSnapshotListener
            ioScope.launch {
                dao.upsert(profile.toEntity())
            }
        }
    }

    fun saveProfile(uid: String, profile: BusinessProfile, onError: (String) -> Unit) {
        ioScope.launch { dao.upsert(profile.toEntity()) }
        docRef(uid)
            .set(profile, SetOptions.merge())
            .addOnFailureListener { onError(it.message ?: "Failed to save profile.") }
    }

    fun uploadLogo(
        context: Context,
        uid: String,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val processedUri = createCroppedSquarePng(context, uri) ?: run {
            onError("Unable to process image.")
            return
        }
        val ref = storage.reference.child("users/$uid/logo_${System.currentTimeMillis()}.png")
        ref.putFile(processedUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                onSuccess(downloadUrl.toString())
            }
            .addOnFailureListener { onError(it.message ?: "Logo upload failed.") }
    }

    private fun createCroppedSquarePng(context: Context, uri: Uri): Uri? {
        return try {
            val resolver = context.contentResolver
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: return null

            val size = min(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)

            val file = File(context.cacheDir, "logo_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}

private fun BusinessProfileEntity.toModel(): BusinessProfile =
    BusinessProfile(
        businessName = businessName,
        logoUrl = logoUrl,
        phone = phone,
        updatedAt = updatedAt
    )

private fun BusinessProfile.toEntity(): BusinessProfileEntity =
    BusinessProfileEntity(
        businessName = businessName,
        logoUrl = logoUrl,
        phone = phone,
        updatedAt = updatedAt
    )
