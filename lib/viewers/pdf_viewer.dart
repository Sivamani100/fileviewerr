import 'dart:io';
import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';

class ArkioPdfViewer extends StatelessWidget {
  final String filePath;
  const ArkioPdfViewer({super.key, required this.filePath});

  @override
  Widget build(BuildContext context) {
    return SfPdfViewer.file(
      File(filePath),
      enableDoubleTapZooming: true,
      enableTextSelection: true,
      pageLayoutMode: PdfPageLayoutMode.continuous,
    );
  }
}
