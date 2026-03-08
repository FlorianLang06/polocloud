import com.google.protobuf.gradle.*

plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf.plugin)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.protobuf.kotlin)
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.netty.shaded)

    api(libs.kotlinx.coroutines.core)

    compileOnly(libs.javax.annotation.api)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        proto {
            srcDir("src/proto")
        }

        kotlin {
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/kotlin"))
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/grpckt"))
        }

        java {
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/java"))
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/grpc"))
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.kotlin.toString()
    }

    plugins {
        id("grpc") {
            artifact = libs.protoc.gen.grpc.java.toString()
        }
        id("grpckt") {
            artifact = libs.grpc.kotlin.stub.toString() + ":jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("kotlin")
            }
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
