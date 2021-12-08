
import 'dart:async';

import 'package:fizido_pay/card_details.dart';
import 'package:flutter/services.dart';

class FizidoPay {
  static const MethodChannel _channel = const MethodChannel('fizido_pay');

  static Future<CardDetails> search(int transactionAmount) async {
    late  CardDetails cardDetails;
    try {
      var result = await _channel
          .invokeMethod('searchCard', {"transactionAmount": transactionAmount});
      print(result);
      var cardResponse = Map<String, String>.from(result);
      cardDetails = CardDetails.fromJson(cardResponse);
      var track2Data = cardDetails.the57;
      var strTrack2 = track2Data!.split("F")[0];
      var pan = strTrack2.split('D')[0];
      var expiry = strTrack2.split('D')[1].substring(0, 4);
      var src = strTrack2.split("D")[1].substring(4, 7);
      cardDetails.strTrack2 = strTrack2;
      cardDetails.pan =  pan;
      cardDetails.expiry = expiry;
      cardDetails.src = src;
    } on PlatformException catch (e) {
      print(e.stacktrace);
    }
    return cardDetails;
  }

  static Future<void> initializeTerminal() async {
    try {
      var result = await _channel.invokeMethod('initEmv');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> stopSearch() async {
    try {
      var result = await _channel.invokeMethod('stopSearch');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> sunyardPrint(Map<String, dynamic> printerDetails) async {
    try {
      var result = await _channel.invokeMethod('startPrinter', printerDetails);
    } catch (e) {
      print(e);
    }
  }

  static Future<bool> checkCard() async {
    var result ;
    try {
      result  = await _channel.invokeMethod('checkSunyardCard');

    } catch (e) {
      print(e);
    }
    return result ;
  }
}
