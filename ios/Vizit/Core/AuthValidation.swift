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
        if !value.contains(where: \.isLowercase) { return "A jelszó tartalmazzon legalább egy kisbetűt." }
        if !value.contains(where: \.isUppercase) { return "A jelszó tartalmazzon legalább egy nagybetűt." }
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

public enum AuthOperation: Sendable, Equatable {
    case login
    case registration
    case passwordResetRequest
    case callback
    case passwordChange
}

/// Turns stable Supabase Auth error codes into actionable, non-sensitive copy.
/// Unknown failures intentionally fall back to the operation-specific message.
public enum AuthFailureMessage {
    public static func text(
        operation: AuthOperation,
        errorCode: String?,
        httpStatus: Int?,
        diagnostic: String?
    ) -> String? {
        let code = errorCode?.lowercased()
        let detail = diagnostic?.lowercased() ?? ""

        if code == "bad_code_verifier"
            || detail.contains("code verifier")
            || detail.contains("pkce") {
            return "Ezt a hivatkozást egy másik vagy régebbi VIZIT-verzió kérte. Frissíts a legúj TestFlight-buildre, majd kérj új hivatkozást."
        }

        switch code {
        case "invalid_credentials":
            return "Hibás e-mail-cím vagy jelszó. Ha nem emlékszel a jelszóra, kérj újat."
        case "email_not_confirmed":
            return "Az e-mail-cím még nincs megerősítve. Nyisd meg a legutóbbi megerősítő levelet."
        case "over_email_send_rate_limit", "over_request_rate_limit":
            return "Túl gyorsan kértél új levelet. Várj legalább 15 másodpercet, majd próbáld újra."
        case "flow_state_not_found", "flow_state_expired", "otp_expired":
            return "A hivatkozás lejárt vagy már felhasználták. Kérj új jelszó-visszaállító levelet."
        case "weak_password":
            return "A jelszó nem elég erős. Használj legalább 8 karaktert, kis- és nagybetűt, valamint számot."
        case "same_password":
            return "Az új jelszó nem lehet ugyanaz, mint a jelenlegi."
        case "email_exists", "user_already_exists":
            return "Ehhez az e-mail-címhez már tartozik fiók. Lépj be, vagy kérj új jelszót."
        case "user_banned":
            return "Ez a fiók jelenleg nem használható. Vedd fel a kapcsolatot a VIZIT támogatásával."
        default:
            break
        }

        if operation == .passwordResetRequest, httpStatus == 429 {
            return "Túl gyorsan kértél új levelet. Várj legalább 15 másodpercet, majd próbáld újra."
        }
        if operation == .callback,
           detail.contains("expired") || detail.contains("invalid") || detail.contains("no code") {
            return "A hivatkozás lejárt vagy már felhasználták. Kérj új jelszó-visszaállító levelet."
        }
        return nil
    }
}

public enum PasswordResetPolicy {
    public static let cooldown: TimeInterval = 15

    public static func remainingSeconds(
        since lastRequest: Date?,
        now: Date = Date(),
        cooldown: TimeInterval = cooldown
    ) -> Int {
        guard let lastRequest else { return 0 }
        return max(0, Int(ceil(cooldown - now.timeIntervalSince(lastRequest))))
    }
}
