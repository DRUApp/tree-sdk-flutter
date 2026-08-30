/// Flutter bridge to the native Tree SDK: Nawaf AI chat, health diary,
/// medications and reminders, backed by the Dru B2B API.
///
/// The SDK owns the whole screen. [TreeSdk.start] initializes it and presents
/// its UI over the Flutter view; it returns once the SDK is *on screen*, not
/// when the user leaves it. Listen for [TreeSdk.onClose] to know that.
///
/// **iOS and Android.** Everywhere else, [TreeSdk.start] throws
/// [TreeSdkException] with code `unsupported_platform`. On Android,
/// [TreeScreen.chat] is not yet a valid [start]'s `initial` screen — Nawaf is
/// reached through its floating button instead of a dedicated screen there —
/// and throws the same code.
library;

import 'dart:async';

import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform, visibleForTesting;
import 'package:flutter/services.dart';

/// A feature module the host can enable. Wire values must match Swift's
/// `TreeModule` raw values.
enum TreeModule {
  nawafAi('nawafAI'),
  diary('diary'),
  medications('medications');

  const TreeModule(this.wire);
  final String wire;
}

/// The screen the SDK opens on. Wire values must match Swift's `TreeScreen`
/// raw values.
enum TreeScreen {
  explore('explore'),
  diary('diary'),
  medications('medications'),
  reminders('reminders'),
  chat('chat');

  const TreeScreen(this.wire);
  final String wire;

  /// Whether [modules] permits opening on this screen. Mirrors Swift's
  /// `TreeModuleAccess.allows`, which gates navigation and hides entry points.
  bool isAllowedBy(Set<TreeModule> modules) => switch (this) {
    // The hub is never gated; its cards are, individually.
    TreeScreen.explore => true,
    TreeScreen.diary => modules.contains(TreeModule.diary),
    TreeScreen.medications => modules.contains(TreeModule.medications),
    // Reminders is not a module of its own — it reminds you to log readings
    // or take medicines, so it needs at least one of them.
    TreeScreen.reminders =>
      modules.contains(TreeModule.diary) ||
          modules.contains(TreeModule.medications),
    TreeScreen.chat => modules.contains(TreeModule.nawafAi),
  };
}

/// Which backend the SDK talks to. Wire values must match Swift's
/// `TreeEnvironment` cases.
enum TreeEnvironment {
  production('production'),
  staging('staging'),
  development('development'),

  /// Requires [TreeSdk.start]'s `baseUrl`.
  custom('custom');

  const TreeEnvironment(this.wire);
  final String wire;
}

/// How the SDK obtains a backend access token.
sealed class TreeAuth {
  const TreeAuth();
}

/// The host supplies tokens. This is the production option: the partner secret
/// stays on your server and never reaches the app binary.
///
/// [getToken] is called on **every** backend request, so return a cached token
/// and only refresh it when it is close to expiring. Throwing from it fails the
/// request with `unauthorized`.
class TreeTokenProvider extends TreeAuth {
  const TreeTokenProvider(this.getToken);
  final Future<String> Function() getToken;
}

/// The SDK exchanges partner credentials itself.
///
/// This puts the partner secret in the app binary, where anyone who unzips the
/// build can read it. Use it for local development only; ship
/// [TreeTokenProvider].
class TreeClientCredentials extends TreeAuth {
  const TreeClientCredentials({
    required this.clientId,
    required this.clientSecret,
  });
  final String clientId;
  final String clientSecret;
}

/// Anything that stopped the SDK from starting.
///
/// [code] is the native `TreeFailure` case — `unauthorized`, `network`,
/// `timeout`, `forbidden`, `not_found`, `validation`, `server`, `cancelled`,
/// `configuration`, `unknown` — or one of this plugin's own:
/// `unsupported_platform`, `no_window`.
class TreeSdkException implements Exception {
  TreeSdkException(this.code, this.message);

  final String code;
  final String message;

  @override
  String toString() => 'TreeSdkException($code): $message';
}

/// Entry point. See the README for host requirements.
class TreeSdk {
  TreeSdk._();

  static const MethodChannel _channel = MethodChannel('tree_sdk_flutter');

  static TreeAuth? _auth;
  static bool _handlerInstalled = false;
  static final StreamController<void> _closed =
      StreamController<void>.broadcast();

