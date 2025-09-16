plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

configurations.all {
    resolutionStrategy {
        val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
        force(libsCatalog.findLibrary("kotlin-stdlib").get())
        force(libsCatalog.findLibrary("kotlin-stdlib-jdk7").get())
        force(libsCatalog.findLibrary("kotlin-stdlib-jdk8").get())
    }
}

android {
    namespace = "com.aucepsinnovations.smartimagepicker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aucepsinnovations.smartimagepicker"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":smart-image-picker"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.leakcanary.android)
}