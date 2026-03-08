val major    = findProperty("polocloud.version.major")   as String? ?: "0"
val minor    = findProperty("polocloud.version.minor")   as String? ?: "0"
val patch    = findProperty("polocloud.version.patch")   as String? ?: "0"
val channel  = (findProperty("polocloud.version.channel") as String? ?: "SNAPSHOT").uppercase()
val build    = findProperty("polocloud.version.build")   as String? ?: "local"

val versionString = when (channel) {
    "RELEASE" -> "$major.$minor.$patch"
    else      -> "$major.$minor.$patch-${channel.lowercase()}.$build"
}

version = versionString

subprojects {
    tasks.withType<ProcessResources> {

        filesMatching("**/version.properties") {

            val gitProps = project(":common").extensions.extraProperties["gitProps"] as Map<String, String>

            expand(
                "major"     to major,
                "minor"     to minor,
                "patch"     to patch,
                "channel"   to channel,
                "build"     to build,
                "buildTime" to System.currentTimeMillis().toString(),
                "commitId" to (gitProps["git.commit.id"] ?: "unknown"),
                "commitIdAbbrev" to (gitProps["git.commit.id.abbrev"] ?: "unknown")
            )
        }
    }
}