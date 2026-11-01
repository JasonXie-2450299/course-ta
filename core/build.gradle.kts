plugins { `java-library` }
repositories { mavenCentral() }
dependencies {
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation("com.google.code.gson:gson:2.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
java { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release.set(21) }
tasks.test { useJUnitPlatform() }
tasks.register<JavaExec>("auditCourse") {
    workingDir = rootProject.projectDir
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "edu.groupb.courseta.core.CourseAudit"
    args(providers.gradleProperty("courseDir").getOrElse("samples"),
        rootProject.layout.buildDirectory.dir("audit").get().asFile.absolutePath)
}
