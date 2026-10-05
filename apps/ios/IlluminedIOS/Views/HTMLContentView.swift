import SwiftUI
import WebKit

struct HTMLContentView: UIViewRepresentable {
    let html: String
    @Binding var calculatedHeight: CGFloat
    var onWalkthroughSections: (([WalkthroughLessonSection])->Void)? = nil

    func makeUIView(context: Context) -> WKWebView {
        let configuration=WKWebViewConfiguration()
        configuration.userContentController.add(context.coordinator,name:"lessonLayout")
        let webView = WKWebView(frame:.zero,configuration:configuration)
        webView.navigationDelegate = context.coordinator
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onSections=onWalkthroughSections
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
        <body>\(html)
        <script>
        (() => {
            const report = () => {
                const headings = [...document.querySelectorAll('h2,h3,h4')];
                window.webkit.messageHandlers.lessonLayout.postMessage({
                    height: document.body.getBoundingClientRect().height,
                    sections: headings.map((heading,index) => ({
                        id: 'lesson-part-' + index,
                        title: heading.textContent.trim(),
                        top: heading.getBoundingClientRect().top + window.scrollY,
                        height: heading.getBoundingClientRect().height
                    }))
                });
            };
            new ResizeObserver(report).observe(document.body);
            window.addEventListener('resize',report);
            window.addEventListener('load',report);
            report();
        })();
        </script></body>
        </html>
        """

        guard context.coordinator.loadedHTML != wrappedHTML else {return}
        context.coordinator.loadedHTML=wrappedHTML
        webView.loadHTMLString(wrappedHTML, baseURL: nil)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(calculatedHeight: $calculatedHeight)
    }

    static func dismantleUIView(_ webView:WKWebView,coordinator:Coordinator) {
        coordinator.onSections=nil
        webView.configuration.userContentController.removeScriptMessageHandler(forName:"lessonLayout")
        webView.navigationDelegate=nil
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        @Binding var calculatedHeight: CGFloat
        var loadedHTML: String?
        var onSections: (([WalkthroughLessonSection])->Void)?

        init(calculatedHeight: Binding<CGFloat>) {
            _calculatedHeight = calculatedHeight
        }

        func userContentController(_ userContentController:WKUserContentController,didReceive message:WKScriptMessage) {
            guard message.frameInfo.isMainFrame, let data=message.body as? [String:Any],
                  let height=data["height"] as? NSNumber else {return}
            let sections=(data["sections"] as? [[String:Any]] ?? []).compactMap { value -> WalkthroughLessonSection? in
                guard let id=value["id"] as? String, let title=value["title"] as? String,
                      let top=value["top"] as? NSNumber, let height=value["height"] as? NSNumber else {return nil}
                return WalkthroughLessonSection(id:id,title:title,top:CGFloat(truncating:top),height:CGFloat(truncating:height))
            }
            DispatchQueue.main.async {
                let newHeight=CGFloat(truncating:height)+24
                if abs(self.calculatedHeight-newHeight)>0.5 {self.calculatedHeight=newHeight}
                self.onSections?(sections)
            }
        }

    }
}
