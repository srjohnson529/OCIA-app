# Explore Illumined — real-page instructor walkthrough

New instructors receive a “Would you like to explore Illumined?” invitation after profile setup on their initial sign-in. Eligibility uses Firebase Auth creation/last-sign-in metadata (within two minutes), instructor role, and an account-scoped v2 offered marker on the device/browser. Existing instructors are not interrupted. The marker is local, not cloud synchronized.

Six bilingual coachmarks navigate actual pages: welcome/class ID, personal lesson progress, Lessons, Discussion, Formation, and More/Instructor Tools. Callouts outline live cards or page navigation; native Lesson Tracker and web lesson counters open Lessons. Nothing is automatically published, completed, acknowledged, or sent. The normal pages still load their real classroom data.

Explore begins the tour; Not now dismisses the invitation. Back, Next, Skip and Finish are available. Instructor Tools retains a replay launcher, not the former sample-description screen. Replays start on Home. Daily Formation and instructor startup updates wait until the invitation/tour ends. Web startup is guarded against repeated profile refreshes and account changes.

Implementation:
- Web: public/instructor-tour.js, integrated by Catechism app.html.
- iOS: Views/InstructorWalkthrough.swift, MainTabView and real card anchors.
- Android: ui/InstructorWalkthrough.kt, FormationHome and real card anchors.
The previous v1 sample views/resources are no longer linked from the user flow.

instructor-tour.test.cjs checks invitation eligibility, navigation, Back/Skip/replay, Spanish, storage failures, account changes, missing targets and startup sequencing. Android compilation and iOS syntax parsing are separate from full iOS/device visual testing. Before release, test a genuinely new instructor account and an existing instructor replay on narrow screens, both languages, large text, and with Daily Formation/startup updates enabled. No production account creation or deployment is part of this work.
