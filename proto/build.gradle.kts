import com.google.protobuf.gradle.*

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.protobuf.plugin)
    alias(libs.plugins.polocloud.gradle.plugin)
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.kotlin.get()}"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlinVersion.get()}:jdk8@jar"
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
