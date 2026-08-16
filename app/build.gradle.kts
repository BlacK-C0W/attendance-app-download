import java.io.FileInputStream
import java.util.Properties

plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")

}

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("signing.properties")
if (signingPropertiesFile.exists()) {
    FileInputStream(signingPropertiesFile).use(signingProperties::load)
}


android {


    namespace = "com.example.attendanceappfinal"


    compileSdk = 37



    defaultConfig {


        applicationId = "com.example.attendanceappfinal"


        minSdk = 26


        targetSdk = 37


        versionCode = 16


        versionName = "1.0.15"


    }



    compileOptions {


        sourceCompatibility = JavaVersion.VERSION_17


        targetCompatibility = JavaVersion.VERSION_17


    }



    buildFeatures {


        compose = true
        buildConfig = true


    }

    signingConfigs {
        create("release") {
            if (signingPropertiesFile.exists()) {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }


}



dependencies {


    implementation(platform("androidx.compose:compose-bom:2025.06.01"))

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("androidx.activity:activity-compose:1.10.1")

    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.ui:ui")

    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.core:core-ktx:1.16.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")



    // Firebase

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    implementation("com.google.firebase:firebase-database")

    implementation("com.google.firebase:firebase-auth")

    implementation("com.google.firebase:firebase-messaging")

// Fragment Activity Result API 호환
    implementation("androidx.fragment:fragment-ktx:1.8.6")

    testImplementation("junit:junit:4.13.2")

}
