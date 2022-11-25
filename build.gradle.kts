plugins {
    `java-library`
    `maven-publish`
}

group = "org.vertex.mindustry"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val mindustryVersion = "v140.4"
    implementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("${buildDir}/publishing-repository")
        }
    }
}

tasks.jar {
    archiveFileName.set("Mindustry.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}