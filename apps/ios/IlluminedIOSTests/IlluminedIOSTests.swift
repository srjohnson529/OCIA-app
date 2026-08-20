//
//  IlluminedIOSTests.swift
//  IlluminedIOSTests
//
//  Created by Stephen Johnson on 7/9/26.
//

import Foundation
import Testing
@testable import IlluminedIOS

struct IlluminedIOSTests {

    @Test func olderProfilesDefaultToNoSelectedPrayers() throws {
        let data = Data(#"{"userId":"user-1"}"#.utf8)
        let profile = try JSONDecoder().decode(UserProfile.self, from: data)

        #expect(profile.selectedPrayerIds.isEmpty)
    }

    @Test func notificationPreferencesAreBackwardCompatibleAndDecodeSavedChoices() throws {
        let oldData = Data(#"{"userId":"user-1"}"#.utf8)
        let savedData = Data(#"{"userId":"user-1","notificationNewPrayerRequests":false,"notificationNewAssignments":false,"notificationAssignmentReminders":false,"notificationDiscussionReplies":false}"#.utf8)
        let oldProfile = try JSONDecoder().decode(UserProfile.self, from: oldData)
        let savedProfile = try JSONDecoder().decode(UserProfile.self, from: savedData)

        #expect(oldProfile.notificationNewPrayerRequests)
        #expect(oldProfile.notificationNewAssignments)
        #expect(oldProfile.notificationAssignmentReminders)
        #expect(oldProfile.notificationDiscussionReplies)
        #expect(oldProfile.notificationsEnabled)
        #expect(!savedProfile.notificationNewPrayerRequests)
        #expect(!savedProfile.notificationNewAssignments)
        #expect(!savedProfile.notificationAssignmentReminders)
        #expect(!savedProfile.notificationDiscussionReplies)
    }

    @Test func partiallyCreatedProfilesDecodeWithoutAnEmbeddedUserId() throws {
        let data = Data(#"{"displayName":"New Student","classIds":["ocia"]}"#.utf8)
        let profile = try JSONDecoder().decode(UserProfile.self, from: data)

        #expect(profile.userId.isEmpty)
        #expect(profile.displayName == "New Student")
    }

    @Test func selectedPrayersDecodeFromTheUserProfile() throws {
        let data = Data(#"{"userId":"user-1","selectedPrayerIds":["our-father","hail-mary"]}"#.utf8)
        let profile = try JSONDecoder().decode(UserProfile.self, from: data)

        #expect(profile.selectedPrayerIds == ["our-father", "hail-mary"])
    }

    @Test func activeClassIsSelectedWhenItBelongsToInstructor() throws {
        let data = Data(#"{"userId":"user-1","classIds":["ocia","mens-ministry"],"activeClassId":"mens-ministry"}"#.utf8)
        let profile = try JSONDecoder().decode(UserProfile.self, from: data)

        #expect(profile.primaryClassId == "mens-ministry")
    }

    @Test func olderAndInvalidActiveClassValuesUseFirstMembership() throws {
        let oldData = Data(#"{"userId":"user-1","classIds":["ocia","mens-ministry"]}"#.utf8)
        let invalidData = Data(#"{"userId":"user-1","classIds":["ocia","mens-ministry"],"activeClassId":"other"}"#.utf8)

        #expect(try JSONDecoder().decode(UserProfile.self, from: oldData).primaryClassId == "ocia")
        #expect(try JSONDecoder().decode(UserProfile.self, from: invalidData).primaryClassId == "ocia")
    }

    @Test func inviteLinksRoundTripAndRejectMissingPrivilegedCodes() throws {
        let instructor = IlluminedInviteLink(role: .instructor, classId: "OCIA", code: "ABCD-2345")
        #expect(IlluminedInviteLink.parse(instructor.url) == instructor)
        #expect(IlluminedInviteLink.parse(URL(string: "illumined://join?role=student&classId=OCIA")!)?.role == .student)
        #expect(IlluminedInviteLink.parse(URL(string: "https://illumined.net/join?role=instructor&classId=OCIA&code=ABCD-2345")!) == instructor)
        #expect(IlluminedInviteLink.parse(URL(string: "illumined://join?role=instructor&classId=OCIA")!) == nil)
        #expect(IlluminedInviteLink.parse(URL(string: "illumined://join?role=parish")!) == nil)
        #expect(IlluminedInviteLink.parse(URL(string: "https://example.com/join?role=student&classId=OCIA")!) == nil)
    }

}
