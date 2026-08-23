import 'dart:io';
import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';

class ArkioPdfViewer extends StatefulWidget {
  final String filePath;
  const ArkioPdfViewer({super.key, required this.filePath});

  @override
  State<ArkioPdfViewer> createState() => _ArkioPdfViewerState();
}

class _ArkioPdfViewerState extends State<ArkioPdfViewer> {
  final PdfViewerController _pdfViewerController = PdfViewerController();
  PdfTextSearchResult? _searchResult;
  bool _showToolbar = true;
  int _currentPage = 1;
  int _totalPages = 0;
  bool _isSearchVisible = false;
  final TextEditingController _searchTextController = TextEditingController();

  @override
  void dispose() {
    _pdfViewerController.dispose();
    _searchTextController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // The PDF Viewer
          GestureDetector(
            onTap: () {
              setState(() {
                _showToolbar = !_showToolbar;
              });
            },
            child: SfPdfViewer.file(
              File(widget.filePath),
              controller: _pdfViewerController,
              onDocumentLoaded: (details) {
                setState(() {
                  _totalPages = _pdfViewerController.pageCount;
                });
              },
              onPageChanged: (details) {
                setState(() {
                  _currentPage = details.newPageNumber;
                });
              },
              enableDoubleTapZooming: true,
              enableTextSelection: true,
              canShowPaginationDialog: false,
              canShowScrollHead: false,
              pageLayoutMode: PdfPageLayoutMode.continuous,
            ),
          ),

          // Search Bar Overlay
          if (_isSearchVisible)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Container(
                  margin: const EdgeInsets.all(12),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: Theme.of(context).cardColor.withOpacity(0.95),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: Colors.white10),
                    boxShadow: [
                      BoxShadow(color: Colors.black45, blurRadius: 15, spreadRadius: 2)
                    ],
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _searchTextController,
                          autofocus: true,
                          style: const TextStyle(fontSize: 16),
                          decoration: const InputDecoration(
                            hintText: 'Search text...',
                            border: InputBorder.none,
                            isDense: true,
                          ),
                          onSubmitted: (value) {
                            if (value.isNotEmpty) {
                              setState(() {
                                _searchResult = _pdfViewerController.searchText(value);
                                if (_searchResult!.totalInstanceCount == 0) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('No results found'), duration: Duration(seconds: 1)),
                                  );
                                }
                              });
                            }
                          },
                        ),
                      ),
                      if (_searchResult != null && _searchResult!.totalInstanceCount > 0) ...[
                        Text(
                          '${_searchResult!.currentInstanceIndex} / ${_searchResult!.totalInstanceCount}',
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(width: 8),
                        IconButton(
                          icon: const Icon(Icons.keyboard_arrow_up, size: 28),
                          onPressed: () => _searchResult?.previousInstance(),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                        ),
                        const SizedBox(width: 12),
                        IconButton(
                          icon: const Icon(Icons.keyboard_arrow_down, size: 28),
                          onPressed: () => _searchResult?.nextInstance(),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                        ),
                        const SizedBox(width: 8),
                      ],
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () {
                          setState(() {
                            _isSearchVisible = false;
                            _searchResult?.clear();
                            _searchResult = null;
                            _searchTextController.clear();
                          });
                        },
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Bottom Navigation & Page Indicator
          AnimatedPositioned(
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
            bottom: (_showToolbar && !_isSearchVisible) ? 24 : -100,
            left: 20,
            right: 20,
            child: SafeArea(
              child: Container(
                height: 60,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFF1E1E1E).withOpacity(0.9),
                  borderRadius: BorderRadius.circular(30),
                  border: Border.all(color: Colors.white12),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.4),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    )
                  ],
                ),
                child: Row(
                  children: [
                    const SizedBox(width: 8),
                    IconButton(
                      icon: const Icon(Icons.search, color: Colors.white70),
                      onPressed: () {
                        setState(() {
                          _isSearchVisible = true;
                        });
                      },
                    ),
                    const Spacer(),
                    Material(
                      color: Colors.transparent,
                      child: InkWell(
                        onTap: () => _showPageJumpDialog(),
                        borderRadius: BorderRadius.circular(15),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.05),
                            borderRadius: BorderRadius.circular(15),
                          ),
                          child: Text(
                            'PAGE $_currentPage / $_totalPages',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 13,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1.2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const Spacer(),
                    IconButton(
                      icon: const Icon(Icons.add_circle_outline, color: Colors.white70),
                      onPressed: () => _pdfViewerController.zoomLevel += 0.25,
                    ),
                    IconButton(
                      icon: const Icon(Icons.remove_circle_outline, color: Colors.white70),
                      onPressed: () => _pdfViewerController.zoomLevel -= 0.25,
                    ),
                    const SizedBox(width: 8),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showPageJumpDialog() {
    final TextEditingController jumpController = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Jump to Page'),
        content: TextField(
          controller: jumpController,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            hintText: 'Enter page number (1 - $_totalPages)',
            filled: true,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final page = int.tryParse(jumpController.text);
              if (page != null && page > 0 && page <= _totalPages) {
                _pdfViewerController.jumpToPage(page);
                Navigator.pop(context);
              }
            },
            child: const Text('Go'),
          ),
        ],
      ),
    );
  }
}
