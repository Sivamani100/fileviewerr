import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import '../providers/permission_provider.dart';

class PermissionScreen extends ConsumerWidget {
  final String? error;
  const PermissionScreen({super.key, this.error});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.folder_open, size: 80, color: Color(0xFF2563EB)),
              const SizedBox(height: 24),
              const Text(
                'Storage Access Required',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              const Text(
                'ArkioViewer needs access to manage files so you can browse and view all files on your device.',
                textAlign: TextAlign.center,
              ),
              if (error != null) ...[
                const SizedBox(height: 16),
                Text(
                  'Error: $error',
                  style: const TextStyle(color: Colors.red, fontSize: 12),
                  textAlign: TextAlign.center,
                ),
              ],
              const SizedBox(height: 32),
              ElevatedButton.icon(
                icon: const Icon(Icons.security),
                label: const Text('Open Settings'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF2563EB),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                ),
                onPressed: () async {
                  await openAppSettings();
                },
              ),
              const SizedBox(height: 16),
              TextButton(
                onPressed: () {
                  ref.invalidate(permissionProvider);
                },
                child: const Text('Already granted? Check again'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
