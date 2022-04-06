plugins {
    kotlin("jvm") version "1.5.10"
    java
}

group = "com.hxl.xiaoai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
tasks.jar{
    manifest {
        attributes.set("Main-Class", "com.hxl.xiaoai.MainKt")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")


}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
tasks.withType<Jar>() {
    duplicatesStrategy=DuplicatesStrategy.EXCLUDE
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}