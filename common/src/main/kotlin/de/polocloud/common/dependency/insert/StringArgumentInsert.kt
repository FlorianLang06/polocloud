package de.polocloud.common.dependency.insert

import de.polocloud.common.dependency.Dependency

/**
 * A [DependencyInsert] that renders a [Dependency] as a file-URL string.
 *
 * The rendered string is intended for external use cases such as passing the
 * dependency path as a command-line classpath argument (`-cp`) to a child JVM
 * process. Unlike [ClasspathInsert], this implementation does **not** inject the
 * dependency into the current runtime classpath.
 *
 * Calling [connect] is therefore not supported and will throw
 * [UnsupportedOperationException]. Use [renderDependency] directly and pass the
 * resulting string to the target process.
 */
class StringArgumentInsert : DependencyInsert<String>() {

    /**
     * Converts the local cache path of [dependency] to a file-URL string.
     *
     * The path is resolved via [Dependency.localPath] and normalised to an absolute
     * filesystem path so it can be used directly in JVM `-cp` arguments.
     *
     * @param dependency the dependency whose local path should be converted
     * @return the absolute filesystem path to the dependency JAR on disk
     */
    override fun renderDependency(dependency: Dependency): String {
        return dependency.localPath().toAbsolutePath().toString()
    }

    /**
     * Not supported by this implementation.
     *
     * [StringArgumentInsert] is designed to produce a string representation of
     * the dependency for external use. Direct classpath injection is intentionally
     * omitted — use [renderDependency] and forward the string to the target process.
     *
     * @throws UnsupportedOperationException always
     */
    override fun connect(element: String): Nothing {
        throw UnsupportedOperationException(
            "StringArgumentInsert does not support connecting dependencies directly. " +
                "Use the rendered string to pass the dependency path externally (e.g., as a -cp argument)."
        )
    }
}
