plugins {
    base
}

tasks.check {
    dependsOn(":backend:check")
}

tasks.assemble {
    dependsOn(":backend:bootJar")
}
