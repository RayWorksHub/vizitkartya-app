import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun String.isHttpsDocumentUrl(): Boolean = runCatching {
    URI(trim()).let { uri ->
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.fragment == null
    }
}.getOrDefault(false)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val profileHost = providers.gradleProperty("VIZIT_PROFILE_HOST").orElse("vizit.hu").get()
val devSupabaseUrl = providers.gradleProperty("VIZIT_DEV_SUPABASE_URL").orElse("").get()
val devSupabaseKey = providers.gradleProperty("VIZIT_DEV_SUPABASE_KEY").orElse("").get()
val betaSupabaseUrl = providers.gradleProperty("VIZIT_BETA_SUPABASE_URL").orElse("").get()
val betaSupabaseKey = providers.gradleProperty("VIZIT_BETA_SUPABASE_KEY").orElse("").get()
val prodSupabaseUrl = providers.gradleProperty("VIZIT_PROD_SUPABASE_URL").orElse("").get()
val prodSupabaseKey = providers.gradleProperty("VIZIT_PROD_SUPABASE_KEY").orElse("").get()
val devGoogleWebClientId = providers.gradleProperty("VIZIT_DEV_GOOGLE_WEB_CLIENT_ID").orElse("").get()
val devSigningStoreFile = providers.gradleProperty("VIZIT_DEV_SIGNING_STORE_FILE").orElse("").get()
val devSigningPassword = providers.gradleProperty("VIZIT_DEV_SIGNING_PASSWORD").orElse("").get()
val privacyPolicyUrl = providers.gradleProperty("VIZIT_PRIVACY_POLICY_URL").orElse("").get()
val privacyPolicyVersion = providers.gradleProperty("VIZIT_PRIVACY_POLICY_VERSION").orElse("").get()
val termsUrl = providers.gradleProperty("VIZIT_TERMS_URL").orElse("").get()
val termsVersion = providers.gradleProperty("VIZIT_TERMS_VERSION").orElse("").get()
val legalDocumentsReady =
    privacyPolicyUrl.isHttpsDocumentUrl() &&
        privacyPolicyVersion.isNotBlank() &&
        termsUrl.isHttpsDocumentUrl() &&
        termsVersion.isNotBlank()

android {
    namespace = "hu.rayworks.vizit"
    compileSdk = 36

    defaultConfig {
        applicationId = "hu.rayworks.vizit"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        manifestPlaceholders["profileHost"] = profileHost
        buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
        buildConfigField("String", "PRIVACY_POLICY_VERSION", privacyPolicyVersion.asBuildConfigString())
        buildConfigField("String", "TERMS_URL", termsUrl.asBuildConfigString())
        buildConfigField("String", "TERMS_VERSION", termsVersion.asBuildConfigString())
        buildConfigField(
            "boolean",
            "LEGAL_DOCUMENTS_READY",
            legalDocumentsReady.toString(),
        )
    }

    signingConfigs {
        if (devSigningStoreFile.isNotBlank() && devSigningPassword.isNotBlank()) {
            create("devStable") {
                storeFile = file(devSigningStoreFile)
                storePassword = devSigningPassword
                keyAlias = "vizit-dev"
                keyPassword = devSigningPassword
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appLabel"] = "VIZIT Dev"
            manifestPlaceholders["authScheme"] = "vizit-dev"
            buildConfigField("String", "ENVIRONMENT", "DEV".asBuildConfigString())
            buildConfigField("String", "AUTH_SCHEME", "vizit-dev".asBuildConfigString())
            buildConfigField("String", "SUPABASE_URL", devSupabaseUrl.asBuildConfigString())
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", devSupabaseKey.asBuildConfigString())
            buildConfigField("boolean", "SUPABASE_ENABLED", (devSupabaseUrl.isNotBlank() && devSupabaseKey.isNotBlank()).toString())
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", devGoogleWebClientId.asBuildConfigString())
            buildConfigField("boolean", "GOOGLE_SIGN_IN_ENABLED", devGoogleWebClientId.isNotBlank().toString())
            buildConfigField("String", "PUBLIC_PROFILE_BASE_URL", "https://$profileHost/p".asBuildConfigString())
        }

        create("beta") {
            dimension = "environment"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appLabel"] = "VIZIT Beta"
            manifestPlaceholders["authScheme"] = "vizit-beta"
            buildConfigField("String", "ENVIRONMENT", "BETA".asBuildConfigString())
            buildConfigField("String", "AUTH_SCHEME", "vizit-beta".asBuildConfigString())
            buildConfigField("String", "SUPABASE_URL", betaSupabaseUrl.asBuildConfigString())
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", betaSupabaseKey.asBuildConfigString())
            buildConfigField("boolean", "SUPABASE_ENABLED", (betaSupabaseUrl.isNotBlank() && betaSupabaseKey.isNotBlank()).toString())
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "".asBuildConfigString())
            buildConfigField("boolean", "GOOGLE_SIGN_IN_ENABLED", "false")
            buildConfigField("String", "PUBLIC_PROFILE_BASE_URL", "https://$profileHost/p".asBuildConfigString())
        }

        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appLabel"] = "VIZIT"
            manifestPlaceholders["authScheme"] = "vizit"
            buildConfigField("String", "ENVIRONMENT", "PROD".asBuildConfigString())
            buildConfigField("String", "AUTH_SCHEME", "vizit".asBuildConfigString())
            buildConfigField("String", "SUPABASE_URL", prodSupabaseUrl.asBuildConfigString())
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", prodSupabaseKey.asBuildConfigString())
            buildConfigField("boolean", "SUPABASE_ENABLED", (prodSupabaseUrl.isNotBlank() && prodSupabaseKey.isNotBlank()).toString())
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "".asBuildConfigString())
            buildConfigField("boolean", "GOOGLE_SIGN_IN_ENABLED", "false")
            buildConfigField("String", "PUBLIC_PROFILE_BASE_URL", "https://$profileHost/p".asBuildConfigString())
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            if (devSigningStoreFile.isNotBlank() && devSigningPassword.isNotBlank()) {
                signingConfig = signingConfigs.getByName("devStable")
            }
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val supabaseBom = platform("io.github.jan-tennert.supabase:bom:3.7.0")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.room:room-runtime:2.8.2")
    implementation("androidx.room:room-ktx:2.8.2")
    ksp("androidx.room:room-compiler:2.8.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation(supabaseBom)
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.google.zxing:core:3.5.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
