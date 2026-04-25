package de.polocloud.services.sdk

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BootMethod(val priority: Int = 0) {
}
