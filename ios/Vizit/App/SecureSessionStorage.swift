import Foundation
import Security
import Supabase

/// Supabase session bytes are available only while the device is unlocked and
/// never migrate to another device through a backup.
struct SecureSessionStorage: AuthLocalStorage, Sendable {
    let service: String

    func store(key: String, value: Data) throws {
        let base = query(key)
        let attributes: [CFString: Any] = [
            kSecValueData: value,
            kSecAttrAccessible: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        let status = SecItemUpdate(base as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var item = base
            for (key, value) in attributes { item[key] = value }
            try check(SecItemAdd(item as CFDictionary, nil))
        } else {
            try check(status)
        }
    }

    func retrieve(key: String) throws -> Data? {
        var item = query(key)
        item[kSecReturnData] = true
        item[kSecMatchLimit] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(item as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        try check(status)
        guard let data = result as? Data else { throw KeychainError.unexpectedData }
        return data
    }

    func remove(key: String) throws {
        let status = SecItemDelete(query(key) as CFDictionary)
        if status != errSecItemNotFound { try check(status) }
    }

    private func query(_ key: String) -> [CFString: Any] {
        [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: service,
            kSecAttrAccount: key,
            kSecAttrSynchronizable: false
        ]
    }

    private func check(_ status: OSStatus) throws {
        guard status == errSecSuccess else { throw KeychainError.status(status) }
    }
}

enum KeychainError: LocalizedError {
    case status(OSStatus)
    case unexpectedData

    var errorDescription: String? {
        switch self {
        case .status(let code): return "A bejelentkezés nem menthető el (\(code))."
        case .unexpectedData: return "A mentett bejelentkezés sérült. Jelentkezz be újra."
        }
    }
}
