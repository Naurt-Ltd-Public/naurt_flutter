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
      "mockedLocation" to loc.spoofReport.mockedLocation,
      "mockAppsInstalled" to loc.spoofReport.mockAppsInstalled,
      "mockSettingActive" to loc.spoofReport.mockSettingActive
    )
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
            result.success(it)
          }
        }
        "stop" -> {
          Sdk.stop().thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(it)
          }
        }
        "pause" -> {
          Sdk.pause().thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(it)
          }
        }
        "resume" -> {
          Sdk.resume(applicationContext).thenAccept {
            val result: MethodChannel.Result = MainThreadResult(rawResult)
            result.success(it)
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