  /// Fires when the user leaves the SDK and the host's UI is visible again.
  static Stream<void> get onClose => _closed.stream;

  /// Whether the SDK's UI is currently on screen.
  static Future<bool> get isPresented async =>
      await _channel.invokeMethod<bool>('isPresented') ?? false;

  /// Initializes the SDK and presents its UI.
  ///
  /// Calling this while the SDK is already on screen does nothing.
  static Future<void> start({
    required TreeAuth auth,
    required String treeUserId,
    TreeEnvironment environment = TreeEnvironment.production,
    String? baseUrl,
    String languageCode = 'en',
    String? userName,
    String? gender,
    Set<TreeModule> modules = const {
      TreeModule.nawafAi,
      TreeModule.diary,
      TreeModule.medications,
    },
    TreeScreen initial = TreeScreen.explore,
  }) async {
    switch (defaultTargetPlatform) {
      case TargetPlatform.iOS:
      case TargetPlatform.android:
        break;
      default:
        throw TreeSdkException(
          'unsupported_platform',
          'The Tree SDK supports iOS and Android only.',
        );
    }
    if (defaultTargetPlatform == TargetPlatform.android &&
        initial == TreeScreen.chat) {
      throw TreeSdkException(
        'unsupported_platform',
        'TreeScreen.chat is not available as an initial screen on Android '
            'yet; Nawaf is reached through its floating button instead of a '
            'dedicated chat screen there.',
      );
    }
    if (treeUserId.trim().isEmpty) {
      throw TreeSdkException('configuration', 'treeUserId must not be empty.');
    }
    if ((environment == TreeEnvironment.custom) != (baseUrl != null)) {
      throw TreeSdkException(
        'configuration',
        'TreeEnvironment.custom requires baseUrl, and baseUrl requires it.',
      );
    }
    if (modules.isEmpty) {
      throw TreeSdkException('configuration', 'modules must not be empty.');
    }
    if (languageCode != 'en' && languageCode != 'ar') {
      throw TreeSdkException(
        'configuration',
        "languageCode must be 'en' or 'ar'; got '$languageCode'.",
      );
    }
    if (!initial.isAllowedBy(modules)) {
      throw TreeSdkException(
        'configuration',
        'initial ${initial.name} is not reachable with modules '
            '${modules.map((m) => m.name).toList()}.',
      );
    }

    _auth = auth;
    _installHandler();

    try {
      await _channel.invokeMethod<void>('start', <String, Object?>{
        'auth': switch (auth) {
          TreeTokenProvider() => 'tokenProvider',
          TreeClientCredentials() => 'clientCredentials',
        },
        if (auth is TreeClientCredentials) ...<String, Object?>{
          'clientId': auth.clientId,
          'clientSecret': auth.clientSecret,
        },
        'treeUserId': treeUserId,
        'environment': environment.wire,
        'baseUrl': baseUrl,
        'languageCode': languageCode,
        'userName': userName,
        'gender': gender,
        'modules': modules.map((module) => module.wire).toList(),
        'initial': initial.wire,
      });
    } on PlatformException catch (error) {
      throw TreeSdkException(
        error.code,
        error.message ?? 'The SDK failed to start.',
      );
    }
  }

  /// Answers the native side's calls: `getToken` when the SDK needs a bearer
  /// token, `onClose` when the user leaves its UI.
  static void _installHandler() {
    if (_handlerInstalled) return;
    _handlerInstalled = true;
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'getToken':
          final auth = _auth;
          if (auth is! TreeTokenProvider) {
            throw PlatformException(
              code: 'no_token_provider',
              message:
                  'The SDK asked for a token but auth is not a TreeTokenProvider.',
            );
          }
          final token = await auth.getToken();
          if (token.isEmpty) {
            throw PlatformException(
              code: 'empty_token',
              message: 'The token provider returned an empty token.',
            );
          }
          return token;
        case 'onClose':
          _closed.add(null);
          return null;
        default:
          throw MissingPluginException('Unhandled call ${call.method}.');
      }
    });
  }

  /// Test seam: resets the cached auth and channel handler.
  @visibleForTesting
  static void debugReset() {
    _auth = null;
    _handlerInstalled = false;
    _channel.setMethodCallHandler(null);
  }
}
