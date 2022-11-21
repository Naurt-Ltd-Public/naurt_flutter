package com.naurt.flutter_naurt_sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat

import kotlinx.coroutines.*

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.EventChannel

import com.naurt.Naurt as Sdk
import com.naurt.NaurtEvents.*
import com.naurt.NaurtDeviceReport
import com.naurt.NaurtEventListener
import com.naurt.NaurtNewLocationEvent
import com.naurt.*


/** FlutterNaurtSdk */
class FlutterNaurtSdk : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private var locationUpdateEventSink: EventChannel.EventSink? = null
    private lateinit var mainHandler: Handler

    private var unorderedScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private lateinit var naurtLocationListener: NaurtEventListener<NaurtNewLocationEvent>
    private lateinit var naurtOnlineListener: NaurtEventListener<NaurtIsOnlineEvent>
    private lateinit var naurtIsInitialisedListener: NaurtEventListener<NaurtIsInitialisedEvent>
    private lateinit var naurtIsValidatedListener: NaurtEventListener<NaurtIsValidatedEvent>
    private lateinit var naurtNewJourneyListener: NaurtEventListener<NaurtNewJourneyEvent>
    private lateinit var naurtRunningListener: NaurtEventListener<NaurtIsRunningEvent>
    private lateinit var naurtHasLocationProviderListener: NaurtEventListener<NaurtHasLocationProviderEvent>
    private lateinit var naurtNewTrackingStatusListener: NaurtEventListener<NaurtNewTrackingStatusEvent>
    private lateinit var naurtNewDeviceReportListener: NaurtEventListener<NaurtNewDeviceReportEvent>

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET
    )

    /** Check to see if the given context has been granted all permissions in the input array */
    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("naurt", "Missing permission: $permission")
                    return false
                }
            }
        }
        return true
    }

    private fun mapLocation(loc: NaurtLocation): Map<String, Any> {
        return mapOf(
            "latitude" to loc.latitude,
            "longitude" to loc.longitude,
            "timestamp" to loc.timestamp,
            "horizontalAccuracy" to loc.horizontalAccuracy,
            "speed" to loc.speed,
            "heading" to loc.heading,
            "speedAccuracy" to loc.speedAccuracy,
            "headingAccuracy" to loc.headingAccuracy,
            "horizontalCovariance" to loc.horizontalCovariance,
            "altitude" to loc.altitude,
            "verticalAccuracy" to loc.verticalAccuracy,
        )
    }

    private fun mapDeviceReport(rep: NaurtDeviceReport): Map<String, Any> {
        val pn = rep.processName ?: "null"
        val lm = rep.wasLastLocationMocked ?: "null"
        val hma = rep.hasMockingAppsInstalled ?: "null"

        return mapOf(
            "hasMockingAppsInstalled" to hma,
            "isDeveloper" to rep.isDeveloper,
            "isDeviceRooted" to rep.isDeviceRooted,
            "isInWorkProfile" to rep.isInWorkProfile,
            "lastReportChange" to rep.lastReportChange,
            "processName" to pn,
            "wasLastLocationMocked" to lm
        )
    }

    private fun stringifyTrackingStatus(status: NaurtTrackingStatus): String {
        when (status) {
            NaurtTrackingStatus.ALREADY_RUNNING -> return "ALREADY_RUNNING"
            NaurtTrackingStatus.COMPROMISED -> return "COMPROMISED"
            NaurtTrackingStatus.DEGRADED -> return "DEGRADED"
            NaurtTrackingStatus.FULL -> return "FULL"
            NaurtTrackingStatus.INOPERABLE -> return "INOPERABLE"
            NaurtTrackingStatus.INVALID -> return "INVALID"
            NaurtTrackingStatus.LOCATION_NOT_ENABLED -> return "LOCATION_NOT_ENABLED"
            NaurtTrackingStatus.MINIMAL -> return "MINIMAL"
            NaurtTrackingStatus.NOT_INITIALISED -> return "NOT_INITIALISED"
            NaurtTrackingStatus.NOT_RUNNING -> return "NOT_RUNNING"
            NaurtTrackingStatus.NO_PERMISSION -> return "NO_PERMISSION"
            NaurtTrackingStatus.PAUSED -> return "PAUSED"
            NaurtTrackingStatus.STOPPED -> return "STOPPED"
            NaurtTrackingStatus.UNKNOWN -> return "UNKNOWN"
        }
    }

    private fun addListeners() {
        naurtLocationListener = NaurtEventListener<NaurtNewLocationEvent> { p0 ->
            locationUpdateEventSink?.success(mapLocation(p0.newPoint));
        }
        Sdk.on(NaurtEvents.NEW_LOCATION, naurtLocationListener)

        naurtOnlineListener = NaurtEventListener<NaurtIsOnlineEvent> { p0 ->

        }
        Sdk.on(NaurtEvents.IS_ONLINE, naurtOnlineListener)

        naurtIsInitialisedListener = NaurtEventListener<NaurtIsInitialisedEvent> { p0 ->
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onInitialisation", p0.isInitialised)
            }
        }
        Sdk.on(NaurtEvents.IS_INITIALISED, naurtIsInitialisedListener)

        naurtIsValidatedListener = NaurtEventListener<NaurtIsValidatedEvent> { p0 ->
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onValidation", p0.isValidated)
            }
        }
        Sdk.on(NaurtEvents.IS_VALIDATED, naurtIsValidatedListener)

        naurtNewJourneyListener = NaurtEventListener<NaurtNewJourneyEvent> { p0 ->

        }
        Sdk.on(NaurtEvents.NEW_JOURNEY, naurtNewJourneyListener)

        naurtRunningListener = NaurtEventListener<NaurtIsRunningEvent> { p0 ->
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onRunning", p0.isRunning)
            }
        }
        Sdk.on(NaurtEvents.IS_RUNNING, naurtRunningListener)

        naurtHasLocationProviderListener = NaurtEventListener<NaurtHasLocationProviderEvent> { p0 ->

        }
        Sdk.on(NaurtEvents.HAS_LOCATION_PROVIDER, naurtHasLocationProviderListener)

        naurtNewTrackingStatusListener = NaurtEventListener<NaurtNewTrackingStatusEvent> { p0 ->
            Handler(Looper.getMainLooper()).post {
                val toSend = stringifyTrackingStatus(p0.status)
                channel.invokeMethod("onTrackingStatus", toSend)
            }
        }
        Sdk.on(NaurtEvents.NEW_TRACKING_STATUS, naurtNewTrackingStatusListener)

        naurtNewDeviceReportListener = NaurtEventListener<NaurtNewDeviceReportEvent> { p0 ->
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onDeviceReport", mapDeviceReport(p0.naurtDeviceReport))
            }
        }
        Sdk.on(NaurtEvents.NEW_DEVICE_REPORT, naurtNewDeviceReportListener)
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mainHandler = Handler(flutterPluginBinding.applicationContext.mainLooper);
        applicationContext = flutterPluginBinding.applicationContext;

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_naurt_sdk")
        val locationUpdateEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "flutter_naurt_sdk/locationChanged")
        channel.setMethodCallHandler(this)
        locationUpdateEventChannel.setStreamHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull rawResult: MethodChannel.Result) {
        mainHandler.post {
            when (call.method) {
                "initialise" -> {
                    if (!hasPermissions(applicationContext, permissions)) {
                        Log.e("naurt", "Naurt does not have permission to run!")
                    }

                    addListeners()

                    val successInit = Sdk.initialiseService(
                        call.argument<String>("apiKey")!!,
                        applicationContext
                    )
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(successInit)
                }

                "isValidated" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(Sdk.getValidated())
                }
                "isRunning" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(Sdk.getRunning())
                }
                "isInitialised" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(Sdk.getInitialised())
                }
                "naurtPoint" -> {
                    val lastLocation = Sdk.getLocation()
                    if (lastLocation != null) {
                        val result: MethodChannel.Result = MainThreadResult(rawResult)
                        result.success(mapLocation(lastLocation))
                    } else {
                        val result: MethodChannel.Result = MainThreadResult(rawResult)
                        result.success(null)
                    }
                }
                "naurtPoints" -> {
                    val locationsMap =
                        Sdk.getLocationHistory().map { location -> mapLocation(location) }
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(locationsMap)
                }
                "journeyUuid" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(Sdk.getJourneyUuid().toString())
                }
                "start" -> {
                    val startStatus = Sdk.start().get()


                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(stringifyTrackingStatus(startStatus))

                }
                "stop" -> {
                    val stopStatus = Sdk.stop().get()
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(stringifyTrackingStatus(stopStatus))

                }
                "trackingStatus" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.success(stringifyTrackingStatus(Sdk.getTrackingStatus()))
                }
                "deviceReport" -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)

                    if (Sdk.getDeviceReport() != null) {
                        result.success(mapDeviceReport(Sdk.getDeviceReport()!!))
                    } else {
                        result.success(null)
                    }
                }
                else -> {
                    val result: MethodChannel.Result = MainThreadResult(rawResult)
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        mainHandler.post {
            channel.setMethodCallHandler(null)
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        mainHandler.post {
            if (events != null) {
                locationUpdateEventSink = MainThreadEventSink(events) as EventChannel.EventSink
            }
        }
    }

    override fun onCancel(arguments: Any?) {
        mainHandler.post {
            locationUpdateEventSink = null
        }
    }
}
