package dev.httpmarco.polocloud.node

import java.nio.file.Path

val LOCAL_PATH: Path = Path.of("local")

val LOCAL_DATA_PATH: Path = LOCAL_PATH.resolve("data")

val LOCAL_NODE_PATH: Path = Path.of("local-node.json")

val LOCAL_SECURITY_PATH: Path = LOCAL_DATA_PATH.resolve(".security")