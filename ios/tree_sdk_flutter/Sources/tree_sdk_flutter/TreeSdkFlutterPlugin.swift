import Flutter
import SwiftUI
import TreeSDK
import UIKit

/// Presents the native SDK over the Flutter view.
///
/// One method in, `start`: initialize `TreeSDK` from the Dart-supplied config,
/// then present `TreeRootView` in a `UIHostingController`. The SDK owns
/// navigation from there and dismisses itself back to Flutter.
///
/// One method out, `getToken`: when the host chose token-provider auth, the SDK
/// asks Dart for a bearer token. It is called on *every* request, so the Dart
/// side is expected to cache.
public class TreeSdkFlutterPlugin: NSObject, FlutterPlugin {
    private let channel: FlutterMethodChannel

    /// Set while the SDK is on screen, so a second tap doesn't stack a
    /// duplicate presentation on top of the first.
    private weak var presented: UIViewController?

    init(channel: FlutterMethodChannel) {
        self.channel = channel
    }

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "tree_sdk_flutter",
                                           binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(TreeSdkFlutterPlugin(channel: channel), channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "start":
            guard let arguments = call.arguments as? [String: Any] else {
                result(Self.error("configuration", "start expects a map of arguments."))
                return
            }
            start(arguments, result: result)
        case "isPresented":
            result(presented != nil)
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    // MARK: - start

    private func start(_ arguments: [String: Any], result: @escaping FlutterResult) {
        if presented != nil {
            // Already up. The Dart caller got its answer the first time.
            result(nil)
            return
        }
        let config: TreeSDKConfig
        switch parseConfig(from: arguments) {
        case let .failure(message):
            result(Self.error("configuration", message))
            return
        case let .success(parsed):
            config = parsed
        }

        Task { @MainActor in
            switch await TreeSDK.initialize(config) {
            case let .err(failure):
                result(Self.error(Self.code(for: failure), failure.message))
            case let .ok(sdk):
                self.present(sdk, initial: Self.screen(from: arguments), result: result)
            }
        }
    }

    @MainActor
    private func present(_ sdk: TreeSDK, initial: TreeScreen, result: @escaping FlutterResult) {
        guard let host = Self.topViewController() else {
            result(Self.error("no_window", "No view controller to present from."))
            return
        }
        // `controller` is weak on purpose: the hosting controller owns the root
        // view, which owns this closure. A strong capture would retain the
        // controller through its own view and leak it on every open.
        weak var controller: UIViewController?
        let root = TreeRootView(sdk: sdk, initial: initial) { [weak self] in
            controller?.dismiss(animated: true)
            self?.presented = nil
            self?.channel.invokeMethod("onClose", arguments: nil)
        }
        let hosting = UIHostingController(rootView: root)
        hosting.modalPresentationStyle = .fullScreen
        controller = hosting
        presented = hosting
        host.present(hosting, animated: true) { result(nil) }
    }

    // MARK: - Token provider

    /// Bridges `TreeTokenProvider` to a `getToken` call on the Dart side.
    private func hostToken() async throws -> String {
        try await withCheckedThrowingContinuation { continuation in
            // Platform channels are main-thread only.
            DispatchQueue.main.async {
                self.channel.invokeMethod("getToken", arguments: nil) { response in
                    switch response {
                    case let token as String where !token.isEmpty:
                        continuation.resume(returning: token)
                    case let error as FlutterError:
                        continuation.resume(throwing: TokenProviderError(
                            "\(error.code): \(error.message ?? "token provider failed")"))
                    default:
                        // Also covers FlutterMethodNotImplemented, the sentinel
                        // returned when Dart has no handler installed.
                        continuation.resume(throwing: TokenProviderError(
                            "The token provider returned no usable token."))
                    }
                }
            }
        }
    }

    private struct TokenProviderError: LocalizedError {
        let reason: String
        init(_ reason: String) { self.reason = reason }
        var errorDescription: String? { reason }
    }

    // MARK: - Argument parsing

    private enum Parsed {
        case success(TreeSDKConfig)
        case failure(String)
    }

    private func parseConfig(from arguments: [String: Any]) -> Parsed {
        guard let treeUserID = arguments["treeUserId"] as? String else {
            return .failure("treeUserId is required.")
        }

        let auth: TreeAuth
        switch arguments["auth"] as? String {
        case "tokenProvider":
            auth = .tokenProvider { [self] in try await hostToken() }
        case "clientCredentials":
            guard
                let clientID = arguments["clientId"] as? String,
                let clientSecret = arguments["clientSecret"] as? String
            else {
                return .failure("clientCredentials auth requires clientId and clientSecret.")
            }
            auth = .clientCredentials(clientID: clientID, clientSecret: clientSecret)
        default:
            return .failure("auth must be 'tokenProvider' or 'clientCredentials'.")
        }

        let environmentName = arguments["environment"] as? String ?? "production"
        let baseURL = arguments["baseUrl"] as? String
        let environment: TreeEnvironment
        switch environmentName {
        case "production": environment = .production
        case "staging": environment = .staging
        case "development": environment = .development
        case "custom":
            guard let baseURL, !baseURL.isEmpty else {
                return .failure("environment 'custom' requires baseUrl.")
            }
            environment = .custom(baseURL)
        default:
            return .failure("Unknown environment '\(environmentName)'.")
        }

        let wireModules = arguments["modules"] as? [String] ?? []
        let modules = Set(wireModules.compactMap(TreeModule.init(rawValue:)))
        if modules.count != wireModules.count {
            return .failure("Unknown module in \(wireModules).")
        }

        return .success(TreeSDKConfig(
            auth: auth,
            treeUserID: treeUserID,
            environment: environment,
            enabledModules: modules.isEmpty ? Set(TreeModule.allCases) : modules,
            languageCode: arguments["languageCode"] as? String ?? "en",
            userName: arguments["userName"] as? String,
            gender: (arguments["gender"] as? String).flatMap(TreeGender.init(rawValue:))
        ))
    }

    private static func screen(from arguments: [String: Any]) -> TreeScreen {
        (arguments["initial"] as? String).flatMap(TreeScreen.init(rawValue:)) ?? .explore
    }

    private static func error(_ code: String, _ message: String) -> FlutterError {
        FlutterError(code: code, message: message, details: nil)
    }

    /// Maps the SDK's typed failure onto the `PlatformException.code` Dart sees.
    private static func code(for failure: TreeFailure) -> String {
        switch failure {
        case .network: return "network"
        case .timeout: return "timeout"
        case .unauthorized: return "unauthorized"
        case .forbidden: return "forbidden"
        case .notFound: return "not_found"
        case .validation: return "validation"
        case .server: return "server"
        case .cancelled: return "cancelled"
        case .configuration: return "configuration"
        case .unknown: return "unknown"
        }
    }

    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        guard var top = (scene?.keyWindow ?? scene?.windows.first)?.rootViewController else {
            return nil
        }
        while let next = top.presentedViewController { top = next }
        return top
    }
}
