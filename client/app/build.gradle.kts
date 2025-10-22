plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.protobuf")
}

android {
    namespace = "xyz.jdubiel.migawka"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.jdubiel.migawka"
        minSdk = 29
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


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        // Correct way to define plugins using create
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.61.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                // Correct way to apply the created plugins
                it.plugins.create("grpc") {
                    option("lite")
                }
                it.plugins.create("grpckt") {
                    option("lite")
                }
            }
            it.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    
    implementation("io.grpc:grpc-okhttp:1.61.0")
    implementation("io.grpc:grpc-protobuf-lite:1.61.0")
    implementation("io.grpc:grpc-stub:1.61.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
}