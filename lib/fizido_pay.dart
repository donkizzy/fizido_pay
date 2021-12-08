
import 'dart:async';

import 'package:flutter/services.dart';

class FizidoPay {
  static const MethodChannel _channel =
      const MethodChannel('fizido_pay');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
