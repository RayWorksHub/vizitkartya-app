package hu.rayworks.vizit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardLayout
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.card.visibleThrough
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitDigitalCard
import hu.rayworks.vizit.ui.design.components.accentColor
import hu.rayworks.vizit.ui.design.components.endColor
import hu.rayworks.vizit.ui.design.components.startColor
import hu.rayworks.vizit.ui.design.components.VizitDivider
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.design.components.VizitPanel
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitSectionHeader
import hu.rayworks.vizit.ui.design.components.VizitSegmentedControl
import hu.rayworks.vizit.ui.util.rememberProfilePhoto

/**
 * Kártya megjelenése — material, layout and visible elements, decided against a
 * live preview of the owner's own card rather than abstract swatches.
 */
@Composable
fun CardAppearanceScreen(
    profile: ContactProfile,
    presentation: CardPresentation,
    onPresentationChange: (CardPresentation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        ScreenTopBar(title = "Kártya megjelenése", onClose = onClose)

        ProfileCard(profile = profile, presentation = presentation)

        Text(
            text = "Élő előnézet — pontosan ezt látja, akivel megosztod.",
            style = Vizit.type.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        VizitSectionHeader("Színvilág")
        VizitPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardColorway.entries.forEach { colorway ->
                    val selected = presentation.colorway == colorway
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { onPresentationChange(presentation.copy(colorway = colorway)) },
                            )
                            .semantics {
                                contentDescription = colorway.label
                                this.selected = selected
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(2.dp, colors.primary, CircleShape),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(colorway.startColor, colorway.endColor),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colorway.accentColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        VizitSectionHeader("Elrendezés")
        VizitSegmentedControl(
            options = CardLayout.entries.map { it.label },
            selectedIndex = presentation.layout.ordinal,
            onSelect = { onPresentationChange(presentation.copy(layout = CardLayout.entries[it])) },
        )

        VizitSectionHeader("Megjelenő elemek")
        VizitGroup {
            VizitRow(
                label = "Profilkép",
                supporting = if (profile.photoBase64.isBlank()) {
                    "Még nincs feltöltött profilképed."
                } else {
                    null
                },
                showChevron = false,
                checked = presentation.showsPhoto,
                onCheckedChange = { onPresentationChange(presentation.copy(showsPhoto = it)) },
            )
            VizitDivider()
            VizitRow(
                label = "QR-kód a kártyán",
                supporting = "A kártya sarkába kerül, így egy fotóról is beolvasható.",
                showChevron = false,
                checked = presentation.showsQr,
                onCheckedChange = { onPresentationChange(presentation.copy(showsQr = it)) },
            )
            VizitDivider()
            VizitRow(
                label = "Közösségi profilok",
                supporting = if (presentation.sharesSocial) {
                    null
                } else {
                    "Az Adatláthatóságban most ki van kapcsolva."
                },
                enabled = presentation.sharesSocial,
                showChevron = false,
                checked = presentation.showsSocial,
                onCheckedChange = { onPresentationChange(presentation.copy(showsSocial = it)) },
            )
        }

        Text(
            text = "A megjelenés csak ezen a készüléken változik; a névjegyed adatai érintetlenek maradnak.",
            style = Vizit.type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(Vizit.space.xxl))
    }
}

/**
 * Adatláthatóság — per-field control over what leaves the device. Everything
 * switched off here is stripped from the card, the QR and the shared vCard.
 */
@Composable
fun DataVisibilityScreen(
    profile: ContactProfile,
    presentation: CardPresentation,
    onPresentationChange: (CardPresentation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors

    // Who can actually see a field, given both the switch and whether the
    // public web profile is turned on at all.
    fun audience(shared: Boolean): String = when {
        !shared -> "Senki"
        profile.isPublic -> "Mindenki"
        else -> "Csak akivel megosztod"
    }

    data class Field(
        val label: String,
        val value: String,
        val shared: Boolean,
        val apply: (Boolean) -> CardPresentation,
    )

    val fields = listOf(
        Field(
            label = "Cég és beosztás",
            value = listOf(profile.company, profile.jobTitle).filter { it.isNotBlank() }.joinToString(" · "),
            shared = presentation.sharesCompany,
            apply = { presentation.copy(sharesCompany = it) },
        ),
        Field("E-mail-cím", profile.email, presentation.sharesEmail) { presentation.copy(sharesEmail = it) },
        Field("Telefonszám", profile.phone, presentation.sharesPhone) { presentation.copy(sharesPhone = it) },
        Field("Weboldal", profile.website, presentation.sharesWebsite) { presentation.copy(sharesWebsite = it) },
        Field(
            label = "Közösségi profilok",
            value = profile.socialLabels().joinToString(", "),
            shared = presentation.sharesSocial,
            apply = { presentation.copy(sharesSocial = it) },
        ),
        Field("Lakcím", profile.address, presentation.sharesAddress) { presentation.copy(sharesAddress = it) },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.md),
    ) {
        ScreenTopBar(title = "Adatláthatóság", onClose = onClose)

        Text(
            text = "Te döntöd el, mi látszik. Amit itt kikapcsolsz, az sem a megosztott " +
                "névjegyen, sem a nyilvános weboldaladon nem jelenik meg.",
            style = Vizit.type.body,
            color = colors.textSecondary,
        )

        VizitSectionHeader("Mindig látható")
        VizitGroup {
            VizitRow(
                label = "Teljes név",
                value = "kötelező",
                supporting = "A névjegy alapja, nem kapcsolható ki.",
                showChevron = false,
            )
        }

        VizitSectionHeader("Mezőnként állítható")
        VizitGroup {
            fields.forEachIndexed { index, field ->
                if (index > 0) VizitDivider()
                val filled = field.value.isNotBlank()
                VizitRow(
                    label = field.label,
                    supporting = if (filled) audience(field.shared) else "Még nincs kitöltve",
                    enabled = filled,
                    showChevron = false,
                    checked = field.shared,
                    onCheckedChange = { onPresentationChange(field.apply(it)) },
                )
            }
        }

        Text(
            text = "A már megosztott névjegyeken a változás a következő szinkronnál jelenik meg.",
            style = Vizit.type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(Vizit.space.xxl))
    }
}

/** The back-arrow bar every inner screen uses. */
@Composable
private fun ScreenTopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Vissza",
            tint = Vizit.colors.textPrimary,
            modifier = Modifier.size(24.dp).clickable(onClick = onClose),
        )
        Text(
            text = title,
            style = Vizit.type.h2,
            color = Vizit.colors.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

/**
 * The owner's card, rendered exactly as a recipient sees it: styled by their
 * presentation choices and carrying only the fields they left visible. Every
 * screen that shows the card goes through here, so the preview can never drift
 * from what is actually shared.
 */
@Composable
internal fun ProfileCard(
    profile: ContactProfile,
    presentation: CardPresentation,
    modifier: Modifier = Modifier,
) {
    val shown = remember(profile, presentation) { profile.visibleThrough(presentation) }
    VizitDigitalCard(
        fullName = shown.resolvedDisplayName,
        initials = shown.initials,
        jobTitle = shown.jobTitle,
        company = shown.company,
        phone = shown.phone,
        email = shown.email,
        modifier = modifier,
        photo = rememberProfilePhoto(shown.photoBase64),
        presentation = presentation,
        socialLabels = shown.socialLabels(),
        qrCode = rememberCardQr(profile, presentation),
    )
}

/**
 * The card's own miniature QR, generated only when the owner asked for one and
 * the shared profile actually produces a valid payload — a placeholder square
 * would promise a scan that cannot happen.
 */
@Composable
internal fun rememberCardQr(profile: ContactProfile, presentation: CardPresentation): ImageBitmap? {
    val shared = remember(profile, presentation) { profile.visibleThrough(presentation) }
    return remember(shared, presentation.showsQr) {
        if (!presentation.showsQr) {
            null
        } else {
            QrPayloadFactory.contact(shared).getOrNull()
                ?.let { payload -> runCatching { QrCodeGenerator.create(payload, sizePx = 320) }.getOrNull() }
                ?.asImageBitmap()
        }
    }
}

/** The platforms the owner actually filled in, in a stable order. */
internal fun ContactProfile.socialLabels(): List<String> = listOf(
    "LinkedIn" to linkedIn,
    "Facebook" to facebook,
    "Instagram" to instagram,
    "TikTok" to tiktok,
    "YouTube" to youtube,
).filter { it.second.isNotBlank() }.map { it.first }
