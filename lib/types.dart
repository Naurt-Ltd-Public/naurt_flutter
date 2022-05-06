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

class AndroidDeviceReport {
  final String isDeveloper;
  final String isDeviceRooted;
  final String isInWorkProfile;
  final int lastReportChange;
  final String processName;
  final String wasLastLocationMocked;
  final String hasMockingAppsInstalled;
  AndroidDeviceReport._({
    required this.isDeveloper,
    required this.isDeviceRooted,
    required this.isInWorkProfile,
    required this.lastReportChange,
    required this.processName,
    required this.wasLastLocationMocked,
    required this.hasMockingAppsInstalled,
  });

  factory AndroidDeviceReport.fromMap(Map<String, dynamic> dataMap) {
    return AndroidDeviceReport._(
      isDeveloper: dataMap['isDeveloper'],
      isDeviceRooted: dataMap['isDeviceRooted'],
      isInWorkProfile: dataMap['isInWorkProfile'],
      lastReportChange: dataMap['lastReportChange'],
      processName: dataMap['processName'],
      wasLastLocationMocked: dataMap['wasLastLocationMocked'],
      hasMockingAppsInstalled: dataMap['hasMockingAppsInstalled'],
    );
  }

  @override
  String toString() =>
      'AndroidDeviceReport<wasLastLocationMocked: $wasLastLocationMocked, hasMockingAppsInstalled: $hasMockingAppsInstalled lastReportChange: $lastReportChange>';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is AndroidDeviceReport &&
        other.isDeveloper == isDeveloper &&
        other.isDeviceRooted == isDeviceRooted &&
        other.processName == processName &&
        other.wasLastLocationMocked == wasLastLocationMocked &&
        other.hasMockingAppsInstalled == hasMockingAppsInstalled &&
        other.isInWorkProfile == isInWorkProfile;
  }

  @override
  int get hashCode =>
      isDeveloper.hashCode ^
      isDeviceRooted.hashCode ^
      processName.hashCode ^
      wasLastLocationMocked.hashCode ^
      hasMockingAppsInstalled.hashCode ^
      isInWorkProfile.hashCode;
}
