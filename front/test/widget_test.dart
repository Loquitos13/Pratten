import 'package:flutter_test/flutter_test.dart';

import 'package:pratten/main.dart';

void main() {
  testWidgets('arranca a app', (tester) async {
    await tester.pumpWidget(const PrattenApp());
    expect(find.text('Pratten'), findsOneWidget);
  });
}
