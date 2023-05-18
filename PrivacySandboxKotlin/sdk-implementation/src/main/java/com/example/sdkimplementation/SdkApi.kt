/*
* Copyright (C) 2022 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the", eLicense");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an", eAS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example.sdkimplementation

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.example.exampleaidllibrary.ISdkApi
import com.example.sdkimplementation.SdkUtils.deviceAdvertisingId
import com.example.sdkimplementation.SdkUtils.getBrandName
import com.example.sdkimplementation.SdkUtils.getHttpAgentString
import com.example.sdkimplementation.SdkUtils.getModelName
import com.example.sdkimplementation.SdkUtils.getScreenDensity
import com.example.sdkimplementation.SdkUtils.getScreenHeightInDp
import com.example.sdkimplementation.SdkUtils.getScreenSize
import com.example.sdkimplementation.SdkUtils.getScreenWidthInDp
import com.example.sdkimplementation.SdkUtils.isDeviceAdvertisingIdWasGenerated
import com.example.sdkimplementation.SdkUtils.isLimitAdTrackingEnabled
import com.example.sdkimplementation.SdkUtils.isTablet

class SdkApi(private val sdkContext: Context) : ISdkApi.Stub() {
    companion object {
        private const val TAG = "SandboxServer"
    }
    init {
        SdkUtils.initAdProfile(sdkContext)
    }

    private fun printData() {
        Log.d(TAG,"________________________SDK Runtime Side_______________________")
        getDeviceType()
        getUserAgent()
        getAdvertisingIdfa()
        getIfv()
        getLimitAdTracking()
        getLocation()
        getUtcOffset()
        getConnectionTime()
        getMccmnc()
        getCarrier()
        getDisplayWidth()
        getDisplayHeight()
        getDisplayPxRatio()
        getDisplayPpi()
        getOs()
        getOsVersion()
        getDeviceHardwareVersion()
        getDeviceMake()
        getDeviceModel()
        getDeviceLanguage()
        getAppBundle()
        getAppVersion()
        getAppName()
    }

    private fun getDeviceType() {
        try {
            val device = if (isTablet(sdkContext)) "Tablet" else "Phone"
            Log.d(TAG,"getDeviceType: $device")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDeviceType", e)

        }
    }

    private fun getUserAgent() {
        try {
            val userAgent = getHttpAgentString(sdkContext)
            Log.d(TAG,"getUserAgent: $userAgent")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getUserAgent", e)
        }
    }

    private fun getAdvertisingIdfa() {
        try {
            val idfa = deviceAdvertisingId
            Log.d(TAG,"getAdvertisingIdfa: $idfa")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getAdvertisingIdfa", e)
        }
    }

    private fun getIfv() {
        try {
            val idfv = isDeviceAdvertisingIdWasGenerated
            Log.d(TAG,"getIfv: $idfv")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getIfv", e)
        }
    }

    private fun getLimitAdTracking() {
        try {
            val limitAdTracking = isLimitAdTrackingEnabled
            Log.d(TAG,"getLimitAdTracking: $limitAdTracking")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getLimitAdTracking", e)
        }
    }

    private fun getLocation() {
        try {
            val location = SdkUtils.getLocation(sdkContext)
            Log.d(TAG,"getLocation: lat: ${location?.latitude} lon: ${location?.longitude}")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getLocation", e)
        }
    }

    private fun getUtcOffset() {
        try {
            val utcOffset = SdkUtils.getUtcOffset()
            Log.d(TAG,"getUtcOffset: $utcOffset")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getUtcOffset", e)
        }
    }

    private fun getConnectionTime() {
        try {
            val connectionType = SdkUtils.getConnectionData(sdkContext)
            Log.d(TAG,"getConnectionTime: ${connectionType.type}")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getConnectionTime", e)
        }
    }

    private fun getMccmnc() {
        try {
            val mccmnc = SdkUtils.getMccmnc(sdkContext)
            Log.d(TAG,"getMccmnc: $mccmnc")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getMccmnc", e)
        }
    }

    private fun getCarrier() {
        try {
            val carrier = SdkUtils.getCarrier(sdkContext)
            Log.d(TAG,"getCarrier: $carrier")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getCarrier", e)
        }
    }

    private fun getDisplayWidth() {
        try {
            val displayWidth = getScreenWidthInDp(sdkContext)
            Log.d(TAG,"getDisplayWidth: $displayWidth")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDisplayWidth", e)
        }
    }

    private fun getDisplayHeight() {
        try {
            val displayHeight = getScreenHeightInDp(sdkContext)
            Log.d(TAG,"getDisplayHeight: $displayHeight")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDisplayHeight", e)
        }
    }

    private fun getDisplayPxRatio() {
        try {
            val displayPxRatio = getScreenDensity(sdkContext)
            Log.d(TAG,"getDisplayPxRatio: $displayPxRatio")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDisplayPxRatio", e)
        }
    }

    private fun getDisplayPpi() {
        try {
            val displayPpi = getScreenSize(sdkContext)
            Log.d(TAG,"getDisplayPpi: $displayPpi")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDisplayPpi", e)
        }
    }

    private fun getOs() {
        try {
            val os = SdkUtils.getOs()
            Log.d(TAG,"getOs: $os")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getOs", e)
        }
    }

    private fun getOsVersion() {
        try {
            val osv = SdkUtils.getOsVersion()
            Log.d(TAG,"getOsVersion: $osv")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getOsVersion", e)
        }
    }

    private fun getDeviceHardwareVersion() {
        try {
            val deviceHardwareVersion =", e"
            Log.d(TAG,"getDeviceHardwareVersion: $deviceHardwareVersion")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDeviceHardwareVersion", e)
        }
    }

    private fun getDeviceMake() {
        try {
            val deviceMake = getBrandName()
            Log.d(TAG,"getDeviceMake: $deviceMake")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDeviceMake", e)
        }
    }

    private fun getDeviceModel() {
        try {
            val deviceModel = getModelName()
            Log.d(TAG,"getDeviceModel: $deviceModel")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDeviceModel", e)
        }
    }

    private fun getDeviceLanguage() {
        try {
            val deviceLanguage = SdkUtils.getDeviceLanguage()
            Log.d(TAG,"getDeviceLanguage: $deviceLanguage")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getDeviceLanguage", e)
        }
    }

    private fun getAppBundle() {
        try {
            val bundle = SdkUtils.getAppBundle(sdkContext)
            Log.d(TAG,"getAppBundle: $bundle")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getAppBundle", e)
        }
    }

    private fun getAppVersion() {
        try {
            val appVersion = SdkUtils.getAppVersion(sdkContext)
            Log.d(TAG,"getAppVersion: $appVersion")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getAppVersion", e)
        }
    }

    private fun getAppName() {
        try {
            val appName = SdkUtils.getAppName(sdkContext)
            Log.d(TAG,"getAppName: $appName")
        } catch (e: Exception) {
            Log.e(TAG,"exception during getAppName", e)
        }
    }

    @Throws(RemoteException::class)
    override fun getData() {
            Log.d(TAG,", getData:")
            printData()
    }
}
