package de.polocloud.node.group

import de.polocloud.proto.GroupData

/**
 * Maps between the persisted domain [Group] and its protobuf [GroupData] representation.
 */
object GroupProtoMapper {

    fun toProto(group: Group): GroupData = GroupData.newBuilder()
        .setName(group.name)
        .setMemory(group.memory)
        .setStartThreshold(group.startThreshold)
        .setMinOnline(group.minOnline)
        .setMaxOnline(group.maxOnline)
        .setPlatform(group.platform)
        .setVersion(group.version)
        .putAllProperties(group.properties)
        .build()

    fun toDomain(data: GroupData): Group = Group(
        name = data.name,
        memory = data.memory,
        startThreshold = data.startThreshold,
        minOnline = data.minOnline,
        maxOnline = data.maxOnline,
        platform = data.platform,
        version = data.version,
        propertiesJson = PropertyCodec.encode(data.propertiesMap),
    )
}
