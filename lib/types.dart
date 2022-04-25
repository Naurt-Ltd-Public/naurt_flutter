part of 'flutter_naurt_sdk.dart';

class NaurtLocation {
  final double latitude;
  final double longitude;
  final int timestamp;
  final double horizontalAccuracy;
  final double speed;
  final double heading;
  final double speedAccuracy;
  final double headingAccuracy;
  final double horizontalCovariance;
  final double altitude;
  final double verticalAccuracy;
  final bool mockedLocation;
  final bool mockAppsInstalled;
  final bool mockSettingActive;
  NaurtLocation._({
    required this.latitude,
    required this.longitude,
    required this.timestamp,
    required this.horizontalAccuracy,
    required this.speed,
    required this.heading,
    required this.speedAccuracy,
    required this.headingAccuracy,
    required this.horizontalCovariance,
    required this.altitude,
    required this.verticalAccuracy,
    required this.mockedLocation,
    required this.mockAppsInstalled,
    required this.mockSettingActive,
  });

  factory NaurtLocation.fromMap(Map<String, dynamic> dataMap) {
    return NaurtLocation._(
      latitude: dataMap['latitude'],
      longitude: dataMap['longitude'],
      timestamp: dataMap['timestamp'],
      horizontalAccuracy: dataMap['horizontalAccuracy'],
      speed: dataMap['speed'],
      heading: dataMap['heading'],
      speedAccuracy: dataMap['speedAccuracy'],
      headingAccuracy: dataMap['headingAccuracy'],
      horizontalCovariance: dataMap['horizontalCovariance'],
      altitude: dataMap['altitude'],
      verticalAccuracy: dataMap['verticalAccuracy'],
      mockedLocation: dataMap['mockedLocation'],
      mockAppsInstalled: dataMap['mockAppsInstalled'],
      mockSettingActive: dataMap['mockSettingActive'],
    );
  }

  @override
  String toString() =>
      'NaurtLocation<lat: $latitude, long: $longitude timestamp: $timestamp>';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is NaurtLocation &&
        other.latitude == latitude &&
        other.longitude == longitude &&
        other.timestamp == timestamp;
  }

  @override
  int get hashCode =>
      latitude.hashCode ^ longitude.hashCode ^ timestamp.hashCode;
}
