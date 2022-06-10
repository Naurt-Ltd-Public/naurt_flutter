import 'dart:io';
import 'dart:isolate';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_naurt_sdk/flutter_naurt_sdk.dart';

import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool isInitialised = false;
  bool isRunning = false;
  bool isValidated = false;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> getPermissions() async {
    if (Platform.isAndroid) {
      Map<Permission, PermissionStatus> statuses =
          await [Permission.location, Permission.phone].request();
    } else if (Platform.isIOS) {
      Map<Permission, PermissionStatus> statuses = await [
        Permission.location,
        Permission.storage,
        Permission.sensors,
        Permission.phone
      ].request();
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    await getPermissions();

    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      final naurt = Naurt.shared;

      naurt.onLocationChanged.listen((location) {
        print('onLocationChanged: ${location.toString()}');
      });

      naurt.onInitialised = (bool isInitialisedIn) {
        setState(() {
          isInitialised = isInitialisedIn;
        });
      };

      naurt.onValidation = (bool isValid) {
        setState(() {
          isValidated = isValid;
        });
      };

      naurt.onRunning = (bool isRunning) {
        setState(() {
          this.isRunning = isRunning;
        });
      };

      isInitialised = await naurt.initialise(
          apiKey:
              '2a56e73b-acb4-4579-9faf-1d5ec2283394-e2757307-4fdd-4005-a092-ca3bb53de56e',
          precision: 6);
    } on PlatformException {
      isInitialised = false;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Naurt SDK'),
          actions: [
            TextButton(
                onPressed: () async {
                  final isRunning = await Naurt.shared.isRunning();

                  if (isRunning) {
                    final couldStop = await Naurt.shared.stop();
                  } else {
                    final couldStart = await Naurt.shared.start();
                  }
                },
                child: const Text(
                  'Toggle Recording',
                  style: TextStyle(color: Colors.white),
                ))
          ],
        ),
        body: Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  StatusRow(
                    title: 'Is Initialised?',
                    isValid: isInitialised,
                  ),
                  const SizedBox(
                    height: 8,
                  ),
                  StatusRow(
                    title: 'Is Validated?',
                    isValid: isValidated,
                  ),
                  const SizedBox(
                    height: 8,
                  ),
                  StatusRow(
                    title: 'Is Running?',
                    isValid: isRunning,
                  )
                ],
              ),
            )),
      ),
    );
  }
}

class StatusRow extends StatelessWidget {
  final String title;
  final bool isValid;
  const StatusRow({Key? key, required this.title, required this.isValid})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          title,
        ),
        isValid
            ? const Icon(
                Icons.check_circle,
                color: Colors.green,
              )
            : const Icon(
                Icons.cancel,
                color: Colors.red,
              )
      ],
    );
  }
}
