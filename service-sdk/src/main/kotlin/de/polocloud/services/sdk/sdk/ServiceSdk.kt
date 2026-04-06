package de.polocloud.services.sdk.sdk

abstract class ServiceSdk {

    init {
        ServiceSdkRegistry.renderSdk(this);
    }

    companion object {

        fun <T : ServiceSdk> of(clazz: Class<T>): T {
            return ServiceSdkRegistry.of(clazz);
        }
    }

    fun isAvailable(): Boolean {
        return true;
    }
}