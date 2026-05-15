import '../models/file_item.dart';

class FileTypeDetector {
  static FileCategory detect(String extension) {
    final ext = extension.toLowerCase().replaceAll('.', '');
    
    if (_pdfExts.contains(ext)) return FileCategory.pdf;
    if (_imageExts.contains(ext)) return FileCategory.image;
    if (_videoExts.contains(ext)) return FileCategory.video;
    if (_audioExts.contains(ext)) return FileCategory.audio;
    if (_officeExts.contains(ext)) return FileCategory.office;
    if (_archiveExts.contains(ext)) return FileCategory.archive;
    if (_textExts.contains(ext)) return FileCategory.text;
    if (_codeExts.contains(ext)) return FileCategory.code;
    if (_ebookExts.contains(ext)) return FileCategory.ebook;
    return FileCategory.unknown;
  }

  static String iconFor(FileCategory cat) {
    switch (cat) {
      case FileCategory.pdf: return '📄';
      case FileCategory.image: return '🖼️';
      case FileCategory.video: return '🎬';
      case FileCategory.audio: return '🎵';
      case FileCategory.office: return '📊';
      case FileCategory.archive: return '📦';
      case FileCategory.text: return '📝';
      case FileCategory.code: return '💻';
      case FileCategory.ebook: return '📚';
      case FileCategory.unknown: return '📁';
    }
  }

  static const Set<String> _pdfExts = {'pdf', 'pdfa', 'pdfx', 'xps', 'oxps'};

  static const Set<String> _imageExts = {
    'jpg', 'jpeg', 'jpe', 'jfif', 'png', 'gif', 'bmp', 'dib', 'tif', 'tiff',
    'webp', 'avif', 'heif', 'heic', 'jxl', 'ico', 'svg', 'svgz', 'raw',
    'cr2', 'cr3', 'nef', 'arw', 'dng', 'orf', 'rw2', 'psd', 'xcf', 'kra',
    'tga', 'ppm', 'pgm', 'pbm', 'hdr', 'exr', 'pcx', 'pict', 'pct', 'pic',
    'wmf', 'emf', 'xbm', 'xpm', 'sgi', 'rgb', 'rgba', 'bw', 'qoi', 'apng',
  };

  static const Set<String> _videoExts = {
    'mp4', 'm4v', 'mov', 'qt', 'avi', 'mkv', 'mka', 'webm', 'ogv', 'flv',
    'wmv', 'wm', 'asf', 'rm', 'rmvb', 'rv', 'ts', 'mts', 'm2ts', 'mpg',
    'mpeg', 'mpe', '3gp', '3g2', 'divx', 'xvid', 'dv', 'vob', 'f4v',
    'swf', 'hevc', 'h264', 'h265', 'm4p', 'mjpeg', 'mjpg',
  };

  static const Set<String> _audioExts = {
    'mp3', 'mp2', 'aac', 'm4a', 'm4b', 'm4r', 'wav', 'wave', 'flac', 'alac',
    'ape', 'wv', 'ogg', 'oga', 'opus', 'wma', 'aiff', 'aif', 'aifc', 'au',
    'snd', 'ra', 'mpc', 'amr', 'gsm', 'mid', 'midi', 'dts', 'ac3',
    'caf', 'w64', 'bwf', 'voc', 'spx', 'm3u', 'm3u8', 'pls', 'wpl',
  };

  static const Set<String> _officeExts = {
    // Word
    'doc', 'docx', 'docm', 'dot', 'dotx', 'dotm', 'odt', 'ott', 'rtf',
    'wpd', 'wps', 'pages',
    // Excel
    'xls', 'xlsx', 'xlsm', 'xlsb', 'xlt', 'xltx', 'ods', 'csv', 'numbers',
    // PowerPoint
    'ppt', 'pptx', 'pptm', 'pps', 'ppsx', 'pot', 'potx', 'odp', 'key',
  };

  static const Set<String> _archiveExts = {
    'zip', 'zipx', 'gz', 'tgz', 'tar', 'bz2', 'tbz', 'tbz2', 'xz', 'txz',
    '7z', 'rar', 'r00', 'cab', 'arj', 'lha', 'lzh', 'ace', 'arc', 'z',
    'lz4', 'zst', 'br', 'iso', 'img', 'cbr', 'cbz', 'cb7', 'cbt',
    'apk', 'ipa', 'jar', 'war', 'ear', 'deb', 'rpm',
  };

  static const Set<String> _textExts = {
    'txt', 'md', 'markdown', 'mdown', 'mkd', 'rst', 'adoc', 'asciidoc',
    'nfo', 'diz', 'log', 'me', '1st', 'readme', 'csv', 'tsv',
    'json', 'jsonc', 'json5', 'jsonl', 'xml', 'yaml', 'yml', 'toml',
    'ini', 'cfg', 'conf', 'config', 'env', 'properties',
  };

  static const Set<String> _codeExts = {
    'py', 'pyw', 'js', 'mjs', 'cjs', 'jsx', 'ts', 'tsx', 'coffee',
    'java', 'c', 'h', 'cpp', 'cxx', 'cc', 'cs', 'vb', 'go', 'rs',
    'swift', 'kt', 'kts', 'groovy', 'scala', 'clj', 'hs', 'elm', 'erl',
    'ex', 'exs', 'ml', 'fs', 'rkt', 'scm', 'lisp', 'lsp', 'el', 'jl',
    'r', 'rb', 'rbw', 'php', 'lua', 'tcl', 'sh', 'bash', 'zsh', 'fish',
    'ksh', 'csh', 'bat', 'cmd', 'ps1', 'awk', 'sed', 'nim', 'cr', 'zig',
    'pas', 'pp', 'ada', 'cob', 'cbl', 'for', 'f', 'f90', 'f95', 'asm',
    's', 'dart', 'sql', 'graphql', 'gql', 'html', 'htm', 'css', 'scss',
    'sass', 'less', 'vue', 'svelte', 'astro', 'pug', 'haml', 'erb',
    'v', 'vh', 'vhd', 'sv', 'pl', 'pm', 'proto',
  };

  static const Set<String> _ebookExts = {
    'epub', 'mobi', 'azw', 'azw3', 'kfx', 'fb2', 'fb3', 'lit',
    'djvu', 'djv', 'cbr', 'cbz', 'lrf',
  };
}
