import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun configValue(name: String, defaultValue: String = ""): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .getOrElse(defaultValue)

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "VIZIT Dev")
            buildConfigField("String", "ENVIRONMENT", "DEV".asBuildConfigString())
            buildConfigField(
                "String",
                "SUPABASE_URL",
                configValue("VIZIT_DEV_SUPABASE_URL").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                configValue("VIZIT_DEV_SUPABASE_PUBLISHABLE_KEY").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "PUBLIC_PROFILE_BASE_URL",
                configValue("VIZIT_DEV_PUBLIC_PROFILE_BASE_URL", "https://dev.vizit.hu/p")
                    .asBuildConfigString(),
            )
            buildConfigField(
                "boolean",
                "GOOGLE_AUTH_ENABLED",
                configValue("VIZIT_DEV_GOOGLE_AUTH_ENABLED", "false")
                    .toBooleanStrictOrNull()
                    .orEmptyBoolean(),
            )
        }

        create("beta") {
            dimension = "environment"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            resValue("string", "app_name", "VIZIT Beta")
            buildConfigField("String", "ENVIRONMENT", "BETA".asBuildConfigString())
            buildConfigField(
                "String",
                "SUPABASE_URL",
                configValue("VIZIT_BETA_SUPABASE_URL").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                configValue("VIZIT_BETA_SUPABASE_PUBLISHABLE_KEY").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "PUBLIC_PROFILE_BASE_URL",
                configValue("VIZIT_BETA_PUBLIC_PROFILE_BASE_URL", "https://beta.vizit.hu/p")
                    .asBuildConfigString(),
            )
            buildConfigField(
                "boolean",
                "GOOGLE_AUTH_ENABLED",
                configValue("VIZIT_BETA_GOOGLE_AUTH_ENABLED", "false")
                    .toBooleanStrictOrNull()
                    .orEmptyBoolean(),
            )
        }

        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "VIZIT")
            buildConfigField("String", "ENVIRONMENT", "PROD".asBuildConfigString())
            buildConfigField(
                "String",
                "SUPABASE_URL",
                configValue("VIZIT_PROD_SUPABASE_URL").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                configValue("VIZIT_PROD_SUPABASE_PUBLISHABLE_KEY").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "PUBLIC_PROFILE_BASE_URL",
                configValue("VIZIT_PROD_PUBLIC_PROFILE_BASE_URL", "https://vizit.hu/p")
                    .asBuildConfigString(),
            )
            buildConfigField(
                "boolean",
                "GOOGLE_AUTH_ENABLED",
                configValue("VIZIT_PROD_GOOGLE_AUTH_ENABLED", "false")
                    .toBooleanStrictOrNull()
                    .orEmptyBoolean(),
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

fun Boolean?.orEmptyBoolean(): String = (this ?: false).toString()

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.zxing:core:3.5.4")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
