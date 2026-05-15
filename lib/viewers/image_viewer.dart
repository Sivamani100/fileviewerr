import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:path/path.dart' as p;

class ArkioImageViewer extends StatelessWidget {
  final String filePath;
  const ArkioImageViewer({super.key, required this.filePath});

  @override
  Widget build(BuildContext context) {
    final ext = p.extension(filePath).toLowerCase().replaceAll('.', '');
    
    return InteractiveViewer(
      minScale: 0.5,
      maxScale: 10.0,
      child: Center(
        child: _buildImage(ext),
      ),
    );
  }

  Widget _buildImage(String ext) {
    if (ext == 'svg' || ext == 'svgz') {
      return SvgPicture.file(
        File(filePath),
        fit: BoxFit.contain,
        placeholderBuilder: (_) => const CircularProgressIndicator(),
      );
    }
    
    // For HEIC, raw camera formats — show "unsupported" gracefully
    const unsupportedRawFormats = {
      'heic', 'heif', 'raw', 'cr2', 'cr3', 'nef', 'arw', 'dng', 'orf',
      'rw2', 'psd', 'xcf', 'kra', 'exr', 'hdr',
    };
    
    if (unsupportedRawFormats.contains(ext)) {
      return Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.image_not_supported, size: 64, color: Colors.grey),
          const SizedBox(height: 16),
          Text('.${ext.toUpperCase()} format preview not supported'),
          const SizedBox(height: 8),
          const Text('File info is available below', style: TextStyle(color: Colors.grey)),
        ],
      );
    }

    return Image.file(
      File(filePath),
      fit: BoxFit.contain,
      errorBuilder: (context, error, _) => Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.broken_image, size: 64, color: Colors.grey),
          const SizedBox(height: 16),
          Text('Cannot display this image: $error'),
        ],
      ),
    );
  }
}
