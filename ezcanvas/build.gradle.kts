plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.ezcanvas"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    // Expose a single "release" variant for publishing (required by JitPack).
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}

// Publication consumed by JitPack:
//   implementation("com.github.RotemBar18:EZCanvas:<version>")
//
// The group and artifact below are what the local publication uses. JitPack finds that artifact
// and then republishes it under the repository coordinate, com.github.<user>:<repo>, because this
// build publishes a single module. The dependency line above is therefore the one that resolves.
//
// JitPack builds a git tag, then looks for an artifact whose version matches the one requested.
// This value is therefore the release version, and the git tag has to carry the same name. Tagging
// "v1.0.0" while publishing "1.0.0" makes JitPack report the tag build as failed, because it finds
// nothing under the version it was asked for. Bump this and the tag together.
val releaseVersion = "1.1.0"

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.RotemBar18.EZCanvas"
                artifactId = "ezcanvas"
                version = releaseVersion
            }
        }
    }
}
