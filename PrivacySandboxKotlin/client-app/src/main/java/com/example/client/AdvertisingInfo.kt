package com.example.client

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException

object AdvertisingInfo {

    const val defaultAdvertisingId: String = "00000000-0000-0000-0000-000000000000"

    var adProfile : AdvertisingProfile = AdvertisingProfile()

    fun initAdvertisingProfile(context: Context) {
        try {
            adProfile.adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            return
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    class AdvertisingProfile {
        var id: String = defaultAdvertisingId
            private set
            get() = adInfo?.id ?: defaultAdvertisingId

        var isLimitAdTrackingEnabled = false
            private set
            get() = adInfo?.isLimitAdTrackingEnabled == true
        var isAdvertisingIdWasGenerated = false
            private set
            get() = id == defaultAdvertisingId
        var adInfo: AdvertisingIdClient.Info? = null

        override fun toString(): String {
            return "${this.javaClass.simpleName}(id='$id', isLimitAdTrackingEnabled=$isLimitAdTrackingEnabled, isAdvertisingIdWasGenerated=$isAdvertisingIdWasGenerated)"
        }
    }
}