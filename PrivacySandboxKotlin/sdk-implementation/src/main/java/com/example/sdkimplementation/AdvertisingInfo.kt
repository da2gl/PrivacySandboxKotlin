package com.example.sdkimplementation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.regex.Pattern

sealed interface AdvertisingInfoState {
    object NotInitialized : AdvertisingInfoState
    object Initializing : AdvertisingInfoState
    data class Initialized(val advertisingProfile: AdvertisingInfo.AdvertisingProfile) :
        AdvertisingInfoState
}

object AdvertisingInfo {

    const val defaultAdvertisingId: String = "00000000-0000-0000-0000-000000000000"

    private val supportedAdvertisingProfiles = listOf(
        GoogleAdvertisingProfile(),
        DefaultAdvertisingProfile
    )

    private val state = MutableStateFlow<AdvertisingInfoState>(AdvertisingInfoState.NotInitialized)

    suspend fun getAdvertisingProfile(context: Context): AdvertisingProfile =
        withContext(Dispatchers.IO) {
            if (state.compareAndSet(
                    expect = AdvertisingInfoState.NotInitialized,
                    update = AdvertisingInfoState.Initializing
                )
            ) {
                fetchAdvertisingProfile(context)
            }
            val profile =
                state.first { it is AdvertisingInfoState.Initialized } as? AdvertisingInfoState.Initialized
            return@withContext profile?.advertisingProfile ?: getDefaultProfile(context)
        }

    suspend fun fetchAdvertisingProfile(context: Context) = withContext(Dispatchers.IO) {
        state.value = AdvertisingInfoState.Initializing
        state.value = supportedAdvertisingProfiles.firstNotNullOfOrNull { profile ->
            try {
                if (profile.isEnabled(context)) {
                    profile.extractParams(context)
                    AdvertisingInfoState.Initialized(profile)
                } else {
                    null
                }
            } catch (throwable: Throwable) {
                null
            }
        } ?: AdvertisingInfoState.Initialized(getDefaultProfile(context))
    }

    private fun getDefaultProfile(context: Context) =
        DefaultAdvertisingProfile.apply { extractParams(context) }

    abstract class AdvertisingProfile {
        var id: String = defaultAdvertisingId
            protected set
        var isLimitAdTrackingEnabled = false
            protected set
        var isAdvertisingIdWasGenerated = false
            protected set

        @Throws(Throwable::class)
        internal open fun isEnabled(context: Context): Boolean = true

        @Throws(Throwable::class)
        internal open fun extractParams(context: Context) {
            if (isLimitAdTrackingEnabled || id == defaultAdvertisingId || id.isBlank() || !id.isAdId()) {
                id = getUUID(context)
                isAdvertisingIdWasGenerated = true
            }
        }

        internal open fun getUUID(context: Context): String {
            val sharedPref = context.getSharedPreferences("appodeal", Context.MODE_PRIVATE)
            return sharedPref.getString("uuid", null)
                ?: UUID.randomUUID().toString().also {
                    val editor = sharedPref.edit()
                    editor.putString("uuid", it)
                    editor.apply()
                }
        }

        override fun toString(): String {
            return "${this.javaClass.simpleName}(id='$id', isLimitAdTrackingEnabled=$isLimitAdTrackingEnabled, isAdvertisingIdWasGenerated=$isAdvertisingIdWasGenerated)"
        }
    }

    object DefaultAdvertisingProfile : AdvertisingProfile()

    private class GoogleAdvertisingProfile : AdvertisingProfile() {

        @Throws(Throwable::class)
        override fun isEnabled(context: Context): Boolean {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            return true
        }

        @Throws(Throwable::class)
        override fun extractParams(context: Context) {
            val info = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                .getDeclaredMethod("getAdvertisingIdInfo", Context::class.java)
                .invoke(null, context)
            val infoClass =
                Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info")
            id = infoClass.getDeclaredMethod("getId").invoke(info) as String
            isLimitAdTrackingEnabled = infoClass
                .getDeclaredMethod("isLimitAdTrackingEnabled")
                .invoke(info) as Boolean
            super.extractParams(context)
        }
    }
}

private val adIdPattern = Pattern.compile("[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
private fun String.isAdId(): Boolean = adIdPattern.matcher(this).matches()