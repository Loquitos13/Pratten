/// Configuração da app. A URL da API define-se em build time:
/// `flutter run --dart-define=API_BASE_URL=http://localhost:8080/api`
class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );
}
