import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:share_plus/share_plus.dart';
import '../models/file_item.dart';
import '../utils/file_type_detector.dart';
import '../viewers/pdf_viewer.dart';
import '../viewers/image_viewer.dart';
import '../viewers/video_viewer.dart';
import '../viewers/audio_viewer.dart';
import '../viewers/text_viewer.dart';
import '../viewers/office_viewer.dart';
import '../viewers/archive_viewer.dart';
import '../viewers/unsupported_viewer.dart';

class ViewerScreen extends StatelessWidget {
  final String filePath;
  const ViewerScreen({super.key, required this.filePath});

  @override
  Widget build(BuildContext context) {
    final name = p.basename(filePath);
    final ext = p.extension(filePath).toLowerCase();
    final category = FileTypeDetector.detect(ext);

    return Scaffold(
      appBar: AppBar(
        title: Text(name, overflow: TextOverflow.ellipsis),
        actions: [
          IconButton(
            icon: const Icon(Icons.share),
            onPressed: () => _share(filePath),
          ),
        ],
      ),
      body: _buildViewer(category, ext),
    );
  }

  Widget _buildViewer(FileCategory category, String ext) {
    switch (category) {
      case FileCategory.pdf:
        return ArkioPdfViewer(filePath: filePath);
      case FileCategory.image:
        return ArkioImageViewer(filePath: filePath);
      case FileCategory.video:
        return ArkioVideoViewer(filePath: filePath);
      case FileCategory.audio:
        return ArkioAudioViewer(filePath: filePath);
      case FileCategory.text:
      case FileCategory.code:
        return ArkioTextViewer(filePath: filePath, ext: ext);
      case FileCategory.office:
        return ArkioOfficeViewer(filePath: filePath, ext: ext);
      case FileCategory.archive:
        return ArkioArchiveViewer(filePath: filePath);
      case FileCategory.ebook:
        return ArkioUnsupportedViewer(filePath: filePath, reason: 'Ebook viewer coming soon. Open with system reader.');
      case FileCategory.unknown:
        return ArkioUnsupportedViewer(filePath: filePath, reason: 'Unknown file type.');
    }
  }

  void _share(String path) {
    Share.shareXFiles([XFile(path)]);
  }
}
