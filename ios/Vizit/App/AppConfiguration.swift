import Foundation

struct AppConfiguration: Sendable {
    let supabaseURL: URL
    let publishableKey: String
    let callbackURL: URL
    let googleSignInEnabled: Bool
    let privacyPolicyURL: URL
    let privacyPolicyVersion: String
    let termsURL: URL
    let termsVersion: String
    let publicProfileBaseURL: URL

    static func load(bundle: Bundle = .main) throws -> AppConfiguration {
        func value(_ key: String) throws -> String {
            guard let value = bundle.object(forInfoDictionaryKey: key) as? String else {
                throw ConfigurationError.missing(key)
            }
            let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !clean.isEmpty, !clean.contains("$(") else { throw ConfigurationError.missing(key) }
            return clean
        }
        func httpsURL(_ key: String) throws -> URL {
            let text = try value(key)
            guard let url = URL(string: text), url.scheme?.lowercased() == "https",
                  url.host?.isEmpty == false, url.user == nil, url.password == nil else {
                throw ConfigurationError.invalid(key)
            }
            return url
        }

        let supabaseURL = try httpsURL("VIZITSupabaseURL")
        guard supabaseURL.path.isEmpty || supabaseURL.path == "/" else {
            throw ConfigurationError.invalid("VIZITSupabaseURL")
        }
        let key = try value("VIZITSupabaseKey")
        guard key.count >= 20, !key.unicodeScalars.contains(where: CharacterSet.whitespacesAndNewlines.contains)
        else { throw ConfigurationError.invalid("VIZITSupabaseKey") }
        let scheme = try value("VIZITAuthScheme").lowercased()
        guard scheme.range(of: #"^[a-z][a-z0-9+.-]{2,31}$"#, options: .regularExpression) != nil,
              let callback = URL(string: "\(scheme)://auth-callback") else {
            throw ConfigurationError.invalid("VIZITAuthScheme")
        }
        let googleSetting = try value("VIZITGoogleSignInEnabled").uppercased()
        guard googleSetting == "YES" || googleSetting == "NO" else {
            throw ConfigurationError.invalid("VIZITGoogleSignInEnabled")
        }

        return AppConfiguration(
            supabaseURL: supabaseURL,
            publishableKey: key,
            callbackURL: callback,
            googleSignInEnabled: googleSetting == "YES",
            privacyPolicyURL: try httpsURL("VIZITPrivacyPolicyURL"),
            privacyPolicyVersion: try value("VIZITPrivacyPolicyVersion"),
            termsURL: try httpsURL("VIZITTermsURL"),
            termsVersion: try value("VIZITTermsVersion"),
            publicProfileBaseURL: try httpsURL("VIZITPublicProfileBaseURL")
        )
    }
}

enum ConfigurationError: LocalizedError {
    case missing(String)
    case invalid(String)

    var errorDescription: String? {
        switch self {
        case .missing(let key): return "Az alkalmazás konfigurációja hiányos (\(key)). Ezt a buildet ne használd éles adatokkal."
        case .invalid(let key): return "Az alkalmazás konfigurációja érvénytelen (\(key)). Ezt a buildet ne használd éles adatokkal."
        }
    }
}
