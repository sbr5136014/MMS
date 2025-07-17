plugins {
    alias(libs.plugins.android.application)
}

android {
    signingConfigs {
        create("linx") {
                    storeFile = file("C:\\Users\\sbr\\Documents\\newAWplatform.jks") // Replace with the actual path
                    storePassword  = "8455379357"
                    keyAlias = "android"
                    keyPassword = "8455379357"

                }
            }

    namespace = "smartart.tech.mmstest"
    compileSdk = 36

    defaultConfig {
        applicationId = "smartart.tech.mmstest"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("linx")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}