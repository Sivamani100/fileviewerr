import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';
import 'package:audio_video_progress_bar/audio_video_progress_bar.dart';

class ArkioAudioViewer extends StatefulWidget {
  final String filePath;
  const ArkioAudioViewer({super.key, required this.filePath});

  @override
  State<ArkioAudioViewer> createState() => _ArkioAudioViewerState();
}

class _ArkioAudioViewerState extends State<ArkioAudioViewer> {
  final _player = AudioPlayer();
  bool _ready = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    try {
      await _player.setFilePath(widget.filePath);
      if (mounted) setState(() => _ready = true);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) return Center(child: Text('Cannot play: $_error'));
    if (!_ready) return const Center(child: CircularProgressIndicator());

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.music_note, size: 100, color: Color(0xFF2563EB)),
            const SizedBox(height: 32),
            StreamBuilder<Duration?>(
              stream: _player.durationStream,
              builder: (_, snapDuration) {
                return StreamBuilder<Duration>(
                  stream: _player.positionStream,
                  builder: (_, snapPos) {
                    return ProgressBar(
                      progress: snapPos.data ?? Duration.zero,
                      total: snapDuration.data ?? Duration.zero,
                      onSeek: _player.seek,
                      progressBarColor: const Color(0xFF2563EB),
                      thumbColor: const Color(0xFF2563EB),
                    );
                  },
                );
              },
            ),
            const SizedBox(height: 24),
            StreamBuilder<PlayerState>(
              stream: _player.playerStateStream,
              builder: (_, snap) {
                final playing = snap.data?.playing ?? false;
                return Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      iconSize: 32,
                      icon: const Icon(Icons.replay_10),
                      onPressed: () => _player.seek(
                        (_player.position) - const Duration(seconds: 10),
                      ),
                    ),
                    IconButton(
                      iconSize: 64,
                      icon: Icon(playing ? Icons.pause_circle : Icons.play_circle),
                      onPressed: playing ? _player.pause : _player.play,
                    ),
                    IconButton(
                      iconSize: 32,
                      icon: const Icon(Icons.forward_10),
                      onPressed: () => _player.seek(
                        (_player.position) + const Duration(seconds: 10),
                      ),
                    ),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
