package hu.rayworks.vizit.data.sync

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.local.LocalProfileSnapshot
import hu.rayworks.vizit.data.local.ProfileAddressEntity
import hu.rayworks.vizit.data.local.ProfileContactEntity
import hu.rayworks.vizit.data.local.ProfileEntity
import hu.rayworks.vizit.data.local.ProfileFieldSettingsEntity
import hu.rayworks.vizit.data.local.ProfileLinkEntity
import java.nio.charset.StandardCharsets
import java.util.UUID

object ProfileSnapshotMapper {
    fun toLocalSnapshot(
        userId: String,
        profile: ContactProfile,
        previous: LocalProfileSnapshot?,
        updatedAtEpochMs: Long,
        pendingSync: Boolean,
    ): LocalProfileSnapshot {
        val canonical = profile.canonicalized()
        val previousContacts = previous?.contacts.orEmpty()
        val primaryPhone = previousContacts.firstOrNull { it.kind == "phone" }
        val primaryEmail = previousContacts.firstOrNull { it.kind == "email" }
        val handledContactIds = setOfNotNull(primaryPhone?.id, primaryEmail?.id)
        val contacts = buildList {
            canonical.phone.takeIf(String::isNotBlank)?.let { value ->
                add(
                    primaryPhone?.copy(
                        value = value,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ) ?: ProfileContactEntity(
                        id = stableId(userId, "contact", "phone", "primary"),
                        profileOwnerId = userId,
                        kind = "phone",
                        label = "Mobil",
                        value = value,
                        sortOrder = 0,
                        isPublic = false,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ),
                )
            }
            canonical.email.takeIf(String::isNotBlank)?.let { value ->
                add(
                    primaryEmail?.copy(
                        value = value,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ) ?: ProfileContactEntity(
                        id = stableId(userId, "contact", "email", "primary"),
                        profileOwnerId = userId,
                        kind = "email",
                        label = "E-mail",
                        value = value,
                        sortOrder = 1,
                        isPublic = false,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ),
                )
            }
            addAll(
                previousContacts
                    .filterNot { it.id in handledContactIds }
                    .map { it.copy(updatedAtEpochMs = updatedAtEpochMs, pendingSync = pendingSync) },
            )
        }.sortedWith(compareBy(ProfileContactEntity::sortOrder, ProfileContactEntity::id))

        val previousLinks = previous?.links.orEmpty()
        val managedLinks = canonical.managedLinks()
        val primaryLinks = managedLinks.associateWith { definition ->
            previousLinks.firstOrNull { it.kind == definition.kind }
        }
        val handledLinkIds = primaryLinks.values.mapNotNull { it?.id }.toSet()
        val links = buildList {
            managedLinks.forEach { definition ->
                definition.value.takeIf(String::isNotBlank)?.let { value ->
                    add(
                        primaryLinks[definition]?.copy(
                            url = value,
                            updatedAtEpochMs = updatedAtEpochMs,
                            pendingSync = pendingSync,
                        ) ?: ProfileLinkEntity(
                            id = stableId(userId, "link", definition.kind, "primary"),
                            profileOwnerId = userId,
                            kind = definition.kind,
                            label = definition.label,
                            url = value,
                            sortOrder = definition.sortOrder,
                            isPublic = false,
                            updatedAtEpochMs = updatedAtEpochMs,
                            pendingSync = pendingSync,
                        ),
                    )
                }
            }
            addAll(
                previousLinks
                    .filterNot { it.id in handledLinkIds }
                    .map { it.copy(updatedAtEpochMs = updatedAtEpochMs, pendingSync = pendingSync) },
            )
        }.sortedWith(compareBy(ProfileLinkEntity::sortOrder, ProfileLinkEntity::id))

        val previousAddresses = previous?.addresses.orEmpty()
        val primaryAddress = previousAddresses.firstOrNull()
        val addresses = buildList {
            canonical.address.takeIf(String::isNotBlank)?.let { value ->
                add(
                    primaryAddress?.copy(
                        formattedAddress = value,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ) ?: ProfileAddressEntity(
                        id = stableId(userId, "address", "primary"),
                        profileOwnerId = userId,
                        label = "Cím",
                        formattedAddress = value,
                        sortOrder = 0,
                        isPublic = false,
                        updatedAtEpochMs = updatedAtEpochMs,
                        pendingSync = pendingSync,
                    ),
                )
            }
            addAll(
                previousAddresses
                    .drop(if (primaryAddress == null) 0 else 1)
                    .map { it.copy(updatedAtEpochMs = updatedAtEpochMs, pendingSync = pendingSync) },
            )
        }.sortedWith(compareBy(ProfileAddressEntity::sortOrder, ProfileAddressEntity::id))

        return LocalProfileSnapshot(
            profile = ProfileEntity(
                userId = userId,
                firstName = canonical.firstName,
                lastName = canonical.lastName,
                displayName = canonical.fullName,
                company = canonical.company,
                jobTitle = canonical.jobTitle,
                bio = previous?.profile?.bio.orEmpty(),
                displayImagePath = if (canonical.photoBase64 == previous?.profile?.localContactPhotoBase64)
                    previous?.profile?.displayImagePath else null,
                contactImagePath = previous?.profile?.contactImagePath,
                logoPath = previous?.profile?.logoPath,
                publicSlug = canonical.publicSlug.ifBlank { null },
                isPublic = canonical.isPublic,
                updatedAtEpochMs = updatedAtEpochMs,
                pendingSync = pendingSync,
                localContactPhotoBase64 = canonical.photoBase64,
            ),
            contacts = contacts,
            links = links,
            addresses = addresses,
            fieldSettings = ProfileFieldSettingsEntity(
                userId = userId,
                fieldOrderJson = previous?.fieldSettings?.fieldOrderJson
                    ?: ProfilePayloadCodec.encodeFieldOrder(emptyList()),
                fieldVisibilityJson = previous?.fieldSettings?.fieldVisibilityJson
                    ?: ProfilePayloadCodec.encodeFieldVisibility(emptyMap()),
                updatedAtEpochMs = updatedAtEpochMs,
                pendingSync = pendingSync,
            ),
        )
    }

