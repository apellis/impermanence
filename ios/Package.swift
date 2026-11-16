// swift-tools-version:5.9
// Builds the platform-independent bell-timing core and its tests without
// Xcode, so they run on Linux (cloud sessions) as well as macOS.
// The iOS app itself is built from Impermanence/Impermanence.xcodeproj.
//
//   docker run --rm -v "$PWD":/src -w /src/ios swift:6.0-noble swift test
import PackageDescription

let package = Package(
    name: "ImpermanenceCore",
    targets: [
        .target(
            name: "ImpermanenceCore",
            path: "Impermanence/Impermanence/Models",
            exclude: ["DayTimer.swift", "BellPlayer.swift", "DayStore.swift"],
            sources: ["Bell.swift", "BellCatalog.swift", "Day.swift", "Theme.swift", "BellSchedule.swift"]
        ),
        .testTarget(
            name: "CoreTests",
            dependencies: ["ImpermanenceCore"],
            path: "CoreTests"
        ),
    ]
)
