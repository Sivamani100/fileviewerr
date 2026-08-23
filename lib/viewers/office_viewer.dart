import 'dart:io';
import 'package:flutter/material.dart';
import '../utils/office_engine.dart';
import './pdf_viewer.dart';

class ArkioOfficeViewer extends StatefulWidget {
  final String filePath;
  final String ext;
  const ArkioOfficeViewer({super.key, required this.filePath, required this.ext});

  @override
  State<ArkioOfficeViewer> createState() => _ArkioOfficeViewerState();
}

class _ArkioOfficeViewerState extends State<ArkioOfficeViewer> {
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _startConversion();
  }

  Future<void> _startConversion() async {
    try {
      final ext = widget.ext.replaceAll('.', '').toLowerCase();
      
      // 1. Trigger Native ArkioOfficeEngine Conversion
      if (Platform.isAndroid && OfficeEngine.isSupported(ext)) {
        final pdfPath = await OfficeEngine.convertToPdf(widget.filePath);
        
        if (pdfPath != null && File(pdfPath).existsSync()) {
          // 2. SUCCESS: Transition to the professional PDF Viewer
          if (mounted) {
            Navigator.pushReplacement(
              context,
              MaterialPageRoute(
                builder: (context) => ArkioPdfViewer(filePath: pdfPath),
              ),
            );
          }
          return;
        } else {
          throw Exception('Native conversion failed to produce a valid PDF.');
        }
      } else if (!Platform.isAndroid) {
        throw Exception('Native Office Engine is currently only available on Android.');
      } else {
        throw Exception('Format .$ext is not currently supported by the engine.');
      }

    } catch (e) {
      if (mounted) {
        setState(() { 
          _error = e.toString().replaceAll('Exception: ', ''); 
          _loading = false; 
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final fileName = widget.filePath.split(Platform.isWindows ? '\\' : '/').last;
    
    if (_loading) {
      return Scaffold(
        appBar: AppBar(
          title: Text(fileName, style: const TextStyle(fontSize: 16)),
          backgroundColor: const Color(0xFF1A1A2E),
          foregroundColor: Colors.white,
          elevation: 0,
        ),
        backgroundColor: Colors.white,
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Premium Animated Loader
              const SizedBox(
                width: 60,
                height: 60,
                child: CircularProgressIndicator(
                  strokeWidth: 3,
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF1A56DB)),
                ),
              ),
              const SizedBox(height: 32),
              const Text(
                'PREPARING DOCUMENT',
                style: TextStyle(
                  letterSpacing: 2,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A1A2E),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'High-fidelity offline conversion...',
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
            ],
          ),
        ),
      );
    }

    if (_error != null) {
      return Scaffold(
        appBar: AppBar(
          title: Text(fileName),
          backgroundColor: const Color(0xFF1A1A2E),
          foregroundColor: Colors.white,
        ),
        backgroundColor: Colors.white,
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(40),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error_outline, size: 80, color: Colors.redAccent),
                const SizedBox(height: 24),
                const Text(
                  'CONVERSION ERROR',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                Text(
                  _error!,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey[700], height: 1.5),
                ),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: () => Navigator.pop(context),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF1A1A2E),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                  ),
                  child: const Text('Go Back'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return const SizedBox.shrink(); // Should not be reached
  }
}
