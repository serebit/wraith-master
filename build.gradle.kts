plugins {
    kotlin("multiplatform") version "1.4.0-rc" apply false
    id("com.github.ben-manes.versions") version "0.29.0"
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "1.1.0"

    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

tasks.register("distTar") {
    dependsOn(":cli:package", ":gtk:package")

    doLast {
        val packageDir = buildDir.resolve("package")
        val tarballName = "wraith-master-$version.tar.xz"

        temporaryDir.resolve("wraith-master").also { tempDir ->
            packageDir.copyRecursively(tempDir, true)

            tempDir.resolve("wraith-master").setExecutable(true)
            tempDir.resolve("wraith-master-gtk").setExecutable(true)
            exec { workingDir = temporaryDir; commandLine("tar", "-cJf", tarballName, tempDir.name) }
        }

        temporaryDir.resolve(tarballName).copyTo(buildDir.resolve("dist/$tarballName"), true)
        temporaryDir.deleteRecursively()
    }
}
