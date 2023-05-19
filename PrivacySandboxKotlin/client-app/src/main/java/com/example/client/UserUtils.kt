package com.example.client

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.webkit.WebSettings
import com.example.client.AdvertisingInfo.initAdvertisingProfile
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
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
            builder.append(" (Linux; Android ").append(getVersionRelease()).append("; ")
                .append(getModel()).append(" Build/").append(getBuildId()).append("; wv)")
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
                builder.append(" ").append(
                    if (appInfo.labelRes == 0) appInfo.nonLocalizedLabel.toString() else context.getString(
                        appInfo.labelRes
                    )
                ).append("/").append(packageInfo.versionName)
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
        if (!isPermissionGranted(context, ACCESS_FINE_LOCATION) && !isPermissionGranted(
                context,
                ACCESS_COARSE_LOCATION
            )
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
                    TAG, "getLocation: failed to retrieve GPS location: permission not granted", e
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
            permission, android.os.Process.myPid(), android.os.Process.myUid()
        )
    }

    fun getUtcOffset() = TimeUnit.MILLISECONDS.toMinutes(
        TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
    ).toInt()

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

    //DeviceData
    fun isConnected(context: Context): Boolean {
        val connectivityManager = getConnectivityManager(context)
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    fun getOsBuildVersion(): String {
        return Build.DISPLAY
    }

    fun isDeviceRooted(): Boolean {
        try {
            val paths = arrayOf(
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            for (path in paths) {
                if (File(path).exists()) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "isDeviceRooted: ", e)
        }
        return false
    }

    fun getRamUsed(): Long {
        try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            return memInfo.totalPss * 1024L
        } catch (e: Throwable) {
            Log.e(TAG, "getRamUsed: ", e)
        }
        return 0
    }

    /**
     * Get cpu usage
     *
     * @return cpu usage in the range from 0 to 1.
     */
    fun getCpuUsage(): Float {
        try {
            val coreCount = getNumCores()
            var freqSum = 0f
            var minFreqSum = 0f
            var maxFreqSum = 0f
            for (i in 0 until coreCount) {
                freqSum += getCurCpuFreq(i)
                minFreqSum += getMinCpuFreq(i)
                maxFreqSum += getMaxCpuFreq(i)
            }
            return getAverageClock(freqSum, minFreqSum, maxFreqSum)
        } catch (e: Throwable) {
            Log.e(TAG, "getCpuUsage: ", e)
        }
        return 0F
    }

    /**
     * Calculate cpu usage.
     *
     * @return cpu usage in the range from 0 to 1.
     */
    private fun getAverageClock(
        currentFreqSum: Float,
        minFreqSum: Float,
        maxFreqSum: Float
    ): Float {
        if (maxFreqSum - minFreqSum <= 0) {
            return 0F
        }
        return if (maxFreqSum >= 0) {
            (currentFreqSum - minFreqSum) / (maxFreqSum - minFreqSum)
        } else {
            0F
        }
    }

    /**
     * Get number of cores using contents of the system folder /sys/devices/system/cpu/
     *
     * @return core count of CPU.
     */
    private fun getNumCores(): Int {
            try {
                val dir = File("/sys/devices/system/cpu/")
                val files = dir.listFiles { pathname -> Pattern.matches("cpu[0-9]", pathname.name) }
                return files?.size ?: Runtime.getRuntime().availableProcessors()
            } catch (e: Throwable) {
                Log.e(TAG, "getNumCores: ", e)
            }
        return 0
    }

    /**
     * Get current frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq
     *
     * @return current frequency of core in Hz
     */
    private fun getCurCpuFreq(coreNum: Int): Float {
        val path = String.format("/sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq", coreNum)
        return readIntegerFile(path)
    }

    /**
     * Get max frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_max_freq
     *
     * @return max frequency of core in Hz
     */
    private fun getMaxCpuFreq(coreNum: Int): Float {
        val path = String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_max_freq", coreNum)
        return readIntegerFile(path)
    }

    /**
     * Get current frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq
     *
     * @return current frequency of core in Hz
     */
    private fun getMinCpuFreq(coreNum: Int): Float {
        val path = String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_min_freq", coreNum)
        return readIntegerFile(path)
    }

    private fun readIntegerFile(filePath: String): Float {
        runCatching {
            val fileInputStream = FileInputStream(filePath)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader, 1024)
            val line: String = bufferedReader.readLine()
            if (line.isNotEmpty()) {
                return line.toFloat()
            }
        }.onFailure {
            Log.e(TAG, "readIntegerFile: ", it)
        }
        return 0f
    }

    fun getTotalFreeRam(context: Context): Long {
        try {
            return getMemoryInfo(context).availMem
        } catch (e: Throwable) {
            Log.e(TAG, "getTotalFreeRam: ", e)
        }
        return 0
    }

    fun getAppRamSize(context: Context): Long {
        try {
            return getMemoryInfo(context).totalMem
        } catch (e: Throwable) {
            Log.e(TAG, "getAppRamSize: ", e)
        }
        return 0
    }

    fun getStorageFree(): Long {
        try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            return stat.availableBlocks.toLong() * stat.blockSize.toLong()
        } catch (e: Throwable) {
            Log.e(TAG, "getStorageFree: ", e)
        }
        return 0
    }

    fun getStorageSize(): Long {
        try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            return stat.blockCountLong * stat.blockSizeLong
        } catch (e: Throwable) {
            Log.e(TAG, "getStorageSize: ", e)
        }
        return 0
    }

    fun getDeviceName(context: Context): String {
        return Settings.Global.getString(context.contentResolver, "device_name");
    }

    fun getLowRamMemoryStatus(context: Context): Boolean {
        var isLowMemory = false
        try {
            isLowMemory = getMemoryInfo(context).lowMemory
        } catch (e: Throwable) {
            Log.e(TAG, "getLowRamMemoryStatus:", e)
        }
        return isLowMemory
    }

    private fun getActivityManager(context: Context): ActivityManager {
        return context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager: ActivityManager = getActivityManager(context)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    fun isDeviceEmulator(): Boolean {
        return (isGoogleEmulator() || Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith(
            "unknown"
        ) || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") || Build.MODEL.contains(
            "Android SDK built for x86"
        ) || Build.MANUFACTURER.contains("Genymotion") || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith(
            "generic"
        )) || "google_sdk" == Build.PRODUCT
    }

    private fun isGoogleEmulator(): Boolean {
        try {
            val `object` = getStaticObjectByName(Build::class.java, "IS_EMULATOR")
            if (`object` is Boolean) {
                return `object`
            }
        } catch (ignore: Throwable) {
        }
        return false
    }

    @Throws(
        NoSuchFieldException::class,
        SecurityException::class,
        java.lang.IllegalArgumentException::class,
        IllegalAccessException::class
    )
    fun getStaticObjectByName(clazz: Class<*>, name: String?): Any? {
        val field: Field = clazz.getDeclaredField(name)
        field.isAccessible = true
        return if (field.isAccessible) {
            field.get(null)
        } else null
    }

    fun getTimeZone(): String {
        return TimeZone.getDefault().id;
    }

    fun getTimeStamp(): Long {
        return System.currentTimeMillis()
    }

    //App Data
    fun getTargetSdkVersion(context: Context): String {
        return context.applicationInfo.targetSdkVersion.toString()
    }
}