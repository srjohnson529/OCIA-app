import Foundation
import Testing
@testable import IlluminedIOS

struct InstructorWalkthroughTests {
    @Test @MainActor func editedTextPreviewAndFallbackDoNotChangeOnboarding() {
        let tour=InstructorWalkthrough()
        tour.start()
        let original=tour.copy(false)
        let published=WalkthroughText(title:"Published",body:"Explanation",titleEs:"Publicado",bodyEs:"Explicación")
        tour.setPublishedText(["welcome":published])
        #expect(tour.copy(false).0 == "Published")
        #expect(tour.copy(true).0 == "Publicado")
        let draft=WalkthroughText(title:"Draft",body:"Preview",titleEs:"Borrador",bodyEs:"Vista previa")
        tour.preview(["welcome":draft],target:"welcome")
        #expect(tour.copy(false).0 == "Draft")
        let state=tour.onboardingState
        tour.dismiss()
        #expect(tour.onboardingState == state)
        tour.start()
        #expect(tour.copy(false).0 == "Published")
        tour.preview([:],target:"welcome")
        #expect(tour.copy(false).0 == original.0)
        tour.complete()
        #expect(tour.onboardingState == state)
        #expect(WalkthroughText.decode(["welcome":["title":"Invalid"]]).isEmpty)
        #expect(!WalkthroughText(title:"",body:"Text",titleEs:"Título",bodyEs:"Texto").valid)
    }
    @Test @MainActor func universalOfferCanReopenDismissedTourOnlyOncePerRevision() {
        let suite = "walkthrough-tests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let tour = InstructorWalkthrough(defaults: defaults)
        tour.prepare("teacher", instructor: true, newlyCreated: false)
        tour.dismiss()
        tour.offerRevision("revision-one")
        #expect(tour.invitation)
        tour.dismiss()
        let reopened = InstructorWalkthrough(defaults: defaults)
        reopened.prepare("teacher", instructor: true, newlyCreated: false)
        reopened.offerRevision("revision-one")
        #expect(!reopened.invitation)
        reopened.offerRevision("revision-two")
        #expect(reopened.invitation)
        reopened.start()
        let target = reopened.target
        reopened.offerRevision("revision-three")
        #expect(reopened.target == target)
        #expect(!reopened.invitation)
    }
    @Test @MainActor func skippedTourRemainsUntilExplicitlyDismissedOrCompleted() {
        let suite = "walkthrough-tests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let tour = InstructorWalkthrough(defaults: defaults)
        tour.prepare("new", instructor: true, newlyCreated: true)
        #expect(tour.invitation)
        #expect(!tour.showsToolEntry)
        tour.skipForNow()
        #expect(tour.showsToolEntry)
        let reopened = InstructorWalkthrough(defaults: defaults)
        reopened.prepare("new", instructor: true, newlyCreated: false)
        #expect(!reopened.invitation)
        #expect(reopened.showsToolEntry)
        reopened.start()
        #expect(!reopened.showsToolEntry)
        reopened.dismiss()
        #expect(!reopened.showsToolEntry)
        let dismissed = InstructorWalkthrough(defaults: defaults)
        dismissed.prepare("new", instructor: true, newlyCreated: true)
        #expect(!dismissed.invitation)
        #expect(!dismissed.showsToolEntry)
        dismissed.prepare("other", instructor: true, newlyCreated: true)
        #expect(dismissed.invitation)
        dismissed.start()
        dismissed.go("codes-instructor-status")
        dismissed.next()
        #expect(dismissed.onboardingState == "completed")
        #expect(!dismissed.showsToolEntry)
    }

    @Test @MainActor func setupMarkerSupportsOlderAccountsButNotStudents() {
        let suite = "walkthrough-tests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        defaults.set(true, forKey: "walkthrough-older-setup-pending")
        let tour = InstructorWalkthrough(defaults: defaults)
        tour.prepare("older", instructor: false, newlyCreated: false)
        #expect(!tour.invitation)
        tour.prepare("older", instructor: true, newlyCreated: false)
        #expect(tour.invitation)
        tour.skipForNow()
        tour.prepare("older", instructor: true, newlyCreated: false)
        #expect(!tour.invitation)
        #expect(tour.showsToolEntry)
    }

    @Test @MainActor func moreCardsFollowMenuOrderThenOpenInstructorTools() {
        let tour = InstructorWalkthrough()
        tour.start()
        tour.go("more")
        let targets = ["more", "more-awards", "more-chat", "more-account",
                       "more-games", "more-guides"]
        #expect(!tour.steps.contains { $0.target == "more-admin" })
        for target in targets {
            #expect(tour.target == target)
            #expect(tour.page == "more")
            #expect(!tour.copy(true).1.isEmpty)
            #expect(!tour.isLast)
            tour.next()
        }
        #expect(tour.target == "tools-overview")
        #expect(tour.screen == "instructor-tools")
        #expect(!tour.animatesStep)
        tour.back()
        #expect(tour.target == "more-guides")
    }

    @Test @MainActor func adminCardIsOptionalBeforeInstructorTools() {
        let tour = InstructorWalkthrough()
        tour.configureMore(admin: true)
        tour.start()
        tour.go("more-guides")
        #expect(!tour.isLast)
        tour.next()
        #expect(tour.target == "more-admin")
        #expect(!tour.isLast)
        #expect(tour.animatesStep)
        tour.configureMore(admin: false)
        #expect(tour.target == "more-guides")
        #expect(!tour.isLast)
    }

