import SwiftUI
import WebKit

struct HTMLContentView: UIViewRepresentable {
    let html: String
    @Binding var calculatedHeight: CGFloat

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        let wrappedHTML = """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {
                    font-family: Georgia, serif;
                    color: #1f2933;
                    font-size: 17px;
                    line-height: 1.55;
                    margin: 0;
                    padding: 0 2px 24px;
                    background: transparent;
                }
                h3 { color: #5b3417; margin-top: 24px; }
                blockquote {
                    border-left: 4px solid #b88a44;
                    margin: 16px 0;
                    padding: 8px 0 8px 14px;
                    color: #4b5563;
                    background: #fff8ea;
                }
                li { margin-bottom: 8px; }
            </style>
        </head>
        <body>\(html)</body>
        </html>
        """

        webView.loadHTMLString(wrappedHTML, baseURL: nil)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(calculatedHeight: $calculatedHeight)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        @Binding var calculatedHeight: CGFloat

        init(calculatedHeight: Binding<CGFloat>) {
            _calculatedHeight = calculatedHeight
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            webView.evaluateJavaScript("document.body.scrollHeight") { result, _ in
                if let height = result as? NSNumber {
                    DispatchQueue.main.async {
                        self.calculatedHeight = CGFloat(truncating: height) + 24
                    }
                }
            }
        }
    }
}
