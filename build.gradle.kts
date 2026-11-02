plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "edu.groupb.courseta"
version = "0.3.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation(project(":core"))
    intellijPlatform {
        val localPath = providers.gradleProperty("localIdePath")
        if (localPath.isPresent) local(localPath.get()) else intellijIdea("2026.1.1")
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion)) }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release.set(21) }
intellijPlatform {
    pluginConfiguration {
        id = "edu.groupb.course-ta"
        name = "Course TA"
        version = project.version.toString()
        ideaVersion { sinceBuild = "261"; untilBuild = "261.*" }
        vendor { name = "Group B" }
    }
    buildSearchableOptions = false
}

tasks.register<JavaExec>("renderUiSample") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "edu.groupb.courseta.ide.UiSampleRenderer"
    args(layout.buildDirectory.file("audit/course-index.json").get().asFile.absolutePath,
        layout.buildDirectory.dir("ui-samples").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}