    fun toLocalSnapshot(
        userId: String,
        payload: ProfileSyncPayload,
        previous: LocalProfileSnapshot?,
        updatedAtEpochMs: Long,
    ): LocalProfileSnapshot = LocalProfileSnapshot(
        profile = ProfileEntity(
            userId = userId,
            firstName = payload.firstName,
            lastName = payload.lastName,
            displayName = payload.displayName,
            company = payload.company,
            jobTitle = payload.jobTitle,
            bio = payload.bio,
            displayImagePath = payload.displayImagePath,
            contactImagePath = payload.contactImagePath,
            logoPath = payload.logoPath,
            publicSlug = payload.publicSlug,
            isPublic = payload.isPublic,
            updatedAtEpochMs = updatedAtEpochMs,
            pendingSync = false,
            localContactPhotoBase64 = payload.photoBase64 ?: previous?.profile?.localContactPhotoBase64.orEmpty(),
        ),
        contacts = payload.contacts.map {
            ProfileContactEntity(
                id = it.id,
                profileOwnerId = userId,
                kind = it.kind,
                label = it.label,
                value = it.value,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
                updatedAtEpochMs = updatedAtEpochMs,
                pendingSync = false,
            )
        },
        addresses = payload.addresses.map {
            ProfileAddressEntity(
                id = it.id,
                profileOwnerId = userId,
                label = it.label,
                formattedAddress = it.formattedAddress,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
                updatedAtEpochMs = updatedAtEpochMs,
                pendingSync = false,
            )
        },
        links = payload.links.map {
            ProfileLinkEntity(
                id = it.id,
                profileOwnerId = userId,
                kind = it.kind,
                label = it.label,
                url = it.url,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
                updatedAtEpochMs = updatedAtEpochMs,
                pendingSync = false,
            )
        },
        fieldSettings = ProfileFieldSettingsEntity(
            userId = userId,
            fieldOrderJson = ProfilePayloadCodec.encodeFieldOrder(payload.fieldOrder),
            fieldVisibilityJson = ProfilePayloadCodec.encodeFieldVisibility(payload.fieldVisibility),
            updatedAtEpochMs = updatedAtEpochMs,
            pendingSync = false,
        ),
    )

