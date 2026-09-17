// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "VizitCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "VizitCore", targets: ["VizitCore"])],
    targets: [
        .target(name: "VizitCore", path: "Vizit/Core"),
        .testTarget(name: "VizitCoreTests", dependencies: ["VizitCore"], path: "Tests/VizitCoreTests")
    ]
)
