import 'package:flutter/services.dart';

class OfficeEngine {
  static const _channel = MethodChannel('arkio_viewer/office_engine');

  /// Converts an Office file to PDF using the native ArkioOfficeEngine.
  /// Returns the PDF file path if successful, null otherwise.
  static Future<String?> convertToPdf(String filePath) async {
    try {
      final Map<dynamic, dynamic>? result = await _channel.invokeMethod(
        'convertToPdf',
        {'filePath': filePath},
      );

      if (result != null && result['success'] == true) {
        return result['pdfPath'] as String?;
      }
    } on PlatformException catch (e) {
      print('OfficeEngine Error: ${e.message}');
    } catch (e) {
      print('OfficeEngine Unknown Error: $e');
    }
    return null;
  }

  /// Helper to check if the extension is supported by the engine
  static bool isSupported(String ext) {
    final cleanExt = ext.replaceAll('.', '').toLowerCase();
    return [
      'docx', 'docm', 'dotx', 'dot',
      'xlsx', 'xlsm', 'xltx', 'xls', 'xlt',
      'pptx', 'pptm', 'potx', 'pot', 'ppsx', 'pps'
    ].contains(cleanExt);
  }
}