    fun toPayload(snapshot: LocalProfileSnapshot): ProfileSyncPayload = ProfileSyncPayload(
        photoBase64 = snapshot.profile.localContactPhotoBase64,
        firstName = snapshot.profile.firstName,
        lastName = snapshot.profile.lastName,
        displayName = snapshot.profile.displayName,
        company = snapshot.profile.company,
        jobTitle = snapshot.profile.jobTitle,
        bio = snapshot.profile.bio,
        displayImagePath = snapshot.profile.displayImagePath,
        contactImagePath = snapshot.profile.contactImagePath,
        logoPath = snapshot.profile.logoPath,
        publicSlug = snapshot.profile.publicSlug,
        isPublic = snapshot.profile.isPublic,
        fieldOrder = snapshot.fieldSettings?.fieldOrderJson
            ?.let { runCatching { ProfilePayloadCodec.decodeFieldOrder(it) }.getOrNull() }
            .orEmpty(),
        fieldVisibility = snapshot.fieldSettings?.fieldVisibilityJson
            ?.let { runCatching { ProfilePayloadCodec.decodeFieldVisibility(it) }.getOrNull() }
            .orEmpty(),
        contacts = snapshot.contacts.map {
            ProfileContactPayload(
                id = it.id,
                kind = it.kind,
                label = it.label,
                value = it.value,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
            )
        },
        addresses = snapshot.addresses.map {
            ProfileAddressPayload(
                id = it.id,
                label = it.label,
                formattedAddress = it.formattedAddress,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
            )
        },
        links = snapshot.links.map {
            ProfileLinkPayload(
                id = it.id,
                kind = it.kind,
                label = it.label,
                url = it.url,
                sortOrder = it.sortOrder,
                isPublic = it.isPublic,
            )
        },
    )

    fun toContactProfile(snapshot: LocalProfileSnapshot?): ContactProfile {
        if (snapshot == null) return ContactProfile()
        val phone = snapshot.contacts.firstOrNull { it.kind == "phone" }?.value.orEmpty()
        val email = snapshot.contacts.firstOrNull { it.kind == "email" }?.value.orEmpty()
        val website = snapshot.links.firstOrNull { it.kind == "website" }?.url.orEmpty()
        val linkedIn = snapshot.links.firstOrNull { it.kind == "linkedin" }?.url.orEmpty()
        val facebook = snapshot.links.firstOrNull { it.kind == "facebook" }?.url.orEmpty()
        val instagram = snapshot.links.firstOrNull { it.kind == "instagram" }?.url.orEmpty()
        val tiktok = snapshot.links.firstOrNull { it.kind == "tiktok" }?.url.orEmpty()
        val youtube = snapshot.links.firstOrNull { it.kind == "youtube" }?.url.orEmpty()
        return ContactProfile(
            fullName = snapshot.profile.displayName,
            firstName = snapshot.profile.firstName,
            lastName = snapshot.profile.lastName,
            jobTitle = snapshot.profile.jobTitle,
            company = snapshot.profile.company,
            phone = phone,
            email = email,
            website = website,
            address = snapshot.addresses.firstOrNull()?.formattedAddress.orEmpty(),
            linkedIn = linkedIn,
            facebook = facebook,
            instagram = instagram,
            tiktok = tiktok,
            youtube = youtube,
            photoBase64 = snapshot.profile.localContactPhotoBase64,
            publicSlug = snapshot.profile.publicSlug.orEmpty(),
            isPublic = snapshot.profile.isPublic,
        )
    }

    private fun ContactProfile.canonicalized(): ContactProfile = copy(
        fullName = fullName.trim(),
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        jobTitle = jobTitle.trim(),
        company = company.trim(),
        phone = phone.trim(),
        email = email.trim(),
        website = website.trim(),
        address = address.trim(),
        linkedIn = linkedIn.trim(),
        facebook = facebook.trim(),
        instagram = instagram.trim(),
        tiktok = tiktok.trim(),
        youtube = youtube.trim(),
        publicSlug = publicSlug.trim(),
    )

    private fun ContactProfile.managedLinks(): List<ManagedLink> = listOf(
        ManagedLink("website", "Weboldal", 0, website),
        ManagedLink("linkedin", "LinkedIn", 1, linkedIn),
        ManagedLink("facebook", "Facebook", 2, facebook),
        ManagedLink("instagram", "Instagram", 3, instagram),
        ManagedLink("tiktok", "TikTok", 4, tiktok),
        ManagedLink("youtube", "YouTube", 5, youtube),
    )

    private data class ManagedLink(
        val kind: String,
        val label: String,
        val sortOrder: Int,
        val value: String,
    )

    private fun stableId(vararg components: String): String = UUID.nameUUIDFromBytes(
        components.joinToString("|").toByteArray(StandardCharsets.UTF_8),
    ).toString()
}
