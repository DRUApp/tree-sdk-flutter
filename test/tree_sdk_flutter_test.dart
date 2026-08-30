import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tree_sdk_flutter/tree_sdk_flutter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('tree_sdk_flutter');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final calls = <MethodCall>[];
  Object? Function(MethodCall)? respond;

  /// Sends a call *from* the native side into the plugin's own handler, the way
  /// the Swift side asks for a token or reports a close.
  Future<Object?> fromNative(String method) async {
    final data = await messenger.handlePlatformMessage(
      channel.name,
      channel.codec.encodeMethodCall(MethodCall(method)),
      null,
    );
    return data == null ? null : channel.codec.decodeEnvelope(data);
  }

  setUp(() {
    // start() refuses to run off iOS/Android; the host running these tests is neither.
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    calls.clear();
    respond = (_) => null;
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return respond!(call);
    });
  });

  tearDown(() {
    debugDefaultTargetPlatformOverride = null;
    messenger.setMockMethodCallHandler(channel, null);
    TreeSdk.debugReset();
  });

  const credentials = TreeClientCredentials(
    clientId: 'client',
    clientSecret: 'secret',
  );

  Future<void> start({
    TreeAuth auth = credentials,
    String treeUserId = 'EG_PATIENT_5496',
    TreeEnvironment environment = TreeEnvironment.development,
    String? baseUrl,
    String languageCode = 'en',
    Set<TreeModule>? modules,
    TreeScreen initial = TreeScreen.explore,
  }) => TreeSdk.start(
    auth: auth,
    treeUserId: treeUserId,
    environment: environment,
    baseUrl: baseUrl,
    languageCode: languageCode,
    modules: modules ?? TreeModule.values.toSet(),
    initial: initial,
  );

  group('start', () {
    test('sends client credentials', () async {
      await start(initial: TreeScreen.diary, modules: {TreeModule.diary});

      final arguments = calls.single.arguments as Map<Object?, Object?>;
      expect(calls.single.method, 'start');
      expect(arguments['auth'], 'clientCredentials');
      expect(arguments['clientId'], 'client');
      expect(arguments['clientSecret'], 'secret');
      expect(arguments['treeUserId'], 'EG_PATIENT_5496');
      expect(arguments['environment'], 'development');
      expect(arguments['initial'], 'diary');
      expect(arguments['modules'], <String>['diary']);
    });

    test('never puts a secret on the wire for token-provider auth', () async {
      await start(auth: TreeTokenProvider(() async => 'jwt'));

      final arguments = calls.single.arguments as Map<Object?, Object?>;
      expect(arguments['auth'], 'tokenProvider');
      expect(arguments.containsKey('clientId'), isFalse);
      expect(arguments.containsKey('clientSecret'), isFalse);
    });

    test('a native failure surfaces with the native code', () {
      respond = (_) => throw PlatformException(
        code: 'unauthorized',
        message: 'Unauthorized',
      );

      expect(
        start(),
        throwsA(
          isA<TreeSdkException>()
              .having((e) => e.code, 'code', 'unauthorized')
              .having((e) => e.message, 'message', 'Unauthorized'),
        ),
      );
    });
  });

  group('rejects bad config before reaching the platform', () {
    test('off iOS and Android', () {
      debugDefaultTargetPlatformOverride = TargetPlatform.windows;
      expect(
        start(),
        throwsA(
          isA<TreeSdkException>().having(
            (e) => e.code,
            'code',
            'unsupported_platform',
          ),
        ),
      );
      expect(calls, isEmpty);
    });

    test('Android reaches the platform same as iOS', () async {
      debugDefaultTargetPlatformOverride = TargetPlatform.android;
      await start(initial: TreeScreen.diary, modules: {TreeModule.diary});
      expect(calls.single.method, 'start');
    });

    test('chat as initial is rejected on Android, allowed on iOS', () {
      debugDefaultTargetPlatformOverride = TargetPlatform.android;
      expect(
        start(initial: TreeScreen.chat),
        throwsA(
          isA<TreeSdkException>().having(
            (e) => e.code,
            'code',
            'unsupported_platform',
          ),
        ),
      );
      expect(calls, isEmpty);

      debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
      expect(start(initial: TreeScreen.chat), completes);
    });

    test('custom environment and baseUrl require each other', () {
      expect(
        start(environment: TreeEnvironment.custom),
        throwsA(isA<TreeSdkException>()),
      );
      expect(
        start(baseUrl: 'http://localhost:3003'),
        throwsA(isA<TreeSdkException>()),
      );
      expect(calls, isEmpty);
    });

    test('empty user id, empty modules, unknown language', () {
      expect(start(treeUserId: '  '), throwsA(isA<TreeSdkException>()));
      expect(start(modules: {}), throwsA(isA<TreeSdkException>()));
      expect(start(languageCode: 'fr'), throwsA(isA<TreeSdkException>()));
      expect(calls, isEmpty);
    });
  });

  group('calls from the native side', () {
    test('getToken returns the host token, per request', () async {
      var asked = 0;
      await start(
        auth: TreeTokenProvider(() async {
          asked++;
          return 'jwt-$asked';
        }),
      );

      expect(await fromNative('getToken'), 'jwt-1');
      expect(await fromNative('getToken'), 'jwt-2');
    });

    test('an empty token is an error, not a token', () async {
      await start(auth: TreeTokenProvider(() async => ''));
      expect(fromNative('getToken'), throwsA(isA<PlatformException>()));
    });

    test('getToken without a token provider is an error', () async {
      await start();
      expect(fromNative('getToken'), throwsA(isA<PlatformException>()));
    });

    test('onClose reaches the stream', () async {
      await start();
      expectLater(TreeSdk.onClose, emits(null));
      await fromNative('onClose');
    });
  });

  group('module gating', () {
    test('an initial screen outside the enabled modules is rejected', () {
      expect(
        start(modules: {TreeModule.medications}, initial: TreeScreen.diary),
        throwsA(
          isA<TreeSdkException>().having(
            (e) => e.code,
            'code',
            'configuration',
          ),
        ),
      );
      expect(
        start(modules: {TreeModule.diary}, initial: TreeScreen.chat),
        throwsA(isA<TreeSdkException>()),
      );
      expect(calls, isEmpty);
    });

    test('reminders needs diary or medications, not nawaf', () {
      expect(TreeScreen.reminders.isAllowedBy({TreeModule.diary}), isTrue);
      expect(
        TreeScreen.reminders.isAllowedBy({TreeModule.medications}),
        isTrue,
      );
      expect(TreeScreen.reminders.isAllowedBy({TreeModule.nawafAi}), isFalse);
    });

    test('explore is never gated', () {
      for (final module in TreeModule.values) {
        expect(TreeScreen.explore.isAllowedBy({module}), isTrue);
      }
    });

    test('a single module still opens its own screen', () async {
      await start(modules: {TreeModule.diary}, initial: TreeScreen.diary);
      final arguments = calls.single.arguments as Map<Object?, Object?>;
      expect(arguments['modules'], <String>['diary']);
      expect(arguments['initial'], 'diary');
    });
  });

  // The Swift side matches these against TreeModule/TreeScreen/TreeEnvironment,
  // so a rename on either side has to break something.
  test('wire values match the Swift enums', () {
    expect(
      TreeModule.values.map((m) => m.wire),
      containsAll(<String>['nawafAI', 'diary', 'medications']),
    );
    expect(
      TreeScreen.values.map((s) => s.wire),
      containsAll(<String>[
        'explore',
        'diary',
        'medications',
        'reminders',
        'chat',
      ]),
    );
    expect(
      TreeEnvironment.values.map((e) => e.wire),
      containsAll(<String>['production', 'staging', 'development', 'custom']),
    );
  });
}
