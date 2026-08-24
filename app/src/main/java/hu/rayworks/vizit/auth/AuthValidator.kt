package hu.rayworks.vizit.auth

import java.util.Locale

object AuthValidator {
    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun name(name: String): String? = when {
        name.isBlank() -> "Add meg a nevedet."
        name.trim().length < 2 -> "A név legalább 2 karakter hosszú legyen."
        name.trim().length > 100 -> "A név legfeljebb 100 karakter hosszú lehet."
        else -> null
    }

    fun email(email: String): String? = when {
        email.isBlank() -> "Add meg az e-mail-címedet."
        !emailRegex.matches(email.trim()) -> "Az e-mail-cím formátuma nem megfelelő."
        else -> null
    }

    fun password(password: String): String? = when {
        password.isBlank() -> "Add meg a jelszavadat."
        password.length < 8 -> "A jelszó legalább 8 karakter hosszú legyen."
        password.none(Char::isLetter) -> "A jelszó tartalmazzon legalább egy betűt."
        password.none(Char::isDigit) -> "A jelszó tartalmazzon legalább egy számot."
        else -> null
    }

    fun login(email: String, password: String): String? =
        email(email) ?: if (password.isBlank()) "Add meg a jelszavadat." else null

    fun passwordChange(password: String, confirmation: String): String? =
        password(password) ?: when {
            confirmation.isBlank() -> "Ismételd meg az új jelszavadat."
            password != confirmation -> "A két jelszó nem egyezik."
            else -> null
        }

    fun registration(
        name: String,
        email: String,
        password: String,
        confirmation: String,
        legalAccepted: Boolean,
    ): String? = name(name) ?: email(email) ?: password(password) ?: when {
        confirmation.isBlank() -> "Ismételd meg a jelszavadat."
        password != confirmation -> "A két jelszó nem egyezik."
        !legalAccepted ->
            "A regisztrációhoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et."

        else -> null
    }

    fun accountDeletionConfirmation(value: String): String? =
        if (value.trim().uppercase(Locale.ROOT) == ACCOUNT_DELETION_PHRASE) null
        else "A törlés megerősítéséhez írd be: $ACCOUNT_DELETION_PHRASE"

    const val ACCOUNT_DELETION_PHRASE = "TÖRLÉS"
}
