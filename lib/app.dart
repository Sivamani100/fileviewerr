import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'screens/home_screen.dart';
import 'utils/intent_handler.dart';

class ArkioViewerApp extends ConsumerStatefulWidget {
  const ArkioViewerApp({super.key});
  @override
  ConsumerState<ArkioViewerApp> createState() => _ArkioViewerAppState();
}

class _ArkioViewerAppState extends ConsumerState<ArkioViewerApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ArkioViewer',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF2563EB),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const IntentHandlerWrapper(child: HomeScreen()),
    );
  }
}
