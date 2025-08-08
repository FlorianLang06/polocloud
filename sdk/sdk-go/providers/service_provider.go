package providers;

import (
	"context"

	pb "polocloud/sdk/gen/polocloud/v1/proto"
	grpc "google.golang.org/grpc"
)

type ServiceProvider struct {
	client	pb.ServiceControllerClient
}

func NewServiceProvider(conn *grpc.ClientConn) ServiceProvider {
	return ServiceProvider{client: pb.NewServiceControllerClient(conn)}
}

func (provider ServiceProvider) FindAll() ([]*pb.ServiceSnapshot, error) {
	result, err := provider.client.Find(context.Background(), &pb.ServiceFindRequest{})
	if err != nil {
		return nil, err
	}

	return result.Services, nil
}

func (provider ServiceProvider) FindByName(name *string) (*pb.ServiceSnapshot, error) {
	result, err := provider.client.Find(context.Background(), &pb.ServiceFindRequest{
		Name: name,
	})
	if err != nil {
		return nil, err
	}

	if len(result.Services) < 1 {
		return nil, nil
	}

	return result.Services[0], nil
}


func (provider ServiceProvider) FindByGroupName(groupName *string) (*pb.ServiceSnapshot, error) {
	result, err := provider.client.Find(context.Background(), &pb.ServiceFindRequest{
		GroupName: groupName,
	})
	if err != nil {
		return nil, err
	}

	if len(result.Services) < 1 {
		return nil, nil
	}

	return result.Services[0], nil
}