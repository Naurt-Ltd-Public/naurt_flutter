import 'dart:async';

import 'package:flutter/services.dart';

part 'types.dart';

/// Signature for callbacks that report that an underlying value has changed.
///
/// See also:
///
///  * [ValueSetter], for callbacks that report that a value has been set.
typedef ValueChanged<T> = void Function(T value);

class Naurt {
  static const MethodChannel _channel = MethodChannel('flutter_naurt_sdk');

  static const EventChannel _eventChannel =
      EventChannel('flutter_naurt_sdk/locationChanged');

  Naurt._privateConstructor() {
    // Set callbacks from Native -> Flutter
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onValidation') {
        onValidation?.call(call.arguments);
      } else if (call.method == 'onRunning') {
        onRunning?.call(call.arguments);
      } else if (call.method == 'onInitialisation') {
        onInitialised?.call(call.arguments);
      }
    });
  }

  static final Naurt shared = Naurt._privateConstructor();

  /// Returns true if the Naurt SDK is initialized
  Future<bool> initialise(
      {required String apiKey, required int precision}) async {
    final bool isInitialised = await _channel.invokeMethod('initialise', {
      'apiKey': apiKey,
      'precision': precision,
    });

    return isInitialised;
  }

  ValueChanged<bool>? onValidation;
  ValueChanged<bool>? onRunning;
  ValueChanged<bool>? onInitialised;

  /// Is the API key provided to this state valid with the Naurt API server?
  Future<bool> isValidated() async {
    final bool isValidated = await _channel.invokeMethod('isValidated');
    return isValidated;
  }

  /// Is Naurt's Locomotion running at the moment?
  Future<bool> isRunning() async {
    final bool isRunning = await _channel.invokeMethod('isRunning');
    return isRunning;
  }

  /// Is Naurt's Locomotion running at the moment?
  Future<bool> isInitialised() async {
    final bool isInitialised = await _channel.invokeMethod('isInitialised');
    return isInitialised;
  }

  /// Most recent naurt point for the current journey null if no data is available
  Future<NaurtLocation?> lastNaurtPoint() async {
    final Map<String, dynamic>? resultMap =
        await _channel.invokeMapMethod('naurtPoint');
    return resultMap != null ? NaurtLocation.fromMap(resultMap) : null;
  }

  Stream<NaurtLocation> get onLocationChanged {
    return _eventChannel
        .receiveBroadcastStream('onLocationChanged')
        .where((location) => location != null)
        .map((dynamic location) =>
            NaurtLocation.fromMap(Map<String, dynamic>.from(location)));
  }

  /// List of all available naurt points for the current journey. An empty list if no data is available
  Future<List<NaurtLocation>> naurtPoints() async {
    final List<dynamic>? resultMap =
        await _channel.invokeListMethod('naurtPoints');

    return resultMap != null
        ? resultMap
            .map((dataMap) =>
                NaurtLocation.fromMap(Map<String, dynamic>.from(dataMap)))
            .toList()
        : [];
  }

  /// The UUID of the Journey - null if no journey data is available
  Future<String?> journeyUuid() async {
    final String? journeyUuid = await _channel.invokeMethod('journeyUuid');
    return journeyUuid;
  }

  /// Start Naurt Locomotion
  Future<bool> start() async {
    return await _channel.invokeMethod('start');
  }

  Future<bool> stop() async {
    return await _channel.invokeMethod('stop');
  }

  Future<bool> pause() async {
    return await _channel.invokeMethod('pause');
  }

  Future<bool> resume() async {
    return await _channel.invokeMethod('resume');
  }
}
