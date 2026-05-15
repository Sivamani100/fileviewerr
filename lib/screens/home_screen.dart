import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/file_item.dart';
import '../providers/file_provider.dart';
import '../providers/permission_provider.dart';
import '../utils/file_type_detector.dart';
import 'permission_screen.dart';
import 'viewer_screen.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final permAsync = ref.watch(permissionProvider);
    
    return permAsync.when(
      loading: () => const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (e, _) => PermissionScreen(error: e.toString()),
      data: (granted) {
        if (!granted) return const PermissionScreen();
        return const _FileListView();
      },
    );
  }
}

class _FileListView extends ConsumerWidget {
  const _FileListView();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filesAsync = ref.watch(allFilesProvider);
    final selectedCat = ref.watch(selectedCategoryProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('ArkioViewer', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.invalidate(allFilesProvider);
            },
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50),
          child: _CategoryFilter(),
        ),
      ),
      body: filesAsync.when(
        loading: () => const Center(child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Scanning your files...'),
          ],
        )),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (files) {
          final filtered = selectedCat == null
              ? files
              : files.where((f) => f.category == selectedCat).toList();
          
          if (filtered.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text('No files found'),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () => ref.invalidate(allFilesProvider),
                    child: const Text('Refresh Scan'),
                  ),
                ],
              ),
            );
          }

          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(
                  'Showing ${filtered.length} files',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              Expanded(
                child: ListView.builder(
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final file = filtered[index];
                    return ListTile(
                      leading: Text(
                        FileTypeDetector.iconFor(file.category),
                        style: const TextStyle(fontSize: 28),
                      ),
                      title: Text(file.name, maxLines: 1, overflow: TextOverflow.ellipsis),
                      subtitle: Text(
                        '${file.sizeFormatted} • ${_formatDate(file.modified)}',
                        style: const TextStyle(fontSize: 12),
                      ),
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => ViewerScreen(filePath: file.path)),
                      ),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  String _formatDate(DateTime dt) {
    return '${dt.day}/${dt.month}/${dt.year}';
  }
}

class _CategoryFilter extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(selectedCategoryProvider);
    final categories = [null, ...FileCategory.values];
    
    return SizedBox(
      height: 50,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        itemCount: categories.length,
        itemBuilder: (context, i) {
          final cat = categories[i];
          final isSelected = selected == cat;
          final label = cat == null ? 'All' : cat.name.toUpperCase();
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilterChip(
              label: Text(label),
              selected: isSelected,
              onSelected: (_) => ref.read(selectedCategoryProvider.notifier).state = cat,
            ),
          );
        },
      ),
    );
  }
}
