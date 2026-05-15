import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../screens/viewer_screen.dart';

class IntentHandlerWrapper extends ConsumerStatefulWidget {
  final Widget child;
  const IntentHandlerWrapper({super.key, required this.child});

  @override
  ConsumerState<IntentHandlerWrapper> createState() => _IntentHandlerWrapperState();
}

class _IntentHandlerWrapperState extends ConsumerState<IntentHandlerWrapper> {
  static const _channel = MethodChannel('arkio_viewer/intent');

  @override
  void initState() {
    super.initState();
    _checkInitialIntent();
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<void> _checkInitialIntent() async {
    try {
      final String? filePath = await _channel.invokeMethod('getInitialFilePath');
      if (filePath != null && mounted) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => ViewerScreen(filePath: filePath)),
          );
        });
      }
    } catch (e) {
      debugPrint('Intent error: $e');
    }
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    if (call.method == 'openFile' && mounted) {
      final String filePath = call.arguments as String;
      Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => ViewerScreen(filePath: filePath)),
      );
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
