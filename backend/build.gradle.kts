import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
    id("org.openapi.generator") version "7.15.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("net.jqwik:jqwik:1.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.28.0")
    }
}

val contractFile = rootProject.layout.projectDirectory.file("api/openapi.yaml")
val springOutput = layout.buildDirectory.dir("generated/openapi")
val angularOutput = layout.buildDirectory.dir("generated/angular")
val committedClient = rootProject.layout.projectDirectory.dir("frontend/src/app/generated/api")

openApiValidate {
    inputSpec.set(contractFile.asFile.absolutePath)
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(contractFile.asFile.absolutePath)
    outputDir.set(springOutput.map { it.asFile.absolutePath })
    cleanupOutput.set(true)
    apiPackage.set("com.example.supermarket.generated.api")
    modelPackage.set("com.example.supermarket.generated.model")
    globalProperties.set(mapOf("apis" to "", "models" to "", "apiDocs" to "false", "modelDocs" to "false", "apiTests" to "false", "modelTests" to "false"))
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "requestMappingMode" to "api_interface",
        "useTags" to "true",
        "openApiNullable" to "false",
        "containerDefaultToNull" to "true",
        "annotationLibrary" to "none",
        "documentationProvider" to "none",
        "hideGenerationTimestamp" to "true",
        "additionalModelTypeAnnotations" to "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)"
    ))
}

tasks.openApiGenerate {
    dependsOn(tasks.openApiValidate)
}

sourceSets.main {
    java.srcDir(springOutput.map { it.dir("src/main/java") })
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.check {
    dependsOn(tasks.openApiValidate)
}

val generateAngularSources by tasks.registering(GenerateTask::class) {
    group = "openapi tools"
    description = "Generate the reproducible Angular client into build output."
    dependsOn(tasks.openApiValidate)
    generatorName.set("typescript-angular")
    inputSpec.set(contractFile.asFile.absolutePath)
    outputDir.set(angularOutput.map { it.asFile.absolutePath })
    cleanupOutput.set(true)
    configOptions.set(mapOf(
        "ngVersion" to "22.0.0",
        "providedIn" to "root",
        "hideGenerationTimestamp" to "true",
        "stringEnums" to "true"
    ))
}

// Only TypeScript sources belong in the app, not generator metadata or package scaffolding.
val angularSources = fileTree(angularOutput) { include("**/*.ts") }

tasks.register<Sync>("generateFrontendApi") {
    group = "openapi tools"
    description = "Replace the committed Angular client with freshly generated TypeScript sources."
    dependsOn(generateAngularSources)
    from(angularSources)
    into(committedClient)
}

tasks.register("checkFrontendApi") {
    group = "verification"
    description = "Fail on missing, extra or changed Angular client files without changing the source tree."
    dependsOn(generateAngularSources)
    // Order explicitly when both are requested; never regenerate committed files just to check them.
    mustRunAfter("generateFrontendApi")
    inputs.files(angularSources)
    inputs.dir(committedClient)
    doLast {
        val expectedRoot = angularOutput.get().asFile
        val actualRoot = committedClient.asFile
        val expected = angularSources.files.associateBy { it.relativeTo(expectedRoot).invariantSeparatorsPath }
        val actual = fileTree(actualRoot).files.associateBy { it.relativeTo(actualRoot).invariantSeparatorsPath }
        val missing = expected.keys - actual.keys
        val extra = actual.keys - expected.keys
        val changed = (expected.keys intersect actual.keys).filter {
            !expected.getValue(it).readBytes().contentEquals(actual.getValue(it).readBytes())
        }
        if (missing.isNotEmpty() || extra.isNotEmpty() || changed.isNotEmpty()) {
            throw GradleException(
                "Generated Angular client is out of date. " +
                    "Missing: ${missing.sorted()}; extra: ${extra.sorted()}; changed: ${changed.sorted()}. " +
                    "Run ./gradlew :backend:generateFrontendApi and review the generated diff."
            )
        }
        logger.lifecycle("Generated Angular client matches the OpenAPI contract ({} files).", expected.size)
    }
}


jacoco {
    toolVersion = "0.8.13"
}

// Cover every handwritten class in both business features, including future adapters.
val businessClasses = sourceSets.main.get().output.asFileTree.matching {
    include("com/example/supermarket/catalog/**", "com/example/supermarket/checkout/**")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(businessClasses)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(businessClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}
