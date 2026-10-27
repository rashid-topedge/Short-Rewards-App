import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    // Reads app/google-services.json: Firebase project config + default_web_client_id.
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
fun prop(name: String): String? =
    localProps.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv(name.uppercase().replace('.', '_'))?.trim()?.takeIf { it.isNotEmpty() }

fun String.withTrailingSlash() = if (endsWith("/")) this else "$this/"

// Production API. Override per machine in local.properties:
//   api.base.url=https://...           (all builds)
//   api.base.url.debug=http://10.0.2.2:3000/   (debug builds only, e.g. local Next.js)
val apiBaseUrl = (prop("api.base.url") ?: "https://short-rewards-web-api.vercel.app/").withTrailingSlash()
val debugApiBaseUrl = (prop("api.base.url.debug") ?: apiBaseUrl).withTrailingSlash()

// Release signing secrets come from local.properties (git-ignored) or env vars
// (SIGNING_STOREFILE, SIGNING_STOREPASSWORD, SIGNING_KEYALIAS, SIGNING_KEYPASSWORD) – never from this file.
val signingStoreFile = prop("signing.storeFile")
val signingStorePassword = prop("signing.storePassword")
val signingKeyAlias = prop("signing.keyAlias")
val signingKeyPassword = prop("signing.keyPassword")
val hasReleaseSigning = listOf(signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword)
    .all { it != null }

android {
    namespace = "com.dramflix.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dramflix.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Compile for Java 17 using whichever JDK runs Gradle (Android Studio's bundled
    // JBR works). A jvmToolchain(17) here made Gradle demand a separate JDK 17
    // install, which fails on machines that don't have one.
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            // R8: shrink, optimise and obfuscate code; drop unused resources.
            // Keep rules for reflection-based libraries are in proguard-rules.pro;
            // the mapping file for crash reports is in build/outputs/mapping/release/.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        // Don't run the lintVitalRelease check as part of assembleRelease/bundleRelease.
        // Run lint yourself when needed: ./gradlew lintRelease
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    val ktor = "3.0.3"
    // OkHttp engine: HTTP/2, connection pooling and an on-disk HTTP cache.
    implementation("io.ktor:ktor-client-okhttp:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.ktor:ktor-client-logging:$ktor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.7.0")

    val media3 = "1.11.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // Sign in with Google (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase Auth (anonymous + Google). Config comes from app/google-services.json.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Installs the baseline profile (src/main/baseline-prof.txt + the ones shipped
    // by Compose etc.) for faster cold start, including on sideloaded APKs.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Room (offline-first cache)
    val room = "2.8.5"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")
}
