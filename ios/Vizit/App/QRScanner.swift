import SwiftUI
import AVFoundation

struct QRScanner: UIViewControllerRepresentable {
    let onResult: (String) -> Void
    let onError: (String) -> Void
    /// Driven by the overlay's torch button; the controller owns the hardware.
    var torchOn = false
    /// Reported back once the camera is configured, so the overlay only offers
    /// a torch button on a device that actually has one.
    var onTorchAvailability: ((Bool) -> Void)?

    func makeUIViewController(context: Context) -> ScannerController {
        ScannerController(onResult: onResult, onError: onError, onTorchAvailability: onTorchAvailability)
    }
    func updateUIViewController(_ controller: ScannerController, context: Context) {
        controller.setTorch(torchOn)
    }
    static func dismantleUIViewController(_ controller: ScannerController, coordinator: ()) { controller.stop() }
}

final class ScannerController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "hu.rayworks.vizit.ios.camera")
    private let onResult: (String) -> Void
    private let onError: (String) -> Void
    private let onTorchAvailability: ((Bool) -> Void)?
    private var preview: AVCaptureVideoPreviewLayer?
    private var camera: AVCaptureDevice?
    private var finished = false
    private var visible = false
    // Accessed exclusively on sessionQueue.
    private var stopped = false
    private var torchRequested = false

    init(onResult: @escaping (String) -> Void,
         onError: @escaping (String) -> Void,
         onTorchAvailability: ((Bool) -> Void)? = nil) {
        self.onResult = onResult
        self.onError = onError
        self.onTorchAvailability = onTorchAvailability
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("Use init(onResult:onError:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(layer)
        preview = layer
        NotificationCenter.default.addObserver(self, selector: #selector(sessionInterrupted),
            name: .AVCaptureSessionWasInterrupted, object: session)
        NotificationCenter.default.addObserver(self, selector: #selector(sessionInterrupted),
            name: .AVCaptureSessionRuntimeError, object: session)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        visible = true
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: configure()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] allowed in
                DispatchQueue.main.async {
                    guard let self, self.visible else { return }
                    if allowed { self.configure() }
                    else { self.fail("A beolvasáshoz kameraengedély szükséges. Ezt a rendszerbeállításokban engedélyezheted.") }
                }
            }
        default:
            fail("A kamerához nincs hozzáférés. A rendszerbeállításokban engedélyezd a kamerát a VIZIT számára.")
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        visible = false
        stop()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        preview?.frame = view.bounds
        if let orientation = view.window?.windowScene?.interfaceOrientation,
           let connection = preview?.connection, connection.isVideoOrientationSupported {
            switch orientation {
            case .landscapeLeft: connection.videoOrientation = .landscapeLeft
            case .landscapeRight: connection.videoOrientation = .landscapeRight
            case .portraitUpsideDown: connection.videoOrientation = .portraitUpsideDown
            default: connection.videoOrientation = .portrait
            }
        }
    }

    private func configure() {
        sessionQueue.async { [weak self] in
            guard let self, !self.stopped else { return }
            self.session.beginConfiguration()
            guard self.session.inputs.isEmpty,
                  let camera = AVCaptureDevice.default(for: .video),
                  let input = try? AVCaptureDeviceInput(device: camera), self.session.canAddInput(input) else {
                self.session.commitConfiguration()
                self.fail("A kamera nem érhető el. Szimulátorban a kamerás beolvasás nem tesztelhető.")
                return
            }
            self.session.addInput(input)
            self.camera = camera
            let hasTorch = camera.hasTorch && camera.isTorchAvailable
            DispatchQueue.main.async { self.onTorchAvailability?(hasTorch) }
            let output = AVCaptureMetadataOutput()
            guard self.session.canAddOutput(output) else {
                self.session.commitConfiguration()
                self.fail("A QR-beolvasó nem indítható el.")
                return
            }
            self.session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            guard output.availableMetadataObjectTypes.contains(.qr) else {
                self.session.commitConfiguration()
                self.fail("Ez a kamera nem támogatja a QR-kódok felismerését.")
                return
            }
            output.metadataObjectTypes = [.qr]
            self.session.commitConfiguration()
            self.session.startRunning()
            self.applyTorch()
        }
    }

    func setTorch(_ on: Bool) {
        sessionQueue.async { [weak self] in
            guard let self, self.torchRequested != on else { return }
            self.torchRequested = on
            self.applyTorch()
        }
    }

    /// The torch is a camera-device setting, so it has to be locked, changed
    /// and unlocked off the main thread like any other capture configuration.
    private func applyTorch() {
        guard let camera, camera.hasTorch, camera.isTorchAvailable else { return }
        let wanted = torchRequested && !stopped
        guard camera.torchMode != (wanted ? .on : .off) else { return }
        guard (try? camera.lockForConfiguration()) != nil else { return }
        camera.torchMode = wanted ? .on : .off
        camera.unlockForConfiguration()
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.stopped = true
            self.applyTorch()
            if self.session.isRunning { self.session.stopRunning() }
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject],
                        from connection: AVCaptureConnection) {
        guard visible, !finished,
              let value = metadataObjects.compactMap({ ($0 as? AVMetadataMachineReadableCodeObject)?.stringValue }).first else { return }
        finished = true
        stop()
        onResult(value)
    }

    private func fail(_ message: String) {
        DispatchQueue.main.async { [weak self] in
            guard let self, self.visible, !self.finished else { return }
            self.finished = true
            self.stop()
            self.onError(message)
        }
    }

    @objc private func sessionInterrupted(_ notification: Notification) {
        fail("A kamerás beolvasás megszakadt. Zárd be, majd indítsd újra a beolvasót.")
    }
    deinit { NotificationCenter.default.removeObserver(self) }
}
