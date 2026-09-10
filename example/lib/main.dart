import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tree_sdk_flutter/tree_sdk_flutter.dart';

// --- Dev B2B credentials (see example/README.md). ---
// TreeClientCredentials puts the secret in the app binary — fine for this
// local/dev harness, but ship TreeTokenProvider in a real app.
const _kBaseUrl = 'https://dev-b2b-api.dru-app.org';
const _kClientId = '65768202';
const _kClientSecret =
    '3fdf6d3ee0304e3723b2671e0732794a2e5da17178822f63904d04892b880382';
const _kDefaultTreeUserId = 'EG_PATIENT_5555';

void main() {
  runApp(const TreeDemoApp());
}

class TreeDemoApp extends StatelessWidget {
  const TreeDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Tree',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF6750A4),
        scaffoldBackgroundColor: const Color(0xFFF9F1F8),
      ),
      home: const ConnectPage(),
    );
  }
}

/// A named quick-fill shortcut shown under the Tree user ID field.
///
/// These two IDs are placeholders (the real IDs behind "Ahmed Osman" /
/// "Ali Elgohary" weren't provided) — edit them to match real seeded users
/// on your dev B2B backend, or just type an ID directly into the field.
class _QuickUser {
  const _QuickUser(this.label, this.treeUserId);
  final String label;
  final String treeUserId;
}

const _quickUsers = [
  _QuickUser('Ahmed Osman', 'EG_PATIENT_1001'),
  _QuickUser('Ali Elgohary', 'EG_PATIENT_1002'),
];

class ConnectPage extends StatefulWidget {
  const ConnectPage({super.key});

  @override
  State<ConnectPage> createState() => _ConnectPageState();
}

class _ConnectPageState extends State<ConnectPage> {
  final _treeUserIdController =
      TextEditingController(text: _kDefaultTreeUserId);
  String _languageCode = 'en';
  bool _starting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    // Reopen the SDK straight to the tapped reminder — see the plugin
    // README's "Tapping a reminder notification" section. This covers the warm case, where the
    // app is already running when the notification is tapped.
    TreeSdk.onReminderTapped.listen((_) => _start());
    // And the cold one, where the app is launched BY the tap: the native side fires the event
    // above while this page is still being built, so it is gone before the listener exists.
    // Asking instead is what stops a tapped reminder from stalling on this connect screen.
    // Before the first frame where possible, so the form never flashes on the way through.
    WidgetsBinding.instance.addPostFrameCallback((_) => _startIfLaunchedFromReminder());
    TreeSdk.onClose.listen((_) {
      if (mounted) setState(() => _starting = false);
    });
  }

  @override
  void dispose() {
    _treeUserIdController.dispose();
    super.dispose();
  }

  Future<void> _startIfLaunchedFromReminder() async {
    if (!await TreeSdk.hasPendingReminderTap || !mounted) return;
    _start();
  }

  Future<void> _start() async {
    // A cold start from a notification reaches here twice: once from the pending-tap check above,
    // and once from the onReminderTapped stream, whose event arrives just late enough for the
    // listener to catch it. Both used to run, and each launched its own SDK activity - the second
    // with no deep link left to consume, which is why the log screen ended up buried under a
    // second Explore.
    if (_starting) return;
    final treeUserId = _treeUserIdController.text.trim();
    if (treeUserId.isEmpty) {
      setState(() => _error = 'Enter a Tree user ID first.');
      return;
    }
    setState(() {
      _starting = true;
      _error = null;
    });
    try {
      await TreeSdk.start(
        auth: const TreeClientCredentials(
          clientId: _kClientId,
          clientSecret: _kClientSecret,
        ),
        treeUserId: treeUserId,
        environment: TreeEnvironment.custom,
        baseUrl: _kBaseUrl,
        languageCode: _languageCode,
        initial: TreeScreen.explore,
      );
    } on TreeSdkException catch (e) {
      setState(() => _error = '${e.code}: ${e.message}');
    } on PlatformException catch (e) {
      setState(() => _error = e.message ?? 'The SDK failed to start.');
    } finally {
      if (mounted) setState(() => _starting = false);
    }
  }

  void _pickQuickUser(_QuickUser user) {
    setState(() => _treeUserIdController.text = user.treeUserId);
  }

  @override
  Widget build(BuildContext context) {
    const purple = Color(0xFF6750A4);
    // Launched from a reminder notification: the SDK is already starting, so showing the form -
    // with its Connect button - would invite a tap that is not needed and cannot help. A spinner
    // holds the screen until the SDK's own UI takes over.
    if (_starting) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'TREE',
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: 28),
              TextField(
                controller: _treeUserIdController,
                decoration: const InputDecoration(
                  labelText: 'Tree user ID',
                  border: OutlineInputBorder(),
                ),
              ),
              const Padding(
                padding: EdgeInsets.only(top: 6, left: 4),
                child: Text(
                  'External id on the dev b2b',
                  style: TextStyle(fontSize: 12, color: Colors.black54),
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12,
                runSpacing: 8,
                children: [
                  for (final user in _quickUsers)
                    OutlinedButton(
                      onPressed: () => _pickQuickUser(user),
                      child: Text(user.label),
                    ),
                ],
              ),
              const SizedBox(height: 24),
              _LanguageToggle(
                languageCode: _languageCode,
                onChanged: (code) => setState(() => _languageCode = code),
              ),
              const SizedBox(height: 28),
              if (_error != null) ...[
                Text(_error!, style: const TextStyle(color: Colors.red)),
                const SizedBox(height: 12),
              ],
              SizedBox(
                width: double.infinity,
                height: 52,
                child: FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: purple,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28),
                    ),
                  ),
                  onPressed: _starting ? null : _start,
                  child: _starting
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text('Start', style: TextStyle(fontSize: 16)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LanguageToggle extends StatelessWidget {
  const _LanguageToggle({required this.languageCode, required this.onChanged});

  final String languageCode;
  final ValueChanged<String> onChanged;

  static const _selectedBg = Color(0xFFEDE7F6);
  static const _purple = Color(0xFF6750A4);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: Colors.black26),
      ),
      padding: const EdgeInsets.all(4),
      child: Row(
        children: [
          Expanded(
            child: _segment(
              label: 'English',
              selected: languageCode == 'en',
              onTap: () => onChanged('en'),
            ),
          ),
          Expanded(
            child: _segment(
              label: 'العربية',
              selected: languageCode == 'ar',
              onTap: () => onChanged('ar'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _segment({
    required String label,
    required bool selected,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: selected ? _selectedBg : null,
          borderRadius: BorderRadius.circular(24),
        ),
        alignment: Alignment.center,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (selected) ...[
              const Icon(Icons.check, size: 16, color: _purple),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                color: selected ? _purple : Colors.black87,
                fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
