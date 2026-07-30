package com.illumined.app.ui

internal object LessonReaderPolicy {
    fun wrapHtml(content: String) = """
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
                img { max-width: 100%; height: auto; }
            </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}
