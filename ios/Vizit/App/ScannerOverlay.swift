import SwiftUI
import PhotosUI
import CoreImage

/// The viewfinder chrome: a dimmed surround, a cyan-bracketed aperture and a
/// sweeping line, so it is obvious where the code has to go and that the
/// camera is live. Everything sits above the preview layer, never on it.
struct ScannerOverlay<Actions: View>: View {
    var isSweeping = true
    @ViewBuilder var actions: () -> Actions

    private static var apertureSide: CGFloat { 232 }
    private static var bracket: CGFloat { 34 }

    @State private var sweep: CGFloat = 0

    private var accent: Color { Color(uiColor: UIColor(hex: 0x0FBEE6)) }

    var body: some View {
        GeometryReader { geometry in
            let side = min(Self.apertureSide, min(geometry.size.width, geometry.size.height) * 0.62)
            ZStack {
                // The surround is dimmed; the aperture itself stays untouched.
                Color.black.opacity(0.55)
                    .mask {
                        Rectangle()
                            .overlay {
                                RoundedRectangle(cornerRadius: VizitRadius.md, style: .continuous)
                                    .frame(width: side, height: side)
                                    .blendMode(.destinationOut)
                            }
                            .compositingGroup()
                    }
                    .ignoresSafeArea()

                ZStack {
                    ForEach(0..<4, id: \.self) { corner in
                        Bracket(length: Self.bracket)
                            .stroke(accent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                            .frame(width: side, height: side)
                            .rotationEffect(.degrees(Double(corner) * 90))
                    }

                    if isSweeping {
                        Rectangle()
                            .fill(
                                LinearGradient(
                                    colors: [accent.opacity(0), accent, accent.opacity(0)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: side - 8, height: 2)
                            .offset(y: sweep)
                    }
                }
                .frame(width: side, height: side)
                .accessibilityHidden(true)
                .onAppear {
                    guard isSweeping else { return }
                    sweep = -side / 2 + 6
                    withAnimation(.easeInOut(duration: 1.9).repeatForever(autoreverses: true)) {
                        sweep = side / 2 - 6
                    }
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
        .overlay(alignment: .bottom) { instructions }
    }

    private var instructions: some View {
        VStack(spacing: VizitSpace.sm) {
            Text("Irányítsd a kamerát a másik kódra")
                .font(VizitFont.h3)
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
            Text("A beolvasott névjegy a kapcsolataid közé kerül, és offline is elérhető marad. Képet nem készítünk és nem tárolunk.")
                .font(VizitFont.bodySmall)
                .foregroundStyle(.white.opacity(0.74))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            actions()
        }
        .padding(.horizontal, VizitSpace.lg)
        .padding(.top, VizitSpace.lg)
        .padding(.bottom, VizitSpace.xl)
        .frame(maxWidth: .infinity)
        .background(Color.black.opacity(0.82))
    }
}

/// One corner bracket, drawn at the top-leading corner and rotated into the
/// other three — four separate paths would drift apart the first time the
/// aperture is resized.
private struct Bracket: Shape {
    let length: CGFloat

    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY + length))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX + length, y: rect.minY))
        return path
    }
}

/// Reads a QR out of a still image the owner picked, for codes that arrive as
/// a screenshot rather than on someone else's screen.
enum QRImageDecoder {
    static func decode(_ data: Data) -> String? {
        guard data.count <= 24 * 1024 * 1024, let image = CIImage(data: data) else { return nil }
        let detector = CIDetector(
            ofType: CIDetectorTypeQRCode,
            context: CIContext(options: [.useSoftwareRenderer: true]),
            options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]
        )
        let features = detector?.features(in: image).compactMap { $0 as? CIQRCodeFeature } ?? []
        // Two codes in one picture give no way to know which was meant.
        guard features.count == 1 else { return nil }
        return features[0].messageString
    }
}
