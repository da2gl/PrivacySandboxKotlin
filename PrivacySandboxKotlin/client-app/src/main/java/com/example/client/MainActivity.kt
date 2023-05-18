/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.client

import android.annotation.SuppressLint
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.RequestSurfacePackageException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.app.sdksandbox.SdkSandboxManager.EXTRA_DISPLAY_ID
import android.app.sdksandbox.SdkSandboxManager.EXTRA_HEIGHT_IN_PIXELS
import android.app.sdksandbox.SdkSandboxManager.EXTRA_HOST_TOKEN
import android.app.sdksandbox.SdkSandboxManager.EXTRA_SURFACE_PACKAGE
import android.app.sdksandbox.SdkSandboxManager.EXTRA_WIDTH_IN_PIXELS
import android.app.sdksandbox.SdkSandboxManager.SdkSandboxProcessDeathCallback
import android.os.*
import android.util.Log
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.client.UserUtils.getBrandName
import com.example.client.UserUtils.getHttpAgentString
import com.example.client.UserUtils.getModelName
import com.example.client.UserUtils.getScreenDensity
import com.example.client.UserUtils.getScreenHeightInDp
import com.example.client.UserUtils.getScreenSize
import com.example.client.UserUtils.getScreenWidthInDp
import com.example.client.UserUtils.initAdProfile
import com.example.client.UserUtils.isDeviceAdvertisingIdWasGenerated
import com.example.client.UserUtils.isLimitAdTrackingEnabled
import com.example.client.UserUtils.isTablet
import com.example.exampleaidllibrary.ISdkApi
import com.example.privacysandbox.client.R

@SuppressLint("NewApi")
class MainActivity : AppCompatActivity() {
    /**
     * Button to load the SDK to the sandbox.
     */
    private lateinit var mLoadSdkButton: Button

    /**
     * Button to request a SurfacePackage from sandbox which remotely render a webview.
     */
    private lateinit var mRequestWebViewButton: Button

    /**
     * Button to create a file inside sandbox.
     */
    private lateinit var mCreateFileButton: Button

    /**
     * An instance of SdkSandboxManager which contains APIs to communicate with the sandbox.
     */
    private lateinit var mSdkSandboxManager: SdkSandboxManager

    /**
     * The SurfaceView which will be used by the client app to show the SurfacePackage
     * going to be rendered by the sandbox.
     */
    private lateinit var mClientView: SurfaceView

    /**
     * This object is going to be set when SDK is successfully loaded. It is a wrapper for the
     * public SDK API Binder object defined by SDK by implementing the AIDL file from
     * example-aidl-library module.
     */
    private lateinit var mSandboxedSdk : SandboxedSdk

