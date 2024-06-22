plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addLast("--enable-preview")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    jvmArgs("--enable-preview")
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

jmh {
    jvmArgs.set(
        jvmArgs.get() + setOf(
            "--enable-preview",
            "-XX:+UnlockExperimentalVMOptions",
            "-Xms4G",
            "-Xmx4G",
            "-XX:+AlwaysPreTouch",
	    "-Djava.lang.ScopedValue.cacheSize=16"
        )
    )
    resultFormat.set("CSV")
    // humanOutputFile.set(file("$buildDir/reports/jmh/human.txt"))
    // forceGC.set(true)
    profilers.set(
        listOf("gc")
    )
}
