plugins {
    kotlin("multiplatform") version "1.3.70-eap-42" apply false
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "0.4.0-dev"

    repositories {
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

tasks.register("package") {
    dependsOn("build")
    dependsOn(":core:package")
    dependsOn(":gtk:package")
    dependsOn(":cli:package")
}

tasks.register("install") {
    dependsOn("package")
    dependsOn(":core:install")
    dependsOn(":gtk:install")
    dependsOn(":cli:install")
}
