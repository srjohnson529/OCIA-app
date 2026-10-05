# Illumined Spanish Localization

## Locale and fallback

- The first Spanish locale is neutral U.S./Latin American Spanish: `es`.
- English remains the canonical fallback whenever a Spanish string is missing.
- User-entered names, classroom names, announcements, assignments, and discussion posts remain exactly as entered.
- Brand names, database identifiers, lesson identifiers, classroom identifiers, completion records, and notification types are never translated.

## Catholic-content rules

- Use the shared glossary in `es-glossary.csv` for interface and instructional terminology.
- Do not machine-translate quoted Scripture. Use an approved Spanish Bible text with confirmed publication rights, or provide a clearly labeled summary.
- For direct Catechism quotations, use the official Spanish text of the *Catecismo de la Iglesia Católica*. Existing summaries may be translated as summaries rather than presented as quotations.
- Preserve paragraph numbers, Scripture citations, lesson IDs, quiz IDs, and assignment IDs across languages.
- Retain established Latin expressions such as *Lectio Divina* where the glossary specifies them.
- Have sacramental, liturgical, and OCIA/OICA terminology reviewed by a fluent Catholic catechist before release.

## Safe implementation order

1. App shell: authentication, primary navigation, common buttons, errors, and accessibility labels.
2. Student workflow: dashboard, assignments, lessons, quizzes, discussions, and formation.
3. Instructor workflow: classroom settings, lesson tools, Daily Formation, schedules, and CSV import.
4. Canonical content: lesson titles, lesson bodies, quiz text, prayer text, and formation catalogs.
5. Web management application and public join/download pages.
6. Layout, VoiceOver/TalkBack, notification, CSV, and fallback verification.

Each phase must keep English behavior unchanged and must be independently buildable and releasable.

## Canonical content model

Localized content should share one canonical identity. For example, a lesson remains `lesson-123` in every language while its display fields vary by locale:

```json
{
  "id": "lesson-123",
  "title": "The Eucharist",
  "contentHTML": "...",
  "localizations": {
    "es": {
      "title": "La Eucaristía",
      "contentHTML": "..."
    }
  }
}
```

This preserves assignments, quiz results, completion history, analytics, and classroom links when a user changes languages.

