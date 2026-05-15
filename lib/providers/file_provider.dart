import 'dart:io';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import '../models/file_item.dart';
import '../utils/file_type_detector.dart';
import 'permission_provider.dart';

final selectedCategoryProvider = StateProvider<FileCategory?>((ref) => null);
final searchQueryProvider = StateProvider<String>((ref) => '');

final allFilesProvider = FutureProvider<List<FileItem>>((ref) async {
  final granted = await ref.watch(permissionProvider.future);
  if (!granted) return [];
  // Use ref.state to cache or check if already scanning
  return _scanDevice();
});

final filteredFilesProvider = Provider<List<FileItem>>((ref) {
  final filesAsync = ref.watch(allFilesProvider);
  final category = ref.watch(selectedCategoryProvider);
  final query = ref.watch(searchQueryProvider).toLowerCase();

  return filesAsync.maybeWhen(
    data: (files) {
      return files.where((file) {
        final matchesCategory = category == null || file.category == category;
        final matchesQuery = query.isEmpty || file.name.toLowerCase().contains(query);
        return matchesCategory && matchesQuery;
      }).toList();
    },
    orElse: () => [],
  );
});

Future<List<FileItem>> _scanDevice() async {
  final List<FileItem> items = [];
  final Set<String> visitedPaths = {};
  
  final List<String> searchPaths = [
    '/storage/emulated/0/Download',
    '/storage/emulated/0/Documents',
    '/storage/emulated/0/DCIM',
    '/storage/emulated/0/Pictures',
    '/storage/emulated/0/Music',
    '/storage/emulated/0/Movies',
    '/storage/emulated/0/WhatsApp/Media',
    '/storage/emulated/0/Android/media',
    '/storage/emulated/0', // Root of internal storage
  ];

  for (final path in searchPaths) {
    final dir = Directory(path);
    if (dir.existsSync()) {
      await _scanDirectory(dir, items, visitedPaths);
    }
  }

  items.sort((a, b) => b.modified.compareTo(a.modified));
  return items;
}

Future<void> _scanDirectory(Directory dir, List<FileItem> items, Set<String> visited) async {
  if (visited.contains(dir.path)) return;
  visited.add(dir.path);

  try {
    // Stream entities to avoid loading everything into memory at once
    await for (final entity in dir.list(recursive: false, followLinks: false)) {
      if (entity is File) {
        try {
          final name = p.basename(entity.path);
          if (name.startsWith('.')) continue;

          final ext = p.extension(name).toLowerCase();
          final category = FileTypeDetector.detect(ext);
          
          if (category != FileCategory.unknown || ext.isNotEmpty) {
            final stat = await entity.stat();
            items.add(FileItem(
              path: entity.path,
              name: name,
              extension: ext,
              sizeBytes: stat.size,
              modified: stat.modified,
              category: category,
            ));
          }
        } catch (_) {}
      } else if (entity is Directory) {
        final name = p.basename(entity.path);
        if (name.startsWith('.')) continue;
        
        // Skip restricted system folders
        if (name == 'Android' && dir.path == '/storage/emulated/0') {
          // We only scan Android/media, skipped in the main loop above or handled here
          final mediaDir = Directory(p.join(entity.path, 'media'));
          if (mediaDir.existsSync()) {
            await _scanDirectory(mediaDir, items, visited);
          }
          continue; 
        }
        
        // Avoid deep recursion for very large folders if needed, 
        // but for a file manager we usually want it.
        await _scanDirectory(entity, items, visited);
      }
    }
  } catch (_) {
    // Skip unaccessible directories
  }
}
