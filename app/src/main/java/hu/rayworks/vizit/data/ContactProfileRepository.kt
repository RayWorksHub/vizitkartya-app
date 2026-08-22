package hu.rayworks.vizit.data

import android.content.Context
import androidx.core.content.edit

class ContactProfileRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ContactProfile = ContactProfile(
        fullName = preferences.getString(KEY_FULL_NAME, "").orEmpty(),
        firstName = preferences.getString(KEY_FIRST_NAME, "").orEmpty(),
        lastName = preferences.getString(KEY_LAST_NAME, "").orEmpty(),
        jobTitle = preferences.getString(KEY_JOB_TITLE, "").orEmpty(),
        company = preferences.getString(KEY_COMPANY, "").orEmpty(),
        phone = preferences.getString(KEY_PHONE, "").orEmpty(),
        email = preferences.getString(KEY_EMAIL, "").orEmpty(),
        website = preferences.getString(KEY_WEBSITE, "").orEmpty(),
        address = preferences.getString(KEY_ADDRESS, "").orEmpty(),
        linkedIn = preferences.getString(KEY_LINKED_IN, "").orEmpty(),
        publicProfileSlug = preferences.getString(KEY_PUBLIC_PROFILE_SLUG, "").orEmpty(),
        publicProfileUrl = preferences.getString(KEY_PUBLIC_PROFILE_URL, "").orEmpty(),
        note = preferences.getString(KEY_NOTE, "").orEmpty(),
        photoBase64 = preferences.getString(KEY_PHOTO, "").orEmpty(),
    )

    fun save(profile: ContactProfile) {
        preferences.edit {
            putString(KEY_FULL_NAME, profile.fullName.trim())
            putString(KEY_FIRST_NAME, profile.firstName.trim())
            putString(KEY_LAST_NAME, profile.lastName.trim())
            putString(KEY_JOB_TITLE, profile.jobTitle.trim())
            putString(KEY_COMPANY, profile.company.trim())
            putString(KEY_PHONE, profile.phone.trim())
            putString(KEY_EMAIL, profile.email.trim())
            putString(KEY_WEBSITE, profile.website.trim())
            putString(KEY_ADDRESS, profile.address.trim())
            putString(KEY_LINKED_IN, profile.linkedIn.trim())
            putString(KEY_PUBLIC_PROFILE_SLUG, profile.publicProfileSlug.trim())
            putString(KEY_PUBLIC_PROFILE_URL, profile.publicProfileUrl.trim())
            putString(KEY_NOTE, profile.note.trim())
            putString(KEY_PHOTO, profile.photoBase64)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "vizit_profile"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_JOB_TITLE = "job_title"
        const val KEY_COMPANY = "company"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_WEBSITE = "website"
        const val KEY_ADDRESS = "address"
        const val KEY_LINKED_IN = "linked_in"
        const val KEY_PUBLIC_PROFILE_SLUG = "public_profile_slug"
        const val KEY_PUBLIC_PROFILE_URL = "public_profile_url"
        const val KEY_NOTE = "note"
        const val KEY_PHOTO = "photo_base64"
    }
}
