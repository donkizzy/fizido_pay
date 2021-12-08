package com.app.fizido_pay.fizido_pay;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.app.fizido_pay.fizido_pay.Sunyard.SunyardApplication;
import com.app.fizido_pay.fizido_pay.Sunyard.SunyardPrinter;
import com.app.fizido_pay.fizido_pay.Sunyard.SunyardReadCard;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
//import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FizidoPayPlugin */
public class FizidoPayPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  
  private MethodChannel channel;
  SunyardReadCard sunyardReadCard;
  private Context mContext;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "fizido_pay");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    sunyardReadCard = new SunyardReadCard(mContext);
    switch (call.method) {
      case "searchCard":
        sunyardReadCard.searchCard(result, call.argument("transactionAmount"));
        break;
      case "stopSearch":
        sunyardReadCard.stopSearch();
        break;
      case "initEmv":
        SunyardApplication sunyardApplication = new SunyardApplication();
        sunyardApplication.initializeApp(mContext);
        break;
      case "startPrinter":
        SunyardPrinter sunyardPrinter = new SunyardPrinter(mContext);
        Log.d("PrintActivity.class", call.arguments.toString());
        sunyardPrinter.startPrint(call);
        break;
      case "checkSunyardCard":
        sunyardReadCard.checkCard(result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    mContext = binding.getActivity().getApplicationContext();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {
    sunyardReadCard.stopSearch();
  }
}
