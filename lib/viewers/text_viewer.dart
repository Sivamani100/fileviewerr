import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_highlight/flutter_highlight.dart';
import 'package:flutter_highlight/themes/atom-one-dark.dart';
import 'package:csv/csv.dart';

class ArkioTextViewer extends StatefulWidget {
  final String filePath;
  final String ext;
  const ArkioTextViewer({super.key, required this.filePath, required this.ext});

  @override
  State<ArkioTextViewer> createState() => _ArkioTextViewerState();
}

class _ArkioTextViewerState extends State<ArkioTextViewer> {
  String? _content;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final content = await File(widget.filePath).readAsString();
      if (mounted) setState(() => _content = content);
    } catch (e) {
      if (mounted) setState(() => _error = 'Cannot read file: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) return Center(child: Text(_error!));
    if (_content == null) return const Center(child: CircularProgressIndicator());
    
    final ext = widget.ext.replaceAll('.', '');

    // Markdown
    if (['md', 'markdown', 'mdown', 'mkd'].contains(ext)) {
      return Markdown(data: _content!, selectable: true);
    }

    // CSV — render as table
    if (ext == 'csv' || ext == 'tsv') {
      return _CsvTable(content: _content!, isTsv: ext == 'tsv');
    }

    // Code files — syntax highlight
    const codeExts = {
      'py', 'js', 'ts', 'jsx', 'tsx', 'dart', 'java', 'c', 'cpp', 'h',
      'cs', 'go', 'rs', 'swift', 'kt', 'rb', 'php', 'lua', 'sh', 'bash',
      'sql', 'html', 'css', 'scss', 'xml', 'json', 'yaml', 'yml', 'toml',
      'r', 'jl', 'ex', 'exs', 'erl', 'hs', 'scala', 'clj', 'v', 'zig',
    };
    
    if (codeExts.contains(ext)) {
      final lang = _languageMap[ext] ?? ext;
      return SingleChildScrollView(
        scrollDirection: Axis.vertical,
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: HighlightView(
            _content!,
            language: lang,
            theme: atomOneDarkTheme,
            padding: const EdgeInsets.all(16),
            textStyle: const TextStyle(fontFamily: 'JetBrainsMono', fontSize: 13),
          ),
        ),
      );
    }

    // Plain text
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: SelectableText(
        _content!,
        style: const TextStyle(fontFamily: 'JetBrainsMono', fontSize: 14, height: 1.5),
      ),
    );
  }

  static const _languageMap = {
    'py': 'python', 'js': 'javascript', 'ts': 'typescript',
    'jsx': 'javascript', 'tsx': 'typescript', 'dart': 'dart',
    'java': 'java', 'c': 'c', 'cpp': 'cpp', 'h': 'c',
    'cs': 'csharp', 'go': 'go', 'rs': 'rust', 'swift': 'swift',
    'kt': 'kotlin', 'rb': 'ruby', 'php': 'php', 'lua': 'lua',
    'sh': 'bash', 'bash': 'bash', 'sql': 'sql', 'html': 'xml',
    'css': 'css', 'scss': 'scss', 'xml': 'xml', 'json': 'json',
    'yaml': 'yaml', 'yml': 'yaml', 'r': 'r', 'scala': 'scala',
  };
}

class _CsvTable extends StatelessWidget {
  final String content;
  final bool isTsv;
  const _CsvTable({required this.content, required this.isTsv});

  @override
  Widget build(BuildContext context) {
    final rows = const CsvToListConverter().convert(
      content,
      fieldDelimiter: isTsv ? '\t' : ',',
    );
    if (rows.isEmpty) return const Center(child: Text('Empty file'));
    
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: SingleChildScrollView(
        child: DataTable(
          headingRowColor: WidgetStateProperty.all(
            Theme.of(context).colorScheme.primaryContainer,
          ),
          columns: rows.first.map((cell) => DataColumn(
            label: Text(cell.toString(), style: const TextStyle(fontWeight: FontWeight.bold)),
          )).toList(),
          rows: rows.skip(1).map((row) => DataRow(
            cells: row.map((cell) => DataCell(Text(cell.toString()))).toList(),
          )).toList(),
        ),
      ),
    );
  }
}
