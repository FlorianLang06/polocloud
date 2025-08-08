package providers;

import (
	"context"

	pb "github.com/httpmarco/polocloud/sdk/sdk-go/gen/polocloud/v1/proto"
	grpc "google.golang.org/grpc"
)

type PlayerProvider struct {
	client	pb.PlayerControllerClient
}

func NewPlayerProvider(conn *grpc.ClientConn) PlayerProvider {
	return PlayerProvider{client: pb.NewPlayerControllerClient(conn)}
}

func (provider PlayerProvider) FindAll() ([]*pb.PlayerSnapshot, error) {
	result, err := provider.client.FindAll(context.Background(), &pb.PlayerFindRequest{})
	if err != nil {
		return nil, err
	}

	return result.Players, nil
}

func (provider PlayerProvider) FindByName(name string) (*pb.PlayerSnapshot, error) {
	result, err := provider.client.FindByName(context.Background(), &pb.PlayerFindByNameRequest{
		Name: name,
	})
	if err != nil {
		return nil, err
	}

	if len(result.Players) < 1 {
		return nil, nil
	}

	return result.Players[0], nil
}

func (provider PlayerProvider) FindByService(serviceName string) ([]*pb.PlayerSnapshot, error) {
	result, err := provider.client.FindByService(context.Background(), &pb.PlayerFindByServiceRequest{
		CurrentServiceName: serviceName,
	})
	if err != nil {
		return nil, err
	}
	return result.Players, nil
}