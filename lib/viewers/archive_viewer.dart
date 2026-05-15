import 'dart:io';
import 'package:flutter/material.dart';
import 'package:archive/archive.dart';
import 'package:path/path.dart' as p;

class ArkioArchiveViewer extends StatefulWidget {
  final String filePath;
  const ArkioArchiveViewer({super.key, required this.filePath});

  @override
  State<ArkioArchiveViewer> createState() => _ArkioArchiveViewerState();
}

class _ArkioArchiveViewerState extends State<ArkioArchiveViewer> {
  List<ArchiveFile>? _files;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final bytes = await File(widget.filePath).readAsBytes();
      final ext = p.extension(widget.filePath).toLowerCase();
      
      Archive archive;
      if (['.zip', '.cbz', '.apk', '.jar', '.epub', '.ipa', '.docx', '.xlsx', '.pptx'].contains(ext)) {
        archive = ZipDecoder().decodeBytes(bytes);
      } else if (['.tar', '.tgz', '.tar.gz'].any((e) => widget.filePath.endsWith(e))) {
        archive = TarDecoder().decodeBytes(bytes);
      } else if (ext == '.gz') {
        archive = TarDecoder().decodeBytes(GZipDecoder().decodeBytes(bytes));
      } else {
        // Try ZIP by default
        archive = ZipDecoder().decodeBytes(bytes);
      }
      
      if (mounted) setState(() => _files = archive.files);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  String _formatSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) return Center(child: Text('Cannot read archive:\n$_error'));
    if (_files == null) return const Center(child: CircularProgressIndicator());

    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          width: double.infinity,
          color: Theme.of(context).colorScheme.primaryContainer,
          child: Row(children: [
            const Icon(Icons.folder_zip),
            const SizedBox(width: 8),
            Text('${_files!.length} items inside this archive'),
          ]),
        ),
        Expanded(
          child: ListView.builder(
            itemCount: _files!.length,
            itemBuilder: (context, index) {
              final file = _files![index];
              final isDir = file.isFile == false || file.name.endsWith('/');
              return ListTile(
                dense: true,
                leading: Icon(isDir ? Icons.folder : Icons.insert_drive_file, size: 20),
                title: Text(file.name, style: const TextStyle(fontSize: 13)),
                trailing: isDir ? null : Text(
                  _formatSize(file.size),
                  style: const TextStyle(fontSize: 11, color: Colors.grey),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
