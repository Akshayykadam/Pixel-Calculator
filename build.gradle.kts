plugins {
    kotlin("jvm") version "2.0.0"
}

group = "app.morphe.patches"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    if (project.hasProperty("gpr.user") && project.hasProperty("gpr.key")) {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = project.findProperty("gpr.user") as String
                password = project.findProperty("gpr.key") as String
            }
        }
    }
}

sourceSets {
    val stub by creating {
        kotlin.srcDir("src/stub/kotlin")
        java.srcDir("src/stub/java")
    }
    main {
        compileClasspath += stub.output
    }
    test {
        compileClasspath += stub.output
        runtimeClasspath += stub.output
    }
}

dependencies {
    compileOnly("org.json:json:20240303")
    testImplementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("pixel-calculator-morphe-patch")
    manifest {
        attributes(
            "Implementation-Title" to "Pixel Calculator Morphe Patch",
            "Implementation-Version" to project.version
        )
    }
}
