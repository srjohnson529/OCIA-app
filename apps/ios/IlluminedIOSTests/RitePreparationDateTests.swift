import XCTest
@testable import IlluminedIOS

final class RitePreparationDateTests: XCTestCase {
    func testExpiryUsesParishMidnightAcrossDST() throws {
        let formatter = ISO8601DateFormatter()
        XCTAssertEqual(try RitePreparationDate.expiry("2026-03-08", zone: "America/New_York"), formatter.date(from: "2026-03-09T04:00:00Z"))
        XCTAssertEqual(try RitePreparationDate.expiry("2026-11-01", zone: "America/New_York"), formatter.date(from: "2026-11-02T05:00:00Z"))
        XCTAssertEqual(try RitePreparationDate.expiry("2026-12-31", zone: "UTC"), formatter.date(from: "2027-01-01T00:00:00Z"))
    }
    func testInvalidInputIsRejected() {
        XCTAssertThrowsError(try RitePreparationDate.expiry("2026-02-30", zone: "UTC"))
        XCTAssertThrowsError(try RitePreparationDate.expiry("2026-09-08", zone: "invalid-zone"))
    }
}
