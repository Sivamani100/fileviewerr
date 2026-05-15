import 'dart:io';
import 'package:flutter/material.dart';
import 'package:chewie/chewie.dart';
import 'package:video_player/video_player.dart';

class ArkioVideoViewer extends StatefulWidget {
  final String filePath;
  const ArkioVideoViewer({super.key, required this.filePath});

  @override
  State<ArkioVideoViewer> createState() => _ArkioVideoViewerState();
}

class _ArkioVideoViewerState extends State<ArkioVideoViewer> {
  VideoPlayerController? _vpc;
  ChewieController? _cc;
  String? _error;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    try {
      _vpc = VideoPlayerController.file(File(widget.filePath));
      await _vpc!.initialize();
      _cc = ChewieController(
        videoPlayerController: _vpc!,
        autoPlay: true,
        allowFullScreen: true,
        allowMuting: true,
        showControls: true,
      );
      if (mounted) setState(() {});
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  @override
  void dispose() {
    _vpc?.dispose();
    _cc?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) {
      return Center(child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.videocam_off, size: 64, color: Colors.grey),
          const SizedBox(height: 16),
          Text('Cannot play this video format.\n$_error', textAlign: TextAlign.center),
        ],
      ));
    }
    if (_cc == null) return const Center(child: CircularProgressIndicator());
    return Chewie(controller: _cc!);
  }
}
