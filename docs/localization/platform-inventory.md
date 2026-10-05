# Localization Platform Inventory

Inventory date: 2026-08-26

## iOS

- The Xcode project currently declares English and Base as its known regions.
- No `.xcstrings`, `.strings`, or `.stringsdict` localization resources exist yet.
- The iOS application contains approximately 369 direct SwiftUI `Text`, `Button`, `Label`, and navigation-title string usages across 49 Swift files.
- Recommended first migration: add a String Catalog and translate authentication, the five primary tabs, and shared actions. Keep interpolated status/error text in explicitly named localization keys.

## Android

- Android has the standard resource mechanism, but `values/strings.xml` currently contains only the application name and notification-channel identifier.
- Most Compose interface text is hardcoded in Kotlin across 13 UI-bearing source files.
- No `values-es/strings.xml` resource exists yet.
- Recommended first migration: move authentication, primary navigation, and shared actions into named resources, then provide matching Spanish resources. Preserve the notification-channel identifier as non-translatable.

## Web application

- The main web application is a single large English HTML/JavaScript surface with no translation dictionary or locale selector.
- The current document language is `en`.
- Approximately 343 visible English text nodes exist in the main page before counting dynamically generated JavaScript messages.
- Recommended first migration: introduce an English/Spanish dictionary with an English fallback and a locale resolver. Migrate login and primary navigation before instructor-management pages.

## Canonical lesson and formation content

- Lesson and formation catalogs are currently English-only JSON.
- The same lesson data is copied into web and Android hosting/application locations.
- Localized fields must remain attached to the existing canonical IDs; creating separate Spanish lesson IDs would split assignments and completion history.
- Scripture quotations and official Church texts require a separate editorial and rights review before publication.

## First code batch

The first code batch should contain only:

1. English and Spanish resources for authentication.
2. English and Spanish resources for the five primary navigation tabs.
3. English and Spanish resources for shared actions such as Save, Cancel, Back, Continue, and Sign Out.
4. English fallback tests and a Spanish-resource completeness check.

No lessons, instructor-authored content, database fields, or canonical identifiers should change in that batch.

