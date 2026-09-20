import SwiftUI
import Contacts
import ContactsUI

struct ShareFile: Identifiable { let id = UUID(); let url: URL }
struct IncomingContact: Identifiable { let id = UUID(); let contact: CNContact }

enum ContactBridge {
    static func contact(_ profile: ContactProfile) -> CNContact {
        let p = profile.normalized
        let contact = CNMutableContact()
        contact.givenName = p.firstName
        contact.familyName = p.lastName
        if p.firstName.isEmpty && p.lastName.isEmpty { contact.givenName = p.displayName }
        else if !p.fullName.isEmpty { contact.nickname = p.fullName }
        contact.organizationName = p.company
        contact.jobTitle = p.jobTitle
        if !p.phone.isEmpty {
            contact.phoneNumbers = [CNLabeledValue(label: CNLabelPhoneNumberMobile, value: CNPhoneNumber(stringValue: p.phone))]
        }
        if !p.email.isEmpty { contact.emailAddresses = [CNLabeledValue(label: CNLabelWork, value: p.email as NSString)] }
        var urls: [CNLabeledValue<NSString>] = []
        if !p.website.isEmpty {
            urls.append(CNLabeledValue(label: CNLabelWork, value: p.website as NSString))
        }
        urls += p.socialProfiles.compactMap { item in
            guard !item.url.isEmpty else { return nil }
            return CNLabeledValue(label: item.platform.label, value: item.url as NSString)
        }
        contact.urlAddresses = urls
        if !p.address.isEmpty {
            let address = CNMutablePostalAddress()
            address.street = p.address
            contact.postalAddresses = [CNLabeledValue(label: CNLabelWork, value: address)]
        }
        if let data = Data(base64Encoded: p.photoBase64), !data.isEmpty { contact.imageData = data }
        return contact
    }

    static func shareFile(_ profile: ContactProfile) throws -> ShareFile {
        guard !profile.normalized.photoBase64.isEmpty else { throw ProfileError.missingPhoto }
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("VIZIT-share-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let url = directory.appendingPathComponent("nevjegy.vcf")
        do {
            let data = Data(try VCard.encode(profile, includePhoto: true).utf8)
            try data.write(to: url, options: [.atomic, .completeFileProtection])
            return ShareFile(url: url)
        } catch {
            try? FileManager.default.removeItem(at: directory)
            throw error
        }
    }

    static func removeShareFile(_ url: URL) {
        let directory = url.deletingLastPathComponent()
        // Never remove anything outside the application's own temporary export.
        guard directory.deletingLastPathComponent().standardizedFileURL == FileManager.default.temporaryDirectory.standardizedFileURL,
              directory.lastPathComponent.hasPrefix("VIZIT-share-") else { return }
        try? FileManager.default.removeItem(at: directory)
    }
}

struct ActivitySheet: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

/// The operating system presents the final save action. We do not request
/// permission to enumerate the user's address book or silently create contacts.
struct ContactEditor: UIViewControllerRepresentable {
    let contact: CNContact
    let onComplete: (Bool) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onComplete: onComplete) }
    func makeUIViewController(context: Context) -> UINavigationController {
        let editor = CNContactViewController(forNewContact: contact)
        editor.delegate = context.coordinator
        editor.allowsActions = false
        return UINavigationController(rootViewController: editor)
    }
    func updateUIViewController(_ controller: UINavigationController, context: Context) {}

    final class Coordinator: NSObject, CNContactViewControllerDelegate {
        let onComplete: (Bool) -> Void
        init(onComplete: @escaping (Bool) -> Void) { self.onComplete = onComplete }
        func contactViewController(_ viewController: CNContactViewController, didCompleteWith contact: CNContact?) {
            onComplete(contact != nil)
        }
        func contactViewController(_ viewController: CNContactViewController,
                                   shouldPerformDefaultActionFor property: CNContactProperty) -> Bool { false }
    }
}
