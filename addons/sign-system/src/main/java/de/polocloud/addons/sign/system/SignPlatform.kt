package de.polocloud.addons.sign.system

abstract class SignPlatform {

    abstract fun listSignTypes(): Set<String>

    abstract fun displaySign(data: SignData)

}