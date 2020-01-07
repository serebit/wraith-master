plugins {
    kotlin("multiplatform") version "1.3.61" apply false
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "0.3.0-dev"

    repositories {
        jcenter()
    }
}

tasks.register("package") {
    dependsOn(":gtk:package")
    dependsOn(":cli:package")
    outputs.cacheIf { false }
}

tasks.register("install") {
    dependsOn("package")
    dependsOn(":gtk:install")
    dependsOn(":cli:install")
}
