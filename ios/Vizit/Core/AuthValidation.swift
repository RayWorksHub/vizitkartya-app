import Foundation

public enum AuthValidation {
    private static let emailPattern = #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$"#

    public static func name(_ value: String) -> String? {
        let value = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty { return "Add meg a nevedet." }
        if value.count < 2 { return "A név legalább 2 karakter hosszú legyen." }
        if value.count > 100 { return "A név legfeljebb 100 karakter hosszú lehet." }
        if value.unicodeScalars.contains(where: CharacterSet.controlCharacters.contains) {
            return "A név nem tartalmazhat vezérlőkaraktert."
        }
        return nil
    }

    public static func email(_ value: String) -> String? {
        let value = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty { return "Add meg az e-mail-címedet." }
        if value.range(of: emailPattern, options: [.regularExpression, .caseInsensitive]) == nil {
            return "Az e-mail-cím formátuma nem megfelelő."
        }
        return nil
    }

    public static func password(_ value: String) -> String? {
        if value.isEmpty { return "Add meg a jelszavadat." }
        if value.count < 8 { return "A jelszó legalább 8 karakter hosszú legyen." }
        if !value.contains(where: \.isLetter) { return "A jelszó tartalmazzon legalább egy betűt." }
        if !value.contains(where: \.isNumber) { return "A jelszó tartalmazzon legalább egy számot." }
        return nil
    }

    public static func login(email: String, password: String) -> String? {
        self.email(email) ?? (password.isEmpty ? "Add meg a jelszavadat." : nil)
    }

    public static func registration(name: String, email: String, password: String,
                                    confirmation: String, legalAccepted: Bool) -> String? {
        self.name(name) ?? self.email(email) ?? self.password(password) ?? {
            if confirmation.isEmpty { return "Ismételd meg a jelszavadat." }
            if password != confirmation { return "A két jelszó nem egyezik." }
            if !legalAccepted { return "A regisztrációhoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et." }
            return nil
        }()
    }

    public static func passwordChange(_ password: String, confirmation: String) -> String? {
        self.password(password) ?? {
            if confirmation.isEmpty { return "Ismételd meg az új jelszavadat." }
            if password != confirmation { return "A két jelszó nem egyezik." }
            return nil
        }()
    }

    public static func deletionPhrase(_ value: String) -> String? {
        value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == "TÖRLÉS"
            ? nil : "A törlés megerősítéséhez írd be: TÖRLÉS"
    }
}

public enum AuthCallback {
    /// Rejects lookalike schemes/hosts before the SDK consumes an OAuth or
    /// recovery callback.
    public static func accepts(_ url: URL, expected: URL) -> Bool {
        url.scheme?.lowercased() == expected.scheme?.lowercased()
            && url.host?.lowercased() == expected.host?.lowercased()
            && url.path == expected.path
            && url.port == expected.port
            && url.user == nil && url.password == nil
    }
}
