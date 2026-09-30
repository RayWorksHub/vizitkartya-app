package hu.rayworks.vizit.v10.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardPresentation
import java.io.ByteArrayOutputStream

internal fun ContactProfile.toV10Profile(
    id: String,
    label: String,
    presentation: CardPresentation,
): Profile = Profile(
    id = id,
    label = label.ifBlank { resolvedDisplayName },
    real = true,
    name = resolvedDisplayName,
    title = jobTitle,
    company = company,
    bio = bio,
    phone = phone,
    email = email,
    web = website,
    address = address,
    socials = mapOf(
        "linkedin" to linkedIn,
        "facebook" to facebook,
        "instagram" to instagram,
        "tiktok" to tiktok,
        "youtube" to youtube,
        "x" to x,
        "github" to github,
        "other" to customSocial,
    ),
    photo = photoBase64.toPicture(),
    logo = logoBase64.toPicture(),
    isPublic = isPublic,
    slug = publicSlug,
    customDomain = customDomain,
    domainVerified = customDomainVerified,
    presetId = presentation.colorway.toPresetId(),
    layout = if (presentation.layout == hu.rayworks.vizit.data.card.CardLayout.LANDSCAPE) {
        CardLayout.Landscape
    } else {
        CardLayout.Portrait
    },
    showPhoto = presentation.showsPhoto,
    showQr = presentation.showsQr,
    showSocial = presentation.showsSocial,
    vis = mapOf(
        "company" to presentation.sharesCompany,
        "email" to presentation.sharesEmail,
        "phone" to presentation.sharesPhone,
        "web" to presentation.sharesWebsite,
        "social" to presentation.sharesSocial,
        "address" to presentation.sharesAddress,
    ),
)

internal fun Profile.toContactProfile(context: Context): ContactProfile = ContactProfile(
    fullName = name,
    jobTitle = title,
    company = company,
    bio = bio,
    phone = phone,
    email = email,
    website = web,
    address = address,
    linkedIn = socials["linkedin"].orEmpty(),
    facebook = socials["facebook"].orEmpty(),
    instagram = socials["instagram"].orEmpty(),
    tiktok = socials["tiktok"].orEmpty(),
    youtube = socials["youtube"].orEmpty(),
    x = socials["x"].orEmpty(),
    github = socials["github"].orEmpty(),
    customSocial = socials["other"].orEmpty(),
    photoBase64 = photo.toBase64(context),
    logoBase64 = logo.toBase64(context),
    publicSlug = slug,
    isPublic = isPublic,
    customDomain = customDomain,
    customDomainVerified = domainVerified,
)

internal fun Profile.toCardPresentation(): CardPresentation = CardPresentation(
    colorway = when (presetId) {
        "markakek" -> CardColorway.BRAND
        "smaragd" -> CardColorway.EMERALD
        "ametiszt" -> CardColorway.AMETHYST
        "rez" -> CardColorway.COPPER
        "grafit" -> CardColorway.GRAPHITE
        else -> CardColorway.INK
    },
    layout = if (layout == CardLayout.Landscape) {
        hu.rayworks.vizit.data.card.CardLayout.LANDSCAPE
    } else {
        hu.rayworks.vizit.data.card.CardLayout.PORTRAIT
    },
    showsPhoto = showPhoto,
    showsQr = showQr,
    showsSocial = showSocial,
    sharesCompany = vis["company"] != false,
    sharesEmail = vis["email"] != false,
    sharesPhone = vis["phone"] != false,
    sharesWebsite = vis["web"] != false,
    sharesSocial = vis["social"] != false,
    sharesAddress = vis["address"] != false,
)

private fun CardColorway.toPresetId(): String = when (this) {
    CardColorway.INK -> "tinta"
    CardColorway.BRAND -> "markakek"
    CardColorway.EMERALD -> "smaragd"
    CardColorway.AMETHYST -> "ametiszt"
    CardColorway.COPPER -> "rez"
    CardColorway.GRAPHITE -> "grafit"
}

private fun String.toPicture(): Pic? {
    if (isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.let(Pic::Bmp)
    }.getOrNull()
}

private fun Pic?.toBase64(context: Context): String {
    val bitmap = when (this) {
        is Pic.Bmp -> bitmap.asAndroidBitmap()
        is Pic.Res -> BitmapFactory.decodeResource(context.resources, id)
        null -> return ""
    } ?: return ""
    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
