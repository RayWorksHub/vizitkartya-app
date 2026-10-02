import SwiftUI

struct ZWIntro: View {
    @ObservedObject var w: ZWState
    let canClose: Bool
    let onClose: () -> Void
    let onPick: (ZWProfileType) -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            GeometryReader { geo in ZWIntroBackground(size: geo.size) }.ignoresSafeArea()
            ZWWaves().frame(width: 260, height: 284).offset(x: 40, y: 70).frame(maxWidth: .infinity, alignment: .topTrailing).allowsHitTesting(false)
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    if canClose {
                        Button(action: onClose) {
                            Image(systemName: "xmark").font(.system(size: 17, weight: .semibold)).foregroundStyle(.white)
                                .frame(width: 44, height: 44).background(Circle().fill(Color.white.opacity(0.10)))
                        }.buttonStyle(.plain)
                    } else { Color.clear.frame(width: 44, height: 44) }
                    Text("VIZIT").font(.system(size: 13, weight: .heavy)).tracking(3.9).foregroundStyle(Color.white.opacity(0.65)).frame(maxWidth: .infinity)
                    Color.clear.frame(width: 44, height: 44)
                }.padding(.horizontal, 10).padding(.top, 8)
                GeometryReader { g in
                    ScrollView(.vertical, showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Kezdjük meg a profilod létrehozását!").font(.system(size: 34, weight: .medium)).tracking(-0.34).foregroundStyle(.white)
                            Text("Milyen profil lesz?").font(.system(size: 14, weight: .medium)).foregroundStyle(Color.white.opacity(0.70)).padding(.top, 6).padding(.bottom, 2)
                            VStack(spacing: 10) {
                                ZWTypePick(checked: w.profileType == .business, type: .business, icon: "briefcase", label: "Vállalkozói", onTap: onPick)
                                    .accessibilityIdentifier("wizard.business")
                                ZWTypePick(checked: w.profileType == .individual, type: .individual, icon: "person", label: "Magánszemély", onTap: onPick)
                                    .accessibilityIdentifier("wizard.private")
                            }
                        }
                        .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 28)
                        .frame(maxWidth: .infinity, minHeight: g.size.height, alignment: .bottomLeading)
                    }
                }
            }
        }
    }
}

struct ZWTypePick: View {
    let checked: Bool
    let type: ZWProfileType
    let icon: String
    let label: String
    let onTap: (ZWProfileType) -> Void
    private var bars: [Color] { ZW_BLOCKS.filter { type != .individual || $0.id != "company" }.map(.color.color) }
    var body: some View {
        Button { onTap(type) } label: {
            HStack(spacing: 12) {
                Image(systemName: icon).font(.system(size: 19, weight: .semibold)).foregroundStyle(.white)
                    .frame(width: 44, height: 44).background(Circle().fill(checked ? ZW.h2 : Color.white.opacity(0.15)))
                VStack(alignment: .leading, spacing: 9) {
                    Text(label).font(.system(size: 17, weight: .medium)).foregroundStyle(checked ? ZW.ink : .white)
                    HStack(spacing: 4) { ForEach(Array(bars.enumerated()), id: .offset) { _, color in RoundedRectangle(cornerRadius: 3).fill(color).frame(width: 22, height: 5) } }
                }.frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .bold)).foregroundStyle(.white)
                    .frame(width: 30, height: 30).background(Circle().fill(checked ? ZW.h2 : Color.white.opacity(0.15)))
            }
            .padding(.leading, 14).padding(.trailing, 12).padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 20).fill(checked ? ZW.surface : Color.white.opacity(0.10)))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(checked ? ZW.surface : Color.white.opacity(0.22), lineWidth: 1))
        }.buttonStyle(.plain)
    }
}

struct ZWIntroBackground: View {
    let size: CGSize
    var body: some View {
        ZStack {
            ZW.h1
            LinearGradient(stops: [.init(color: ZW.h2.opacity(0), location: 0.45), .init(color: ZW.h2.opacity(0.65), location: 1)],
                           startPoint: UnitPoint(x: 0.37, y: 0), endPoint: UnitPoint(x: 0.63, y: 1))
            RadialGradient(colors: [ZW.glow2, .clear], center: .bottomLeading, startRadius: 0, endRadius: size.width * 0.9)
            RadialGradient(colors: [ZW.glow1, .clear], center: .topTrailing, startRadius: 0, endRadius: size.width * 1.2)
        }.frame(width: size.width, height: size.height).clipped()
    }
}

struct ZWWaves: View {
    var body: some View {
        ZStack {
            Circle().fill(Color.white.opacity(0.15)).frame(width: 26, height: 26).offset(x: -70)
            ForEach([82.0, 124.0, 166.0], id: .self) { diameter in
                Circle().trim(from: 0.34, to: 0.66).stroke(Color.white.opacity(0.15), style: StrokeStyle(lineWidth: 14.2, lineCap: .round))
                    .frame(width: diameter * 2, height: diameter * 2).offset(x: diameter * 0.28)
            }
        }
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        return stride(from: 0, to: count, by: size).map { Array(self[$0..<Swift.min($0 + size, count)]) }
    }
}
