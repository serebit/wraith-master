plugins {
    kotlin("multiplatform") version "1.3.70-eap-274" apply false
    base // to add clean task, for cleaning up package output
}

subprojects {
    group = "com.serebit.wraith"
    version = "0.5.0-dev"

    repositories { 
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap") 
    }
}
