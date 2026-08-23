import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'dart:io';

class OfficeViewerScreen extends StatefulWidget {
  final String fileUrl;
  final String fileName;

  const OfficeViewerScreen({
    super.key,
    required this.fileUrl,
    required this.fileName,
  });

  @override
  State<OfficeViewerScreen> createState() => _OfficeViewerScreenState();
}

class _OfficeViewerScreenState extends State<OfficeViewerScreen> {
  late final WebViewController _controller;
  bool _isLoading = true;
  double _loadingProgress = 0;
  bool _hasError = false;

  // IMPORTANT: Replace this with your ONLYOFFICE Document Server URL
  // Example: 'https://documents.arkio.cloud' or 'http://192.168.1.100:8080'
  static const String onlyOfficeServer = 'https://your-onlyoffice-server';

  @override
  void initState() {
    super.initState();
    _initializeController();
  }

  void _initializeController() {
    // Generate the ONLYOFFICE Editor URL
    // Documentation: https://api.onlyoffice.com/editors/basic
    final String encodedUrl = Uri.encodeComponent(widget.fileUrl);
    final String viewerUrl = '$onlyOfficeServer/web-apps/apps/documenteditor/main/index.html?fileUrl=$encodedUrl';

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (int progress) {
            setState(() {
              _loadingProgress = progress / 100;
            });
          },
          onPageStarted: (String url) {
            setState(() {
              _isLoading = true;
              _hasError = false;
            });
          },
          onPageFinished: (String url) {
            setState(() {
              _isLoading = false;
            });
          },
          onWebResourceError: (WebResourceError error) {
            debugPrint('WebView Error: ${error.description}');
            setState(() {
              _isLoading = false;
              _hasError = true;
            });
          },
        ),
      )
      ..loadRequest(Uri.parse(viewerUrl));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.fileName,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              overflow: TextOverflow.ellipsis,
            ),
            const Text(
              'ONLYOFFICE Document Editor',
              style: TextStyle(fontSize: 12, color: Colors.white70),
            ),
          ],
        ),
        backgroundColor: const Color(0xFF2C3E50),
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _controller.reload(),
          ),
        ],
      ),
      body: Stack(
        children: [
          // WebView Component
          if (!_hasError)
            WebViewWidget(controller: _controller),

          // Error UI
          if (_hasError)
            _buildErrorUI(),

          // Loading Indicator
          if (_isLoading && !_hasError)
            _buildLoadingUI(),
        ],
      ),
    );
  }

  Widget _buildLoadingUI() {
    return Container(
      color: Colors.white,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Premium looking loader
            SizedBox(
              width: 60,
              height: 60,
              child: CircularProgressIndicator(
                value: _loadingProgress > 0 ? _loadingProgress : null,
                strokeWidth: 5,
                color: const Color(0xFF3498DB),
                backgroundColor: Colors.grey.shade200,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Rendering Document...',
              style: TextStyle(
                color: Colors.grey.shade700,
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
            if (_loadingProgress > 0)
              Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: Text(
                  '${(_loadingProgress * 100).toInt()}%',
                  style: const TextStyle(color: Colors.blueAccent, fontWeight: FontWeight.bold),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorUI() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      color: Colors.white,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.cloud_off, size: 80, color: Colors.redAccent),
          const SizedBox(height: 24),
          const Text(
            'Failed to load document',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          Text(
            'Ensure the ONLYOFFICE server is reachable and the file URL is valid.\n\nServer: $onlyOfficeServer',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey.shade600),
          ),
          const SizedBox(height: 32),
          ElevatedButton.icon(
            onPressed: () => _initializeController(),
            icon: const Icon(Icons.replay),
            label: const Text('Try Again'),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF2C3E50),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
            ),
          ),
        ],
      ),
    );
  }
}
