plugins {
    kotlin("multiplatform") version "1.3.61" apply false
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
}
