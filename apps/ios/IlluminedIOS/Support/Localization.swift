import Foundation

enum IlluminedL10n {
    static func string(_ key: String) -> String {
        NSLocalizedString(key, comment: "")
    }

    static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(
            format: NSLocalizedString(key, comment: ""),
            locale: Locale.current,
            arguments: arguments
        )
    }

    static func count(_ count: Int, singular: String, plural: String) -> String {
        format(count == 1 ? singular : plural, count)
    }
}
