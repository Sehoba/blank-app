import SwiftUI
import WebKit
import UIKit

@main
struct MoebelSchroederApp: App {
    var body: some Scene {
        WindowGroup {
            WebContainer()
                .ignoresSafeArea(.container, edges: .bottom)
        }
    }
}

struct WebContainer: UIViewRepresentable {
    private let homeURL = URL(string: "https://moebel-schroeder.net/index.html/")!

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.allowsInlineMediaPlayback = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic
        context.coordinator.webView = webView
        webView.load(URLRequest(url: homeURL))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        weak var webView: WKWebView?

        private func isInternal(_ url: URL) -> Bool {
            guard let host = url.host?.lowercased() else { return false }
            return host == "moebel-schroeder.net" || host == "www.moebel-schroeder.net"
        }

        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }

            let scheme = (url.scheme ?? "").lowercased()
            if (scheme == "http" || scheme == "https") && isInternal(url) {
                decisionHandler(.allow)
                return
            }

            if ["http", "https", "mailto", "tel", "sms", "maps"].contains(scheme) {
                UIApplication.shared.open(url)
                decisionHandler(.cancel)
                return
            }

            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
            decisionHandler(.cancel)
        }

        func webView(_ webView: WKWebView,
                     createWebViewWith configuration: WKWebViewConfiguration,
                     for navigationAction: WKNavigationAction,
                     windowFeatures: WKWindowFeatures) -> WKWebView? {
            if navigationAction.targetFrame == nil, let url = navigationAction.request.url {
                if isInternal(url) {
                    webView.load(URLRequest(url: url))
                } else {
                    UIApplication.shared.open(url)
                }
            }
            return nil
        }

        func webView(_ webView: WKWebView,
                     didFailProvisionalNavigation navigation: WKNavigation!,
                     withError error: Error) {
            let html = """
            <!doctype html>
            <html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
            <style>body{font-family:-apple-system,sans-serif;text-align:center;padding:60px 24px;color:#222}button{font-size:17px;padding:12px 18px;border:0;border-radius:10px;background:#5A3E2B;color:white}</style></head>
            <body><h2>Keine Verbindung</h2><p>Die Möbelschröder-Webseite konnte gerade nicht geladen werden.</p><button onclick=\"location.href='https://moebel-schroeder.net/index.html/'\">Erneut versuchen</button></body></html>
            """
            webView.loadHTMLString(html, baseURL: nil)
        }
    }
}
