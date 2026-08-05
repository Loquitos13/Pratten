import 'package:flutter/material.dart';

import 'config.dart';

void main() {
  runApp(const PrattenApp());
}

class PrattenApp extends StatelessWidget {
  const PrattenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pratten',
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(title: const Text('Pratten')),
        body: Center(
          child: Text('Frontend em preparação\n${AppConfig.apiBaseUrl}'),
        ),
      ),
    );
  }
}
