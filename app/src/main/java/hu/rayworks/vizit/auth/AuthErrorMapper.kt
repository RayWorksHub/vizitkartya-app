package hu.rayworks.vizit.auth

fun authErrorMessage(error: Throwable): String = when (error) {
    is EmailConfirmationNotEnforcedException ->
        "Az e-mailes megerősítés átmenetileg nem érhető el. Próbáld újra később."

    else -> authErrorMessage(error.message)
}

internal fun authErrorMessage(rawMessage: String?): String {
    val raw = rawMessage.orEmpty().lowercase()
    return when {
        "invalid login credentials" in raw -> "Hibás e-mail-cím vagy jelszó."
        "email address" in raw && "invalid" in raw -> "Az e-mail-cím formátuma nem megfelelő."
        "email not confirmed" in raw ->
            "Az e-mail-cím még nincs megerősítve. Ellenőrizd a postafiókodat."

        "user already registered" in raw || "already been registered" in raw ->
            "Ezzel az e-mail-címmel már létezik fiók."

        "weak password" in raw || "password" in raw && "weak" in raw ->
            "A jelszó nem felel meg a biztonsági követelményeknek."

        "email rate limit" in raw || "rate limit" in raw || "too many requests" in raw ->
            "Túl sok próbálkozás történt. Várj néhány percet, majd próbáld újra."

        "signup is disabled" in raw || "signups not allowed" in raw ->
            "A regisztráció jelenleg nem érhető el."

        "otp_expired" in raw || "expired" in raw && ("link" in raw || "token" in raw) ->
            "A hitelesítő link lejárt vagy már felhasználták. Kérj új levelet."

        "cancelled" in raw || "canceled" in raw -> "A Google-belépést megszakítottad."

        "network" in raw ||
            "timeout" in raw ||
            "unable to resolve host" in raw ||
            "failed to connect" in raw ||
            "connection" in raw ->
            "Hálózati hiba történt. Ellenőrizd az internetkapcsolatot, majd próbáld újra."

        else -> "A művelet nem sikerült. Próbáld újra."
    }
}
