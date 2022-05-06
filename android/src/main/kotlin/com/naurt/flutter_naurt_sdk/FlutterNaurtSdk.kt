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

import com.naurt.Sdk.INSTANCE as Sdk
import com.naurt.*
import com.naurt.events.*


/** FlutterNaurtSdk */
class FlutterNaurtSdk: FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var applicationContext: Context
  private var locationUpdateEventSink: EventChannel.EventSink? = null
  private lateinit var mainHandler: Handler

  private var unorderedScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

  private lateinit var naurtLocationListener: EventListener<NaurtNewLocationEvent>
  private lateinit var naurtOnlineListener: EventListener<NaurtIsOnlineEvent>
  private lateinit var naurtIsInitialisedListener: EventListener<NaurtIsInitialisedEvent>
  private lateinit var naurtIsValidatedListener: EventListener<NaurtIsValidatedEvent>
  private lateinit var naurtNewJourneyListener: EventListener<NaurtNewJourneyEvent>
  private lateinit var naurtRunningListener: EventListener<NaurtIsRunningEvent>
  private lateinit var naurtHasLocationProviderListener: EventListener<NaurtHasLocationProviderEvent>
  private lateinit var naurtNewTrackingStatusListener: EventListener<NaurtNewTrackingStatusEvent>
  private lateinit var naurtNewDeviceReportListener: EventListener<NaurtNewDeviceReportEvent>

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

  private fun mapLocation(loc: NaurtLocation):  Map<String, Any> {
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

  private fun mapDeviceReport(rep: DeviceReport): Map<String, Any> {
    val pn = rep.processName?: "null"
    val lm = rep.wasLastLocationMocked?: "null"
    val hma = rep.hasMockingAppsInstalled?: "null"

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
    naurtLocationListener = EventListener<NaurtNewLocationEvent> { p0 ->
      locationUpdateEventSink?.success(mapLocation(p0.newPoint));
    }
    Sdk.on("NAURT_NEW_POINT", naurtLocationListener)

    naurtOnlineListener = EventListener<NaurtIsOnlineEvent> { p0 ->

    }
    Sdk.on("NAURT_IS_ONLINE", naurtOnlineListener)

    naurtIsInitialisedListener = EventListener<NaurtIsInitialisedEvent> { p0 ->
      Handler(Looper.getMainLooper()).post {
        channel.invokeMethod("onInitialisation", p0.isInitialised)
      }
    }
    Sdk.on("NAURT_IS_INITIALISED", naurtIsInitialisedListener)

    naurtIsValidatedListener = EventListener<NaurtIsValidatedEvent> { p0 ->
      Handler(Looper.getMainLooper()).post {
        channel.invokeMethod("onValidation", p0.isValidated)
      }
    }
    Sdk.on("NAURT_IS_VALIDATED", naurtIsValidatedListener)

    naurtNewJourneyListener = EventListener<NaurtNewJourneyEvent> { p0 ->

    }
    Sdk.on("NAURT_NEW_JOURNEY", naurtNewJourneyListener)

    naurtRunningListener = EventListener<NaurtIsRunningEvent> { p0 ->
      Handler(Looper.getMainLooper()).post {
        channel.invokeMethod("onRunning", p0.isRunning)
      }
    }
    Sdk.on("NAURT_IS_RUNNING", naurtRunningListener)

    naurtHasLocationProviderListener = EventListener<NaurtHasLocationProviderEvent> { p0 ->

    }
    Sdk.on("NAURT_HAS_LOCATION", naurtHasLocationProviderListener)

    naurtNewTrackingStatusListener = EventListener<NaurtNewTrackingStatusEvent> { p0 ->
      Handler(Looper.getMainLooper()).post {
        val toSend = stringifyTrackingStatus(p0.status)
        channel.invokeMethod("onTrackingStatus", toSend)
      }
    }
    Sdk.on("NAURT_NEW_TRACKING_STATUS", naurtNewTrackingStatusListener)

    naurtNewDeviceReportListener = EventListener<NaurtNewDeviceReportEvent> { p0 ->
      Handler(Looper.getMainLooper()).post {
        channel.invokeMethod("onDeviceReport", mapDeviceReport(p0.deviceReport))
      }
    }
    Sdk.on("NAURT_NEW_DEVICE_REPORT", naurtNewDeviceReportListener)
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    mainHandler = Handler(flutterPluginBinding.applicationContext.getMainLooper());
    applicationContext = flutterPluginBinding.applicationContext;

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_naurt_sdk")
    val locationUpdateEventChannel = EventChannel(flutterPluginBinding.binaryMessenger,"flutter_naurt_sdk/locationChanged")
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

          Sdk.initialise(
            call.argument<String>("apiKey")!!,
            applicationContext,
            call.argument<Int>("precision")!!
          ).thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(it)
          }
        }
        "isValidated" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(Sdk.isValidated)
        }
        "isRunning" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(Sdk.isRunning)
        }
        "isInitialised" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(Sdk.isInitialised)
        }
        "naurtPoint" -> {
          val lastLocation = Sdk.naurtPoint
          if (lastLocation != null) {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(mapLocation(lastLocation))
          } else {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(null)
          }
        }
        "naurtPoints" -> {
          val locationsMap = Sdk.naurtPoints.map { location -> mapLocation(location) }
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(locationsMap)
        }
        "journeyUuid" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(Sdk.journeyUuid.toString())
        }
        "start" -> {
          Sdk.start().thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(stringifyTrackingStatus(it))
          }
        }
        "stop" -> {
          Sdk.stop().thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(stringifyTrackingStatus(it))
          }
        }
        "pause" -> {
          Sdk.pause().thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(stringifyTrackingStatus(it))
          }
        }
        "resume" -> {
          Sdk.resume(applicationContext).thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(stringifyTrackingStatus(it))
          }
        }
        "trackingStatus" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)
          result.success(stringifyTrackingStatus(Sdk.trackingStatus))
        }
        "deviceReport" -> {
          val result: MethodChannel.Result = MainThreadResult(rawResult)

          if (Sdk.deviceReport != null) {
            result.success(mapDeviceReport(Sdk.deviceReport!!))
          }
          else {
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