    private var mSdkLoaded = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSdkSandboxManager = applicationContext.getSystemService(
            SdkSandboxManager::class.java
        )
        mClientView = findViewById(R.id.rendered_view)
        mClientView.setZOrderOnTop(true)
        mLoadSdkButton = findViewById(R.id.load_sdk_button)
        mRequestWebViewButton = findViewById(R.id.request_webview_button)
        mCreateFileButton = findViewById(R.id.create_file_button)
        registerLoadCodeProviderButton()
        registerRequestWebViewButton()
        registerCreateFileButton()
        initAdProfile(this)
    }

    /**
     * Register the callback action after once mLoadSdkButton got clicked.
     */
    private fun registerLoadCodeProviderButton() {
        mLoadSdkButton.setOnClickListener { _: View? ->
            // Register for sandbox death event.
            mSdkSandboxManager.addSdkSandboxProcessDeathCallback(
                { obj: Runnable -> obj.run() }, SdkSandboxProcessDeathCallbackImpl())
            log("Attempting to load sandbox SDK")
            val callback = LoadSdkCallbackImpl()
            printData()
            mSdkSandboxManager.loadSdk(
                SDK_NAME, Bundle(), { obj: Runnable -> obj.run() }, callback
            )
        }
    }

    /**
     * Register the callback action after once mRequestWebViewButton got clicked.
     */
    private fun registerRequestWebViewButton() {
        mRequestWebViewButton.setOnClickListener {
            if (!mSdkLoaded) {
                makeToast("Please load the SDK first!")
                return@setOnClickListener
            }
            log("Getting SurfacePackage.")
            Handler(Looper.getMainLooper()).post {
                val params = Bundle()
                params.putInt(EXTRA_WIDTH_IN_PIXELS, mClientView.width)
                params.putInt(EXTRA_HEIGHT_IN_PIXELS, mClientView.height)
                params.putInt(EXTRA_DISPLAY_ID, display?.displayId!!)
                params.putBinder(EXTRA_HOST_TOKEN, mClientView.hostToken)
                mSdkSandboxManager.requestSurfacePackage(
                    SDK_NAME, params, { obj: Runnable -> obj.run() }, RequestSurfacePackageCallbackImpl())
            }
        }
    }

    /**
     * Register the callback action after once mCreateFileButton got clicked.
     */
    private fun registerCreateFileButton() {
        mCreateFileButton.setOnClickListener { _ ->
            if (!mSdkLoaded) {
                makeToast("Please load the SDK first!")
                return@setOnClickListener
            }
            log("Creating file inside sandbox.")
            val binder: IBinder? = mSandboxedSdk.getInterface()
            val sdkApi = ISdkApi.Stub.asInterface(binder)
            sdkApi.getData()
            makeToast("Done")
        }
    }

    /**
     * A callback for tracking events regarding loading an SDK.
     */
    private inner class LoadSdkCallbackImpl : OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        /**
         * This notifies client application that the requested SDK is successfully loaded.
         *
         * @param sandboxedSdk a [SandboxedSdk] is returned from the sandbox to the app.
         */
        @SuppressLint("Override")
        override fun onResult(sandboxedSdk: SandboxedSdk) {
            log("SDK is loaded")
            makeToast("Loaded successfully!")
            mSdkLoaded = true
            mSandboxedSdk = sandboxedSdk;
        }

        /**
         * This notifies client application that the requested Sdk failed to be loaded.
         *
         * @param error a [LoadSdkException] containing the details of failing to load the
         * SDK.
         */
        @SuppressLint("Override")
        override fun onError(error: LoadSdkException) {
            log("onLoadSdkFailure(" + error.loadSdkErrorCode.toString() + "): " + error.message)
            makeToast("Load SDK Failed! " + error.message)
        }
    }

    /**
     * A callback for tracking Sdk Sandbox process death event.
     */
    private inner class SdkSandboxProcessDeathCallbackImpl : SdkSandboxProcessDeathCallback {
        /**
         * Notifies the client application that the SDK sandbox has died. The sandbox could die for
         * various reasons, for example, due to memory pressure on the system, or a crash in the
         * sandbox.
         *
         * The system will automatically restart the sandbox process if it died due to a crash.
         * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
         * would have to be loaded again, using [SdkSandboxManager.loadSdk] to continue using them.
         */
        @SuppressLint("Override")
        override fun onSdkSandboxDied() {
            makeToast("Sdk Sandbox process died")
        }
    }

    /**
     * A callback for tracking a request for a surface package from an SDK.
     */
    private inner class RequestSurfacePackageCallbackImpl :
        OutcomeReceiver<Bundle?, RequestSurfacePackageException?> {
        /**
         * This notifies client application that [SurfacePackage]
         * is ready to remote render view from the SDK.
         *
         * @param response a [Bundle] which should contain the key EXTRA_SURFACE_PACKAGE with
         * a value of [SurfacePackage] response.
         */
        @SuppressLint("Override")
        override fun onResult(response: Bundle) {
            log("Surface package ready")
            makeToast("Surface Package Rendered!")
            Handler(Looper.getMainLooper()).post {
                log("Setting surface package in the client view")
                val surfacePackage: SurfacePackage? = response.getParcelable(
                    EXTRA_SURFACE_PACKAGE, SurfacePackage::class.java)
                mClientView.setChildSurfacePackage(surfacePackage!!)
                mClientView.visibility = View.VISIBLE
            }
        }

        /**
         * This notifies client application that requesting [SurfacePackage] has failed.
         *
         * @param error a [RequestSurfacePackageException] containing the details of failing
         * to request the surface package.
         */
        @SuppressLint("Override")
        override fun onError(error: RequestSurfacePackageException) {
            log("onSurfacePackageError" + error.requestSurfacePackageErrorCode
                .toString() + "): "
                  + error.message)
            makeToast("Surface Package Failed! " + error.message)
        }
    }

    private fun makeToast(message: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
    }

    private fun log(message: String) {
        Log.e(TAG, message)
    }

    companion object {
        private const val TAG = "SandboxClient"

        /**
         * Name of the SDK to be loaded.
         */
        private const val SDK_NAME = "com.example.privacysandbox.provider"
    }

    private fun printData() {
        Log.d(TAG,"________________________User App Side_______________________")
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
        val device = if (isTablet(this)) "Tablet" else "Phone"
        Log.d(TAG,"getDeviceType: $device")
    }
    private fun getUserAgent() {
        val userAgent = getHttpAgentString(this)
        Log.d(TAG,"getUserAgent: $userAgent")
    }
    private fun getAdvertisingIdfa() {
        val idfa = UserUtils.deviceAdvertisingId
        Log.d(TAG,"getAdvertisingIdfa: $idfa")
    }
    private fun getIfv() {
        val idfv = isDeviceAdvertisingIdWasGenerated
        Log.d(TAG,"getIfv: $idfv")
    }
    private fun getLimitAdTracking() {
       val limitAdTracking = isLimitAdTrackingEnabled
        Log.d(TAG,"getLimitAdTracking: $limitAdTracking")
    }
    private fun getLocation() {
        val location = UserUtils.getLocation(this)
        Log.d(TAG,"getLocation: lat: ${location?.latitude} lon: ${location?.longitude}")
    }
    private fun getUtcOffset() {
       val utcOffset = UserUtils.getUtcOffset()
        Log.d(TAG,"getUtcOffset: $utcOffset")
    }
    private fun getConnectionTime() {
       val connectionType = UserUtils.getConnectionData(this)
        Log.d(TAG,"getConnectionTime: ${connectionType.type}")
    }
    private fun getMccmnc() {
        val mccmnc = UserUtils.getMccmnc(this)
        Log.d(TAG,"getMccmnc: $mccmnc")
    }
    private fun getCarrier() {
       val carrier = UserUtils.getCarrier(this)
        Log.d(TAG,"getCarrier: $carrier")
    }
    private fun getDisplayWidth() {
       val displayWidth = getScreenWidthInDp(this)
        Log.d(TAG,"getDisplayWidth: $displayWidth")
    }
    private fun getDisplayHeight() {
        val displayHeight = getScreenHeightInDp(this)
        Log.d(TAG,"getDisplayHeight: $displayHeight")
    }
    private fun getDisplayPxRatio() {
       val displayPxRatio = getScreenDensity(this)
        Log.d(TAG,"getDisplayPxRatio: $displayPxRatio")
    }
    private fun getDisplayPpi() {
       val displayPpi = getScreenSize(this)
        Log.d(TAG,"getDisplayPpi: $displayPpi")
    }
    private fun getOs() {
        val os = UserUtils.getOs()
        Log.d(TAG,"getOs: $os")
    }
    private fun getOsVersion() {
       val osv = UserUtils.getOsVersion()
        Log.d(TAG,"getOsVersion: $osv")
    }
    private fun getDeviceHardwareVersion() {
       val deviceHardwareVersion = ""
        Log.d(TAG,"getDeviceHardwareVersion: $deviceHardwareVersion")
    }
    private fun getDeviceMake() {
        val deviceMake = getBrandName()
        Log.d(TAG,"getDeviceMake: $deviceMake")
    }
    private fun getDeviceModel() {
       val deviceModel = getModelName()
        Log.d(TAG,"getDeviceModel: $deviceModel")
    }
    private fun getDeviceLanguage() {
       val deviceLanguage = UserUtils.getDeviceLanguage()
        Log.d(TAG,"getDeviceLanguage: $deviceLanguage")
    }
    private fun getAppBundle() {
        val bundle = UserUtils.getAppBundle(this)
        Log.d(TAG,"getAppBundle: $bundle")
    }
    private fun getAppVersion() {
       val appVersion = UserUtils.getAppVersion(this)
        Log.d(TAG,"getAppVersion: $appVersion")
    }
    private fun getAppName() {
       val appName = UserUtils.getAppName(this)
        Log.d(TAG,"getAppName: $appName")
    }
}
