package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.LocalService
import dev.httpmarco.polocloud.agent.utils.IndexDetector
import dev.httpmarco.polocloud.v1.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceBootRequest
import dev.httpmarco.polocloud.v1.services.ServiceBootResponse
import dev.httpmarco.polocloud.v1.services.ServiceBootWithConfigurationRequest
import dev.httpmarco.polocloud.v1.services.ServiceBootWithConfigurationResponse
import dev.httpmarco.polocloud.v1.services.ServiceControllerGrpc
import dev.httpmarco.polocloud.v1.services.ServiceFindRequest
import dev.httpmarco.polocloud.v1.services.ServiceFindResponse
import io.grpc.stub.StreamObserver

class ServiceGrpcService : ServiceControllerGrpc.ServiceControllerImplBase() {

    override fun find(request: ServiceFindRequest, responseObserver: StreamObserver<ServiceFindResponse>) {
        val serviceStorage = Agent.runtime.serviceStorage();
        val builder = ServiceFindResponse.newBuilder()

        if (request.hasName()) {
            builder.addServices(serviceStorage.find(request.name)?.toSnapshot())
        } else if(request.hasGroupName()) {
            serviceStorage.findByGroup(request.groupName).forEach {
                builder.addServices(it.toSnapshot())
            }
        } else {
            serviceStorage.findAll().forEach {
                if (!request.hasType() || it.type == request.type) {
                    builder.addServices(it.toSnapshot())
                }
            }
        }
        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun boot(request: ServiceBootRequest, responseObserver: StreamObserver<ServiceBootResponse>) {
        val groupStorage = Agent.runtime.groupStorage()
        val group = groupStorage.find(request.groupName)
        val builder = ServiceBootResponse.newBuilder()

        if(group == null) {
            responseObserver.onError(IllegalArgumentException("group not found: ${request.groupName}"))
            return
        }

        val index = IndexDetector.findIndex(group)
        val service = when (group.platform().type) {
            GroupType.PROXY -> LocalService(group, index, "0.0.0.0")
            else -> LocalService(group, index)
        }

        Agent.runtime.serviceStorage().deployAbstractService(service)
        Agent.runtime.factory().bootApplication(service)
        builder.setService(service.toSnapshot())

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()

    }

    override fun bootWithConfiguration(request: ServiceBootWithConfigurationRequest, responseObserver: StreamObserver<ServiceBootWithConfigurationResponse>) {
        val groupStorage = Agent.runtime.groupStorage()
        val group = groupStorage.find(request.groupName)
        val builder = ServiceBootWithConfigurationResponse.newBuilder()

        if(group == null) {
            responseObserver.onError(IllegalArgumentException("group not found: ${request.groupName}"))
            return
        }

        val index = IndexDetector.findIndex(group)
        val service = when (group.platform().type) {
            GroupType.PROXY -> LocalService(group, index, "0.0.0.0")
            else -> LocalService(group, index)
        }

        if(request.hasMinimumMemory()) {
            service.minMemory = request.minimumMemory
        }
        if(request.hasMaximumMemory()) {
            service.maxMemory = request.maximumMemory
        }

        service.templates += request.templatesList
        request.excludedTemplatesList.forEach { template ->
            if (service.templates.contains(template)) {
                service.templates -= template
            }
        }
        service.properties += request.propertiesMap

        Agent.runtime.serviceStorage().deployAbstractService(service)
        Agent.runtime.factory().bootApplication(service)
        builder.setService(service.toSnapshot())

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()

    }

}