import 'dart:convert';
import 'dart:io';

/// Mints access tokens for this harness the way a partner's **server** would.
///
/// The SDK no longer accepts client credentials: `TreeAuth.ClientCredentials` has an internal
/// constructor on Android and is compiled out of the iOS binary, so no app can hand a partner
/// secret to it. A host supplies a [TreeTokenProvider] instead, and the exchange below is what
/// belongs behind that provider - on a server in production, and here in the app only because
/// this is the SDK's own test harness pointed at the dev backend.
///
/// The token is cached and reused: the SDK asks for one on **every** request - four to nine times
/// to render Explore alone - so an uncached provider would hammer /auth/token.
class DevTokenService {
  DevTokenService({
    required this.baseUrl,
    required this.clientId,
    required this.clientSecret,
  });

  final String baseUrl;
  final String clientId;
  final String clientSecret;

  String? _token;
  DateTime? _expiresAt;

  /// Refreshed a minute early, so a token cannot expire between being handed over and being used.
  static const _expiryGuard = Duration(seconds: 60);

  Future<String> token() async {
    final cached = _token;
    final expiry = _expiresAt;
    if (cached != null && expiry != null && DateTime.now().isBefore(expiry.subtract(_expiryGuard))) {
      return cached;
    }

    final client = HttpClient();
    try {
      final request = await client.postUrl(Uri.parse('$baseUrl/auth/token'));
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode(<String, String>{
        'client_id': clientId,
        'client_secret': clientSecret,
      }));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw HttpException('POST /auth/token failed (${response.statusCode}): $body');
      }

      final json = jsonDecode(body) as Map<String, dynamic>;
      final token = json['access_token'] as String?;
      if (token == null || token.isEmpty) {
        throw const HttpException('POST /auth/token returned no access_token.');
      }
      final seconds = (json['expires_in'] as num?)?.toInt() ?? 3600;

      _token = token;
      _expiresAt = DateTime.now().add(Duration(seconds: seconds));
      return token;
    } finally {
      client.close();
    }
  }

  /// Drops the cached token, so the next request mints a fresh one.
  void invalidate() {
    _token = null;
    _expiresAt = null;
  }
}
