import SwiftUI
import PhotosUI
import ImageIO

struct ZipProfileWizard: View {
    var isAdditional = false
    var onSaving: () -> Void = {}
    var onSaveFailed: () -> Void = {}
    var onFinished: () -> Void = {}
    var onCancel: (() -> Void)? = nil

    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var presentation: CardPresentationStore
    @StateObject private var w = ZWState()
    @FocusState private var focus: String?
    @State private var page = 0
    @State private var dragOffset: CGFloat = 0
    @State private var photoItem: PhotosPickerItem?
    @State private var logoItem: PhotosPickerItem?
    @State private var imageError: String?
    @State private var published = false

    private var keyboardVisible: Bool { focus != nil }

    var body: some View {
        ZStack {
            main
            if w.introShown {
                ZWIntro(w: w, canClose: isAdditional || onCancel != nil, onClose: close, onPick: pickType)
                    .transition(.move(edge: .leading))
                    .zIndex(2)
            }
        }
        .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.38), value: w.introShown)
        .statusBarHidden(w.introShown)
        .onAppear {
            w.takenSlugs = store.businessCards.map(.profile.publicSlug).filter { !$0.isEmpty }
        }
        .onChange(of: photoItem) { item in process(item, isLogo: false) }
        .onChange(of: logoItem) { item in process(item, isLogo: true) }
        .alert("Kilépsz? Az adatok elvesznek.", isPresented: $w.confirmOpen) {
            Button("Kilépés", role: .destructive) {
                w.confirmOpen = false
                onCancel?()
            }
            Button("Maradok", role: .cancel) { w.confirmOpen = false }
        }
    }

    private var main: some View {
        VStack(spacing: 0) {
            if published {
                publishedView
            } else {
                topBar
                progress
                GeometryReader { geo in pager(size: geo.size) }
            }
        }
        .background(ZW.bg.ignoresSafeArea())
    }

    private var currentBlock: ZWBlock? { w.flow.indices.contains(page) ? w.flow[page] : w.flow.first }

    private var topBar: some View {
        HStack(spacing: 0) {
            Group {
                if isAdditional || onCancel != nil {
                    Button(action: close) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(ZW.sub)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                } else {
                    Color.clear.frame(width: 44, height: 44)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 1) {
                if !keyboardVisible {
                    Button { w.introShown = true } label: {
                        HStack(spacing: 3) {
                            Text((w.profileType == .individual ? "Magánszemély" : "Vállalkozói") + " · (page + 1)/(w.flow.count)")
                                .font(.system(size: 11.5, weight: .semibold))
                                .foregroundStyle(ZW.sub)
                            Image(systemName: "chevron.down")
                                .font(.system(size: 9, weight: .bold)).foregroundStyle(ZW.sub)
                        }
                        .padding(.horizontal, 6).padding(.vertical, 2)
                    }.buttonStyle(.plain)
                }
                if let block = currentBlock {
                    HStack(spacing: 7) {
                        Circle().fill(block.color.color).frame(width: 9, height: 9)
                        Text(block.title).font(.system(size: 16, weight: .medium)).foregroundStyle(ZW.ink).lineLimit(1)
                    }
                }
            }
            .layoutPriority(1)

            if keyboardVisible, let block = w.activeBlock {
                Button(w.actionLabel(block)) { perform(block) }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(w.isSkip(block) ? ZW.blue : Color.white)
                    .padding(.horizontal, 14).frame(height: 36)
                    .background(w.isSkip(block) ? ZW.blueSoft : ZW.blueFill)
                    .clipShape(Capsule())
                    .disabled(!w.valid(block.id))
                    .frame(maxWidth: .infinity, alignment: .trailing)
            } else {
                Color.clear.frame(width: 44, height: 44).frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
        .padding(.horizontal, 8).padding(.top, 2).padding(.bottom, keyboardVisible ? 2 : 6)
    }

    private var progress: some View {
        HStack(spacing: 4) {
            ForEach(Array(w.flow.enumerated()), id: .element.id) { item in
                let index = item.offset
                let block = item.element
                let state = w.status(block.id)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: index == page ? 3 : 1)
                            .fill(progressBase(state, block))
                        if state == .active {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(block.color.color)
                                .frame(width: geo.size.width * 0.45)
                        }
                    }
                    .frame(height: index == page ? 8 : 4)
                    .frame(maxHeight: .infinity, alignment: .center)
                    .contentShape(Rectangle())
                    .onTapGesture { go(index) }
                }
                .frame(height: 14)
            }
        }
        .padding(.horizontal, 16).padding(.bottom, keyboardVisible ? 6 : 10)
    }

    private func progressBase(_ status: ZWBlockStatus, _ block: ZWBlock) -> Color {
        switch status {
        case .done, .open: return block.color.color
        case .skip: return ZW.chev
        default: return ZW.switchOff
        }
    }

    private func pager(size: CGSize) -> some View {
        let side: CGFloat = 28
        let spacing: CGFloat = 12
        let width = max(size.width - side * 2, 120)
        let stride = width + spacing
        return HStack(spacing: spacing) {
            ForEach(Array(w.flow.enumerated()), id: .element.id) { item in
                ZWBlockPage(
                    w: w,
                    block: item.element,
                    index: item.offset,
                    keyboardVisible: keyboardVisible,
                    focus: $focus,
                    photoItem: $photoItem,
                    logoItem: $logoItem,
                    imageError: imageError,
                    onAction: { perform(item.element) },
                    onReopen: { w.reopen(item.element.id) }
                )
                .frame(width: width, height: max(size.height - (keyboardVisible ? 14 : 24), 120))
            }
        }
        .padding(.horizontal, side)
        .offset(x: -CGFloat(page) * stride + dragOffset)
        .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.32), value: page)
        .gesture(
            DragGesture(minimumDistance: 12)
                .onChanged { dragOffset = $0.translation.width }
                .onEnded { value in
                    if value.translation.width < -60 { go(min(page + 1, w.flow.count - 1)) }
                    else if value.translation.width > 60 { go(max(page - 1, 0)) }
                    dragOffset = 0
                }
        )
        .clipped()
    }

    private func go(_ index: Int) {
        focus = nil
        page = min(max(index, 0), max(w.flow.count - 1, 0))
    }

    private func perform(_ block: ZWBlock) {
        guard w.valid(block.id) else { return }
        if block.id == "done" { publish() }
        else {
            w.next(block.id)
            if let next = w.activeBlock, let idx = w.flow.firstIndex(where: { $0.id == next.id }) { go(idx) }
        }
    }

    private func pickType(_ type: ZWProfileType) {
        w.chooseType(type)
        w.begin()
        page = max(w.flow.firstIndex(where: { $0.id == "personal" }) ?? 0, 0)
    }

    private func close() {
        focus = nil
        if w.dirty { w.confirmOpen = true }
        else { onCancel?() }
    }

    private func publish() {
        guard let profile = w.profile() else { return }
        focus = nil
        onSaving()
        do {
            var cardPresentation = presentation.value
            cardPresentation.colorway = w.colorway
            if isAdditional {
                try store.createBusinessCard(profile, presentation: cardPresentation)
            } else {
                try store.save(profile)
                store.updateActiveCardPresentation(cardPresentation)
            }
            presentation.value = cardPresentation
            published = true
        } catch {
            onSaveFailed()
            w.errors["publish"] = error.localizedDescription
        }
    }

    private var publishedView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "checkmark.circle.fill").font(.system(size: 54)).foregroundStyle(ZW.green)
            Text("Publikálva").font(.system(size: 26, weight: .bold)).foregroundStyle(ZW.ink)
            Text(w.isPublic ? "A névjegyed él. Oszd meg a címét, vagy mutasd a QR-kódot." : "A névjegyed elkészült.")
                .font(.system(size: 15)).foregroundStyle(ZW.sub).multilineTextAlignment(.center)
            Button("Tovább az áttekintéshez", action: onFinished)
                .font(.system(size: 15, weight: .semibold)).foregroundStyle(.white)
                .frame(maxWidth: 420).frame(height: 52).background(ZW.blueFill).clipShape(RoundedRectangle(cornerRadius: 16))
                .accessibilityIdentifier("wizard.finish")
            Spacer()
        }
        .padding(20)
    }

    private func process(_ item: PhotosPickerItem?, isLogo: Bool) {
        guard let item else { return }
        Task {
            do {
                guard let bytes = try await item.loadTransferable(type: Data.self), bytes.count <= 25 * 1024 * 1024,
                      let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                      let thumb = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                        kCGImageSourceCreateThumbnailFromImageAlways: true,
                        kCGImageSourceCreateThumbnailWithTransform: true,
                        kCGImageSourceThumbnailMaxPixelSize: isLogo ? 320 : 640
                      ] as CFDictionary),
                      let jpeg = UIImage(cgImage: thumb).jpegData(compressionQuality: 0.82),
                      jpeg.count <= 256 * 1024 else { throw ProfileError.invalidPhoto }
                await MainActor.run {
                    if isLogo { w.logoBase64 = jpeg.base64EncodedString(); w.errors["logo"] = nil }
                    else { w.photoBase64 = jpeg.base64EncodedString(); w.errors["photo"] = nil }
                    w.dirty = true
                }
            } catch {
                await MainActor.run { w.errors[isLogo ? "logo" : "photo"] = "A kép betöltése nem sikerült." }
            }
        }
    }
}
