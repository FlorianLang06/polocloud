package providers

import (
	pb "github.com/httpmarco/polocloud/sdk/sdk-go/gen/polocloud/v1/proto"
	grpc "google.golang.org/grpc"
)

type EventProvider struct {
	client      pb.EventProviderClient
	serviceName string
}

func NewEventProvider(conn *grpc.ClientConn, serviceName string) EventProvider {
	return EventProvider{client: pb.NewEventProviderClient(conn), serviceName: serviceName}
}

func (ep EventProvider) Client() pb.EventProviderClient {
	return ep.client
}

func (ep EventProvider) ServiceName() string {
	return ep.serviceName
}
