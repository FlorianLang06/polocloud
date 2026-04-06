package de.polocloud.services.sdk.sdk

object ServiceSdkRegistry {

    private val serviceSdks = mutableListOf<ServiceSdk>()

    fun renderSdk(serviceSdk: ServiceSdk) {
        this.serviceSdks.add(serviceSdk)
    }

    fun <T : ServiceSdk> of(c: Class<T>): T {
        return this.serviceSdks.first { it.javaClass.equals(c) } as T
    }
}