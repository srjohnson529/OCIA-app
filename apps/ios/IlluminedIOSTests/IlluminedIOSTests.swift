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

    @Test func selectedPrayersDecodeFromTheUserProfile() throws {
        let data = Data(#"{"userId":"user-1","selectedPrayerIds":["our-father","hail-mary"]}"#.utf8)
        let profile = try JSONDecoder().decode(UserProfile.self, from: data)

        #expect(profile.selectedPrayerIds == ["our-father", "hail-mary"])
    }

}
