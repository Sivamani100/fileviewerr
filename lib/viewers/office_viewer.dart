import 'dart:io';
import 'package:flutter/material.dart';
import 'package:archive/archive.dart';
import 'package:xml/xml.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';

class ArkioOfficeViewer extends StatefulWidget {
  final String filePath;
  final String ext;
  const ArkioOfficeViewer({super.key, required this.filePath, required this.ext});

  @override
  State<ArkioOfficeViewer> createState() => _ArkioOfficeViewerState();
}

class _ArkioOfficeViewerState extends State<ArkioOfficeViewer> {
  String? _html;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _convert();
  }

  Future<void> _convert() async {
    try {
      final ext = widget.ext.replaceAll('.', '').toLowerCase();
      String result;
      
      if (['docx', 'odt', 'doc'].contains(ext)) {
        result = await _convertDocx();
      } else if (['xlsx', 'xls', 'ods', 'csv'].contains(ext)) {
        result = await _convertXlsx();
      } else if (['pptx', 'ppt', 'odp'].contains(ext)) {
        result = await _convertPptx();
      } else if (ext == 'rtf') {
        result = await _convertRtf();
      } else {
        result = '<p>Preview not available for .$ext files</p>';
      }
      
      if (mounted) setState(() { _html = result; _loading = false; });
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _loading = false; });
    }
  }

  Future<String> _convertDocx() async {
    // DOCX = ZIP. Unzip and parse word/document.xml
    final bytes = await File(widget.filePath).readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);
    
    // Find word/document.xml
    final docFile = archive.files.firstWhere(
      (f) => f.name == 'word/document.xml',
      orElse: () => throw Exception('Not a valid DOCX file'),
    );
    
    final xmlStr = String.fromCharCodes(docFile.content as List<int>);
    final doc = XmlDocument.parse(xmlStr);
    
    final buffer = StringBuffer('<html><body style="font-family: sans-serif; padding: 16px; line-height: 1.6;">');
    
    // Extract paragraphs
    final paragraphs = doc.findAllElements('w:p');
    for (final para in paragraphs) {
      // Check if it's a heading
      final pStyle = para.findElements('w:pStyle').firstOrNull?.getAttribute('w:val') ?? '';
      final isH1 = pStyle.contains('Heading1') || pStyle.contains('heading1');
      final isH2 = pStyle.contains('Heading2') || pStyle.contains('heading2');
      
      // Extract text runs
      final texts = para.findAllElements('w:t').map((e) => e.innerText).join();
      if (texts.trim().isEmpty) {
        buffer.write('<br/>');
        continue;
      }
      
      // Check bold/italic
      final isBold = para.findAllElements('w:b').isNotEmpty;
      final isItalic = para.findAllElements('w:i').isNotEmpty;
      
      String tag = isH1 ? 'h1' : isH2 ? 'h2' : 'p';
      String style = '';
      if (isBold) style += 'font-weight:bold;';
      if (isItalic) style += 'font-style:italic;';
      
      buffer.write('<$tag style="$style">$texts</$tag>');
    }
    
    // Also try to render tables
    final tables = doc.findAllElements('w:tbl');
    for (final table in tables) {
      buffer.write('<table border="1" cellpadding="4" style="border-collapse:collapse;margin:8px 0">');
      for (final row in table.findElements('w:tr')) {
        buffer.write('<tr>');
        for (final cell in row.findElements('w:tc')) {
          final cellText = cell.findAllElements('w:t').map((e) => e.innerText).join(' ');
          buffer.write('<td style="padding:4px 8px">$cellText</td>');
        }
        buffer.write('</tr>');
      }
      buffer.write('</table>');
    }
    
    buffer.write('</body></html>');
    return buffer.toString();
  }

  Future<String> _convertXlsx() async {
    // XLSX = ZIP. Parse xl/worksheets/sheet1.xml and xl/sharedStrings.xml
    final bytes = await File(widget.filePath).readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);
    
    // Get shared strings (text values)
    final Map<int, String> sharedStrings = {};
    final ssFile = archive.files.where((f) => f.name == 'xl/sharedStrings.xml').firstOrNull;
    if (ssFile != null) {
      final ssXml = XmlDocument.parse(String.fromCharCodes(ssFile.content as List<int>));
      int idx = 0;
      for (final si in ssXml.findAllElements('si')) {
        sharedStrings[idx++] = si.findAllElements('t').map((e) => e.innerText).join('');
      }
    }
    
    // Get first sheet
    final sheetFile = archive.files.where(
      (f) => f.name == 'xl/worksheets/sheet1.xml'
    ).firstOrNull;
    
    if (sheetFile == null) return '<p>No worksheet found</p>';
    
    final sheetXml = XmlDocument.parse(String.fromCharCodes(sheetFile.content as List<int>));
    
    final buffer = StringBuffer(
      '<html><body><table border="1" cellpadding="4" style="border-collapse:collapse;font-family:sans-serif;font-size:13px">'
    );
    
    for (final row in sheetXml.findAllElements('row')) {
      buffer.write('<tr>');
      for (final c in row.findElements('c')) {
        final type = c.getAttribute('t') ?? '';
        final vEl = c.findElements('v').firstOrNull;
        String value = vEl?.innerText ?? '';
        
        if (type == 's') {
          // Shared string
          value = sharedStrings[int.tryParse(value) ?? -1] ?? value;
        }
        buffer.write('<td style="padding:4px 8px;max-width:200px;overflow:hidden">$value</td>');
      }
      buffer.write('</tr>');
    }
    
    buffer.write('</table></body></html>');
    return buffer.toString();
  }

  Future<String> _convertPptx() async {
    // PPTX = ZIP. Parse ppt/slides/slide1.xml etc.
    final bytes = await File(widget.filePath).readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);
    
    final slideFiles = archive.files
        .where((f) => f.name.startsWith('ppt/slides/slide') && f.name.endsWith('.xml'))
        .toList()
      ..sort((a, b) => a.name.compareTo(b.name));
    
    if (slideFiles.isEmpty) return '<p>No slides found</p>';
    
    final buffer = StringBuffer('<html><body style="font-family:sans-serif">');
    
    for (int i = 0; i < slideFiles.length; i++) {
      final slideXml = XmlDocument.parse(
        String.fromCharCodes(slideFiles[i].content as List<int>)
      );
      
      buffer.write(
        '<div style="border:2px solid #ccc;margin:16px;padding:24px;border-radius:8px;'
        'min-height:300px;background:#1a1a2e">'
        '<div style="color:#888;font-size:12px;margin-bottom:8px">Slide ${i + 1}</div>'
      );
      
      // Extract all text
      for (final txBody in slideXml.findAllElements('p:txBody')) {
        for (final para in txBody.findAllElements('a:p')) {
          final text = para.findAllElements('a:t').map((e) => e.innerText).join('');
          if (text.trim().isEmpty) continue;
          
          // Check font size hint
          final sz = para.findAllElements('a:rPr').firstOrNull?.getAttribute('sz');
          final fontSize = sz != null ? (int.tryParse(sz) ?? 1800) / 100 : 18;
          final isTitle = fontSize > 24;
          
          buffer.write(
            '<p style="font-size:${fontSize.clamp(12, 36)}px;'
            '${isTitle ? "font-weight:bold;color:#fff;" : "color:#ddd;"}'
            'margin:4px 0">$text</p>'
          );
        }
      }
      
      buffer.write('</div>');
    }
    
    buffer.write('</body></html>');
    return buffer.toString();
  }

  Future<String> _convertRtf() async {
    // RTF: strip RTF control words and extract plain text
    final content = await File(widget.filePath).readAsString();
    // Basic RTF stripping — remove control words
    String text = content
        .replaceAll(RegExp(r'\\[a-z]+\d*\s?'), ' ')
        .replaceAll(RegExp(r'[{}]'), '')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    return '<html><body style="font-family:sans-serif;padding:16px;line-height:1.6"><p>$text</p></body></html>';
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        CircularProgressIndicator(),
        SizedBox(height: 16),
        Text('Converting document...'),
      ],
    ));
    
    if (_error != null) return Center(child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Icon(Icons.error_outline, size: 64, color: Colors.red),
        const SizedBox(height: 16),
        Text('Error reading file:\n$_error', textAlign: TextAlign.center),
      ],
    ));
    
    return SingleChildScrollView(
      child: HtmlWidget(
        _html!,
        textStyle: const TextStyle(fontSize: 15, height: 1.5),
      ),
    );
  }
}
