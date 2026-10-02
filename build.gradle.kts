plugins {
    java
}

group = "de.derjannik"
version = "1.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("SleepWarp-${project.version}.jar")
}

val copyToDelivery = tasks.register<Copy>("copyToDelivery") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into(file("delivery"))
}

tasks.build {
    finalizedBy(copyToDelivery)
}
