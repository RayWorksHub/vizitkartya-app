package hu.rayworks.vizit.auth

object AuthValidator {
    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun email(email: String): String? = when {
        email.isBlank() -> "Add meg az e-mail-címedet."
        !emailRegex.matches(email.trim()) -> "Az e-mail-cím formátuma nem megfelelő."
        else -> null
    }

    fun password(password: String): String? = when {
        password.length < 8 -> "A jelszó legalább 8 karakter hosszú legyen."
        password.none(Char::isLetter) -> "A jelszó tartalmazzon legalább egy betűt."
        password.none(Char::isDigit) -> "A jelszó tartalmazzon legalább egy számot."
        else -> null
    }

    fun registration(
        email: String,
        password: String,
        confirmation: String,
        legalAccepted: Boolean,
    ): String? = email(email) ?: password(password) ?: when {
        password != confirmation -> "A két jelszó nem egyezik."
        !legalAccepted ->
            "A regisztrációhoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et."

        else -> null
    }
}
