plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.1.0.6387"
    id("org.owasp.dependencycheck") version "12.2.0"
}

val seleniumJavaVersion = "4.40.0"
val seleniumJupiterVersion = "6.3.1"
val webdrivermanagerVersion = "6.3.3"

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "eshop"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.seleniumhq.selenium:selenium-java:${seleniumJavaVersion}")
    testImplementation("io.github.bonigarcia:selenium-jupiter:${seleniumJupiterVersion}")
    testImplementation("io.github.bonigarcia:webdrivermanager:${webdrivermanagerVersion}")
}
val sonarHostUrlProvider = providers.gradleProperty("sonarHostUrl")
    .orElse(providers.environmentVariable("SONAR_HOST_URL"))
    .orElse("https://sonarcloud.io")
val sonarTokenProvider = providers.gradleProperty("sonarToken")
    .orElse(providers.environmentVariable("SONAR_TOKEN"))
val githubRepositoryProvider = providers.environmentVariable("GITHUB_REPOSITORY")
val nvdApiKeyProvider = providers.environmentVariable("NVD_API_KEY")

sonar {
    properties {
        val githubRepository = githubRepositoryProvider.orNull

        property("sonar.host.url", sonarHostUrlProvider.get())
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile.absolutePath
        )
        property(
            "sonar.junit.reportPaths",
            layout.buildDirectory.dir("test-results/test").get().asFile.absolutePath
        )

        if (!sonarTokenProvider.orNull.isNullOrBlank()) {
            property("sonar.token", sonarTokenProvider.get())
        }
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    scanConfigurations = listOf("runtimeClasspath", "testRuntimeClasspath")
    failBuildOnCVSS = 11.0F
    nvd.apiKey = nvdApiKeyProvider.orNull
}



tasks.register<Test>("unitTest") {
    description = "Runs unit tests."
    group = "verification"

    filter {
        excludeTestsMatching("*FunctionalTest")
    }
}

tasks.register<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"

    filter {
        includeTestsMatching("*FunctionalTest")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.test {
    filter{
        excludeTestsMatching("*FunctionalTest")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.matching { it.name == "sonar" || it.name == "sonarqube" }.configureEach {
    dependsOn(tasks.test, tasks.jacocoTestReport)
}
