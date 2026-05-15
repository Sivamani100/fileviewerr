class FileItem {
  final String path;
  final String name;
  final String extension;
  final int sizeBytes;
  final DateTime modified;
  final FileCategory category;

  const FileItem({
    required this.path,
    required this.name,
    required this.extension,
    required this.sizeBytes,
    required this.modified,
    required this.category,
  });

  String get sizeFormatted {
    if (sizeBytes < 1024) return '$sizeBytes B';
    if (sizeBytes < 1024 * 1024) return '${(sizeBytes / 1024).toStringAsFixed(1)} KB';
    if (sizeBytes < 1024 * 1024 * 1024) return '${(sizeBytes / (1024 * 1024)).toStringAsFixed(1)} MB';
    return '${(sizeBytes / (1024 * 1024 * 1024)).toStringAsFixed(1)} GB';
  }
}

enum FileCategory {
  pdf,
  image,
  video,
  audio,
  text,
  code,
  office,   // docx, xlsx, pptx
  archive,  // zip, rar, 7z, tar
  ebook,
  unknown,
}
