// gradle/version.gradle.kts
// Reads version components from gradle.properties (or -P flags) and injects
// them into the classpath resource version.properties at build time.
//
// Usage:
//   Local dev:   ./gradlew build   (channel=SNAPSHOT, build=local)
//   CI Dev:      ./gradlew build -Pcloud.version.channel=DEV -Pcloud.version.build=$BUILD_NUMBER
//   Beta:        ./gradlew build -Pcloud.version.channel=BETA -Pcloud.version.build=$BUILD_NUMBER
//   Production:  ./gradlew build -Pcloud.version.channel=RELEASE

val major    = findProperty("cloud.version.major")   as String? ?: "0"
val minor    = findProperty("cloud.version.minor")   as String? ?: "0"
val patch    = findProperty("cloud.version.patch")   as String? ?: "0"
val channel  = (findProperty("cloud.version.channel") as String? ?: "SNAPSHOT").uppercase()
val build    = findProperty("cloud.version.build")   as String? ?: "local"

val versionString = when (channel) {
    "RELEASE" -> "$major.$minor.$patch"
    else      -> "$major.$minor.$patch-${channel.lowercase()}.$build"
}

version = versionString

subprojects {
    tasks.withType<ProcessResources> {
        filesMatching("**/version.properties") {
            expand(
                "major"     to major,
                "minor"     to minor,
                "patch"     to patch,
                "channel"   to channel,
                "build"     to build,
                "buildTime" to System.currentTimeMillis().toString()
            )
        }
    }
}

//TODO CI task to build polocloud
//- name: Build Dev
//        run: ./gradlew build
//        -Pcloud.version.channel=DEV
//-Pcloud.version.build=${{ github.run_number }}
//
//- name: Build Beta
//        run: ./gradlew build
//        -Pcloud.version.channel=BETA
//-Pcloud.version.build=${{ github.run_number }}
//
//- name: Build Release
//        run: ./gradlew build
//        -Pcloud.version.channel=RELEASE
//-Pcloud.version.build=${{ github.run_number }}