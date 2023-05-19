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
        Log.d(TAG, "________________________SDK Runtime Side_______________________")
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
        isConnected()
        getOsBuildVersion()
        isRooted()
        getRamUsed()
        getCpuUsage()
        getRamFree()
        getAppRamSize()
        getStorageFree()
        getStorageSize()
        getDeviceName()
        getLowRamMemoryStatus()
        isEmulator()
        getTimeZone()
        getTimeStamp()
        getTargetSdkVersion()
    }

    private fun getDeviceType() {
        try {
            val device = if (isTablet(sdkContext)) "Tablet" else "Phone"
            Log.d(TAG, "getDeviceType: $device")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceType", e)

        }
    }

    private fun getUserAgent() {
        try {
            val userAgent = getHttpAgentString(sdkContext)
            Log.d(TAG, "getUserAgent: $userAgent")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getUserAgent", e)
        }
    }

    private fun getAdvertisingIdfa() {
        try {
            val idfa = deviceAdvertisingId
            Log.d(TAG, "getAdvertisingIdfa: $idfa")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getAdvertisingIdfa", e)
        }
    }

    private fun getIfv() {
        try {
            val idfv = isDeviceAdvertisingIdWasGenerated
            Log.d(TAG, "getIfv: $idfv")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getIfv", e)
        }
    }

    private fun getLimitAdTracking() {
        try {
            val limitAdTracking = isLimitAdTrackingEnabled
            Log.d(TAG, "getLimitAdTracking: $limitAdTracking")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getLimitAdTracking", e)
        }
    }

    private fun getLocation() {
        try {
            val location = SdkUtils.getLocation(sdkContext)
            Log.d(TAG, "getLocation: lat: ${location?.latitude} lon: ${location?.longitude}")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getLocation", e)
        }
    }

    private fun getUtcOffset() {
        try {
            val utcOffset = SdkUtils.getUtcOffset()
            Log.d(TAG, "getUtcOffset: $utcOffset")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getUtcOffset", e)
        }
    }

    private fun getConnectionTime() {
        try {
            val connectionType = SdkUtils.getConnectionData(sdkContext)
            Log.d(TAG, "getConnectionTime: ${connectionType.type}")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getConnectionTime", e)
        }
    }

    private fun getMccmnc() {
        try {
            val mccmnc = SdkUtils.getMccmnc(sdkContext)
            Log.d(TAG, "getMccmnc: $mccmnc")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getMccmnc", e)
        }
    }

    private fun getCarrier() {
        try {
            val carrier = SdkUtils.getCarrier(sdkContext)
            Log.d(TAG, "getCarrier: $carrier")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getCarrier", e)
        }
    }

    private fun getDisplayWidth() {
        try {
            val displayWidth = getScreenWidthInDp(sdkContext)
            Log.d(TAG, "getDisplayWidth: $displayWidth")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDisplayWidth", e)
        }
    }

    private fun getDisplayHeight() {
        try {
            val displayHeight = getScreenHeightInDp(sdkContext)
            Log.d(TAG, "getDisplayHeight: $displayHeight")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDisplayHeight", e)
        }
    }

    private fun getDisplayPxRatio() {
        try {
            val displayPxRatio = getScreenDensity(sdkContext)
            Log.d(TAG, "getDisplayPxRatio: $displayPxRatio")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDisplayPxRatio", e)
        }
    }

    private fun getDisplayPpi() {
        try {
            val displayPpi = getScreenSize(sdkContext)
            Log.d(TAG, "getDisplayPpi: $displayPpi")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDisplayPpi", e)
        }
    }

    private fun getOs() {
        try {
            val os = SdkUtils.getOs()
            Log.d(TAG, "getOs: $os")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getOs", e)
        }
    }

    private fun getOsVersion() {
        try {
            val osv = SdkUtils.getOsVersion()
            Log.d(TAG, "getOsVersion: $osv")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getOsVersion", e)
        }
    }

    private fun getDeviceHardwareVersion() {
        try {
            val deviceHardwareVersion = ", e"
            Log.d(TAG, "getDeviceHardwareVersion: $deviceHardwareVersion")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceHardwareVersion", e)
        }
    }

    private fun getDeviceMake() {
        try {
            val deviceMake = getBrandName()
            Log.d(TAG, "getDeviceMake: $deviceMake")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceMake", e)
        }
    }

    private fun getDeviceModel() {
        try {
            val deviceModel = getModelName()
            Log.d(TAG, "getDeviceModel: $deviceModel")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceModel", e)
        }
    }

    private fun getDeviceLanguage() {
        try {
            val deviceLanguage = SdkUtils.getDeviceLanguage()
            Log.d(TAG, "getDeviceLanguage: $deviceLanguage")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceLanguage", e)
        }
    }

    private fun getAppBundle() {
        try {
            val bundle = SdkUtils.getAppBundle(sdkContext)
            Log.d(TAG, "getAppBundle: $bundle")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getAppBundle", e)
        }
    }

    private fun getAppVersion() {
        try {
            val appVersion = SdkUtils.getAppVersion(sdkContext)
            Log.d(TAG, "getAppVersion: $appVersion")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getAppVersion", e)
        }
    }

    private fun getAppName() {
        try {
            val appName = SdkUtils.getAppName(sdkContext)
            Log.d(TAG, "getAppName: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getAppName", e)
        }
    }

    @Throws(RemoteException::class)
    override fun getData() {
        Log.d(TAG, ", getData:")
        printData()
    }

    private fun isConnected() {
        try {
            val isConnected = SdkUtils.isConnected(sdkContext)
            Log.d(TAG, "isConnected: $isConnected")
        } catch (e: Exception) {
            Log.e(TAG, "exception during isConnected", e)
        }
    }

    private fun getOsBuildVersion() {
        try {
            val osBuildVersion = SdkUtils.getOsBuildVersion()
            Log.d(TAG, "getOsBuildVersion: $osBuildVersion")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getOsBuildVersion", e)
        }
    }

    private fun isRooted() {
        try {
            val isRooted = SdkUtils.isDeviceRooted()
            Log.d(TAG, "isRooted: $isRooted")
        } catch (e: Exception) {
            Log.e(TAG, "exception during isRooted", e)
        }
    }

    private fun getRamUsed() {
        try {
            val ramUsed = SdkUtils.getRamUsed()
            Log.d(TAG, "ramUsed: $ramUsed")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getRamUsed", e)
        }
    }

    private fun getCpuUsage() {
        try {
            val cpuUsage = SdkUtils.getCpuUsage()
            Log.d(TAG, "getCpuUsage: $cpuUsage")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getCpuUsage", e)
        }
    }

    private fun getRamFree() {
        try {
            val ramFree = SdkUtils.getTotalFreeRam(sdkContext)
            Log.d(TAG, "getRamFree: $ramFree")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getRamFree", e)
        }
    }

    private fun getAppRamSize() {
        try {
            val appRam = SdkUtils.getAppRamSize(sdkContext)
            Log.d(TAG, "getAppRamSize: $appRam")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getAppRamSize", e)
        }
    }

    private fun getStorageFree() {
        try {
            val storageFree = SdkUtils.getStorageFree()
            Log.d(TAG, "getStorageFree: $storageFree")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getStorageFree", e)
        }
    }

    private fun getStorageSize() {
        try {
            val storageSize = SdkUtils.getStorageSize()
            Log.d(TAG, "getStorageSize: $storageSize")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getStorageSize", e)
        }
    }

    private fun getDeviceName() {
        try {
            val deviceName = SdkUtils.getDeviceName(sdkContext)
            Log.d(TAG, "getDeviceName: $deviceName")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getDeviceName", e)
        }
    }

    private fun getLowRamMemoryStatus() {
        try {
            val lowMemory = SdkUtils.getLowRamMemoryStatus(sdkContext)
            Log.d(TAG, "getLowRamMemoryStatus: $lowMemory")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getLowRamMemoryStatus", e)
        }
    }

    private fun isEmulator() {
        try {
            val isEmulator = SdkUtils.isDeviceEmulator()
            Log.d(TAG, "isEmulator: $isEmulator")
        } catch (e: Exception) {
            Log.e(TAG, "exception during isEmulator", e)
        }
    }

    private fun getTimeZone() {
        try {
            val timezone = SdkUtils.getTimeZone()
            Log.d(TAG, "getTimeZone: $timezone")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getTimeZone", e)
        }
    }

    private fun getTimeStamp() {
        try {
            val timestamp = SdkUtils.getTimeStamp()
            Log.d(TAG, "getTimeStamp: $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getTimeStamp", e)
        }
    }

    private fun getTargetSdkVersion() {
        try {
            val targetSdkVersion = SdkUtils.getTargetSdkVersion(sdkContext)
            Log.d(TAG, "getTargetSdkVersion: $targetSdkVersion")
        } catch (e: Exception) {
            Log.e(TAG, "exception during getTargetSdkVersion", e)
        }
    }
}
