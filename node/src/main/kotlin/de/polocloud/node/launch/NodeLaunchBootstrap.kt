package de.polocloud.node.launch

fun main(args: Array<String>) {
    NodeLaunch(args).run()

    // Block main thread until shutdown
    Thread.currentThread().join()
}