    @Test @MainActor func instructorToolsCoverEveryCardAndFinishSafely() {
        let tour = InstructorWalkthrough()
        tour.start()
        tour.go("tools-overview")
        let targets = ["tools-overview", "tools-announcements",
                       "tools-assignments", "tools-discussions", "tools-students", "tools-schedule",
                       "tools-daily", "tools-guides", "tools-classes", "tools-codes", "tools-updates"]
        for target in targets {
            #expect(tour.target == target)
            #expect(tour.screen == "instructor-tools")
            #expect(tour.page == "more")
            #expect(tour.canAdvance)
            #expect(!tour.copy(true).1.isEmpty)
            #expect(!tour.isLast)
            tour.next()
        }
        #expect(tour.target == "codes-overview")
        #expect(!tour.animatesStep)
        tour.start()
        tour.go("tools-assignments")
        tour.back()
        #expect(tour.target == "tools-announcements")
        #expect(tour.animatesStep)
        tour.stop()
        #expect(!tour.active)
    }

    @Test @MainActor func classroomCodesExplainBothRolesBeforeFinishing() {
        let tour = InstructorWalkthrough()
        tour.start()
        tour.go("codes-overview")
        tour.back()
        #expect(tour.target == "tools-updates")
        tour.next()
        #expect(!tour.animatesStep)
        let targets = ["codes-overview", "codes-student-create", "codes-student-share",
                       "codes-student-join", "codes-student-renew", "codes-instructor-create",
                       "codes-instructor-share", "codes-instructor-join", "codes-instructor-status"]
        for target in targets {
            #expect(tour.target == target)
            #expect(tour.screen == "classroom-codes")
            #expect(tour.page == "more")
            #expect(tour.canAdvance)
            #expect(!tour.copy(true).1.isEmpty)
            if target == "codes-instructor-status" { #expect(tour.isLast) }
            tour.next()
        }
        #expect(!tour.active)
    }

    @Test @MainActor func formationCardsFollowTheMenuAndSupportBackNavigation() {
        let tour = InstructorWalkthrough()
        tour.start()
        tour.go("discussion")
        tour.next()
        #expect(!tour.animatesStep)
        let targets = ["formation", "formation-examination", "formation-mass",
                       "formation-practices", "formation-daily", "formation-daily-open"]
        for target in targets {
            #expect(tour.target == target)
            #expect(tour.page == "formation")
            #expect(tour.canAdvance)
            #expect(!tour.copy(true).0.isEmpty)
            tour.next()
        }
        #expect(tour.target == "more")
        #expect(!tour.animatesStep)
        tour.back()
        #expect(tour.target == "formation-daily-open")
        tour.back()
        #expect(tour.target == "formation-daily")
        #expect(tour.animatesStep)
        tour.stop()
        #expect(!tour.active)
    }

    @Test @MainActor func dashboardAndNavigationAreCoveredInOrder() {
        let tour=InstructorWalkthrough()
        tour.start()
        let expected=["welcome","schedule","tracker","announcements","guides","assignments","prayers",
                      "nav-home","nav-lessons","nav-discussion","nav-formation","nav-more","lessons","category","lesson-title"]
        for target in expected {
            #expect(tour.target==target)
            tour.next()
        }
        #expect(tour.target=="lesson-title")
        #expect(!tour.canAdvance)
        tour.stop()
        #expect(!tour.active)
    }

    @Test @MainActor func actualLessonHeadingsControlTheRouteAndCopy() {
        let tour=InstructorWalkthrough()
        tour.start()
        tour.prepareLesson("test-lesson")
        tour.go("lesson-title")
        tour.setSections([
            .init(id:"lesson-part-0",title:"Definition",top:0,height:30),
            .init(id:"lesson-part-1",title:"Referencias bíblicas",top:400,height:30),
            .init(id:"lesson-part-2",title:"Proclamation",top:900,height:30)
        ],lesson:"test-lesson")
        #expect(tour.canAdvance)
        tour.next()
        #expect(tour.target=="lesson-part-0")
        #expect(tour.animatesStep)
        tour.next()
        #expect(tour.copy(true).1.contains("Biblia"))
        tour.next()
        #expect(tour.target=="lesson-part-2")
        tour.next()
        #expect(tour.target=="lesson-actions")
        tour.next()
        #expect(tour.page=="discussion")
        #expect(!tour.animatesStep)
    }

    @Test @MainActor func emptySectionsAndLateMeasurementsDoNotTrapTheTour() {
        let tour=InstructorWalkthrough()
        tour.start();tour.prepareLesson("current");tour.go("lesson-title")
        tour.setSections([],lesson:"old")
        #expect(!tour.canAdvance)
        tour.setSections([],lesson:"current")
        #expect(tour.canAdvance)
        tour.next()
        #expect(tour.target=="lesson-actions")
        tour.back()
        #expect(tour.target=="lesson-title")
        tour.start()
        #expect(tour.target=="welcome")
        #expect(tour.lessonSections.isEmpty)
    }
}
