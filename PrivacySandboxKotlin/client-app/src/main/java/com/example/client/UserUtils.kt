package com.example.client

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.webkit.WebSettings
import com.example.client.AdvertisingInfo.initAdvertisingProfile
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.sqrt


object UserUtils {

    private const val TAG = "UserUtils"
    fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.defaultDisplay
        val metrics = context.resources.displayMetrics
        val realSize = Point()
        display.getRealSize(realSize)
        display.getMetrics(metrics)
        val width = (realSize.x / metrics.xdpi).toDouble().pow(2.0)
        val height = (realSize.y / metrics.ydpi).toDouble().pow(2.0)
        val screenInches = sqrt(width + height)
        return screenInches > 7.0
    }

    private var cachedHttpAgentString: String? = null

    fun getHttpAgentString(context: Context): String? {
        if (cachedHttpAgentString != null) {
            return cachedHttpAgentString
        }
        try {
            cachedHttpAgentString = WebSettings.getDefaultUserAgent(context)
        } catch (e: Throwable) {
            Log.e(TAG, "getHttpAgentString: ", e)
        }
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = generateHttpAgentString(context)
        }
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = getSystemHttpAgentString()
        }
        // We shouldn't try to obtain http agent string again after all possible methods has failed
        if (cachedHttpAgentString == null) {
            cachedHttpAgentString = ""
        }
        return cachedHttpAgentString
    }

    private fun generateHttpAgentString(context: Context): String? {
        try {
            val builder: StringBuilder = StringBuilder("Mozilla/5.0")
            builder.append(" (Linux; Android ")
                .append(getVersionRelease())
                .append("; ")
                .append(getModel())
                .append(" Build/")
                .append(getBuildId())
                .append("; wv)")
            // This AppleWebKit version supported from Chrome 68, and it's probably should for for
            // most devices
            builder.append(" AppleWebKit/537.36 (KHTML, like Gecko)")
            // This version is provided starting from Android 4.0
            builder.append(" Version/4.0")
            val pm: PackageManager = context.packageManager
            try {
                val pi: PackageInfo? = pm.getPackageInfo("com.google.android.webview", 0)
                builder.append(" Chrome/").append(pi?.versionName)
            } catch (e: Throwable) {
                Log.e(TAG, "generateHttpAgentString: ", e)
            }
            builder.append(" Mobile")
            try {
                val appInfo: ApplicationInfo = context.applicationInfo
                val packageInfo: PackageInfo = pm.getPackageInfo(context.packageName, 0)
                builder.append(" ")
                    .append(
                        if (appInfo.labelRes == 0) appInfo.nonLocalizedLabel.toString() else context.getString(
                            appInfo.labelRes
                        )
                    )
                    .append("/")
                    .append(packageInfo.versionName)
            } catch (e: Throwable) {
                Log.e(TAG, "generateHttpAgentString: ", e)
            }
            return builder.toString()
        } catch (e: Throwable) {
            return null
        }
    }

    private fun getSystemHttpAgentString(): String? {
        var result: String? = null
        try {
            result = System.getProperty("http.agent", "")
        } catch (e: Throwable) {
            Log.e(TAG, "getSystemHttpAgentString: ", e)
        }
        return result
    }

    private fun getModel(): String {
        return Build.MODEL
    }

    private fun getBuildId(): String {
        return Build.ID
    }

    private fun getVersionRelease(): String {
        return Build.VERSION.RELEASE
    }

    fun initAdProfile(context: Context) {
        initAdvertisingProfile(context)
    }
    @JvmStatic
    val deviceAdvertisingId: String
        get() = AdvertisingInfo.adProfile.id

    @JvmStatic
    val isDeviceAdvertisingIdWasGenerated: Boolean
        get() = AdvertisingInfo.adProfile.isAdvertisingIdWasGenerated

    @JvmStatic
    val isLimitAdTrackingEnabled: Boolean
        get() = AdvertisingInfo.adProfile.isLimitAdTrackingEnabled

    @SuppressLint("MissingPermission")
    fun getLocation(context: Context): Location? {
        if (!isPermissionGranted(context, ACCESS_FINE_LOCATION)
            && !isPermissionGranted(context, ACCESS_COARSE_LOCATION)
        ) {
            return null
        }
        val locationManager: LocationManager = getLocationManager(context)
        val bestProvider = locationManager.getBestProvider(Criteria(), false)
        var location: Location? = null
        if (bestProvider != null) {
            try {
                location = locationManager.getLastKnownLocation(bestProvider)
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "getLocation: failed to retrieve GPS location: permission not granted",
                    e
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    TAG,
                    "getLocation: failed to retrieve GPS location: device has no GPS provider",
                    e
                )
            }
        }
        return location
    }

    private fun getLocationManager(context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkPermission(
            permission,
            android.os.Process.myPid(),
            android.os.Process.myUid()
        )
    }

    fun getUtcOffset() =
        TimeUnit.MILLISECONDS
            .toMinutes(TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong())
            .toInt()

    @SuppressLint("MissingPermission")
    fun getConnectionData(context: Context): ConnectionData {
        val info: NetworkInfo? = getConnectivityManager(context).activeNetworkInfo
        var connectionType: String? = "unknown"
        var connectionSubtype: String? = null
        var fast = false
        if (info != null && info.isConnected) {
            connectionType = info.typeName
            connectionSubtype = info.subtypeName
            fast = when (info.type) {
                ConnectivityManager.TYPE_MOBILE -> when (info.subtype) {
                    TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> false
                    TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_LTE -> true
                    TelephonyManager.NETWORK_TYPE_UNKNOWN -> false
                    else -> false
                }

                ConnectivityManager.TYPE_WIFI -> true
                ConnectivityManager.TYPE_WIMAX -> true
                ConnectivityManager.TYPE_ETHERNET -> true
                ConnectivityManager.TYPE_BLUETOOTH -> false
                else -> false
            }
        }
        if (connectionType != null) {
            if ((connectionType == "CELLULAR")) {
                connectionType = "MOBILE"
            }
            connectionType = connectionType.lowercase()
        }
        if (connectionSubtype != null) {
            connectionSubtype = connectionSubtype.lowercase()
            if (connectionSubtype.isEmpty()) {
                connectionSubtype = null
            }
        }
        return ConnectionData(connectionType, connectionSubtype, fast)
    }

    private fun getConnectivityManager(context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun getMccmnc(context: Context): String? {
        val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkOperator = tel.networkOperator
        return return if (networkOperator.length >= 3) {
            networkOperator.substring(0, 3) + '-' + networkOperator.substring(3)
        } else {
            null
        }
    }

    fun getScreenSize(context: Context): Point {
        val display: Display = getWindowManager(context).defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    /**
     * Get width of screen using [DisplayMetrics].
     *
     * @return width of screen in pixels.
     */
    fun getScreenWidthInDp(context: Context): Float {
        val display: Display = getWindowManager(context).defaultDisplay
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val size = Point()
        display.getSize(size)
        return size.x / displayMetrics.density
    }

    /**
     * Get hight of screen using [DisplayMetrics].
     *
     * @return height of screen in pixels.
     */
    fun getScreenHeightInDp(context: Context): Float {
        val display: Display = getWindowManager(context).defaultDisplay
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val size = Point()
        display.getSize(size)
        return size.y / displayMetrics.density
    }

    /**
     * Get density of screen of device using [DisplayMetrics].
     *
     * @return floating-point zoom level.
     */
    fun getScreenDensity(context: Context): Float {
        val display: Display = getWindowManager(context).defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        return displayMetrics.density
    }

    private fun getWindowManager(context: Context): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun getOsVersion(): String = Build.VERSION.RELEASE

    fun getBrandName(): String = Build.MANUFACTURER

    fun getModelName(): String = Build.MODEL
    fun getOs() = "Android"

    fun getDeviceLanguage(): String {
        return Locale.getDefault().language
    }

    fun getAppBundle(context: Context): String = context.packageName
    fun getAppVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName

    fun getAppName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }

    fun getCarrier(context: Context): String {
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return manager.networkOperatorName
    }
}