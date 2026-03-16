package de.polocloud.node.launch

fun main(args: Array<String>) {
    val launch = NodeLaunch(args).run()

    launch.start()

    // Block main thread until shutdown
    Thread.currentThread().join()
}