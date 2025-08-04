import 'orion_flutter_platform_interface.dart';
import 'package:flutter/services.dart';


class OrionFlutter {

  static const MethodChannel _channel = MethodChannel('orion_flutter');
  static Future<String?> initializeEdOrion({required String cid, required String pid}) async {
    return await _channel.invokeMethod<String>('initializeEdOrion', {
      'cid': cid,
      'pid': pid,
    });
  }

  static Future<String?> getPlatformVersion() {
    return OrionFlutterPlatform.instance.getPlatformVersion();
  }

  static Future<String?> getRuntimeMetrics() {
    return OrionFlutterPlatform.instance.getRuntimeMetrics();
  }

  static Future<void> trackFlutterErrorRaw({
    required String exception,
    required String stack,
    String? library,
    String? context,
    String? screen,
    List<Map<String, dynamic>>? network,
  }) async {
    await _channel.invokeMethod('trackFlutterError', {
      'exception': exception,
      'stack': stack,
      'library': library ?? '',
      'context': context ?? '',
      'screen': screen ?? 'UnknownScreen',
      'network': network ?? [],
    });
  }

  static void trackUnhandledError(Object error, StackTrace stack, {String? screen, List<Map<String, dynamic>>? network}) {
    _channel.invokeMethod('trackFlutterError', {
      'exception': error.toString(),
      'stack': stack.toString(),
      'library': '',
      'context': '',
      'screen': screen ?? 'UnknownScreen',
      'network': network ?? [],
    });
  }

  static Future<void> trackNetworkRequest({
    required String method,
    required String url,
    required int statusCode,
    required int startTime,
    required int endTime,
    required int duration,
    int? payloadSize,
    String? contentType,
    String? errorMessage,
  }) async {
    await _channel.invokeMethod('trackNetworkRequest', {
      'method': method,
      'url': url,
      'statusCode': statusCode,
      'startTime': startTime,
      'endTime': endTime,
      'duration': duration,
      'payloadSize': payloadSize,
      'contentType': contentType,
      'errorMessage': errorMessage,
    });
  }

  static Future<void> trackFlutterScreen({
    required String screen,
    int ttid = -1,
    int ttfd = -1,
    int jankyFrames = 0,
    int frozenFrames = 0,
    List<Map<String, dynamic>> network = const [],
  }) async {
    await _channel.invokeMethod("trackFlutterScreen", {
      "screen": screen,
      "ttid": ttid,
      "ttfd": ttfd,
      "jankyFrames": jankyFrames,
      "frozenFrames": frozenFrames,
      "network": network,
    });
  }



}