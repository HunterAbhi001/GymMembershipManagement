// build.gradle.kts (project root)
plugins {
    // intentionally empty
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
