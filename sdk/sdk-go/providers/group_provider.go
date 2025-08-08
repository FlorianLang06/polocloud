package providers;

import (
	"context"

	pb "polocloud/sdk/gen/polocloud/v1/proto"
	grpc "google.golang.org/grpc"
)

type GroupProvider struct {
	client	pb.GroupControllerClient
}

func NewGroupProvider(conn *grpc.ClientConn) GroupProvider {
	return GroupProvider{client: pb.NewGroupControllerClient(conn)}
}

func (provider GroupProvider) FindAll() ([]*pb.GroupSnapshot, error) {
	result, err := provider.client.Find(context.Background(), &pb.FindGroupRequest{})
	if err != nil {
		return nil, err
	}

	return result.Groups, nil
}

func (provider GroupProvider) FindByName(name string) ([]*pb.GroupSnapshot, error) {
	result, err := provider.client.Find(context.Background(), &pb.FindGroupRequest{
		Name: name,
	})
	if err != nil {
		return nil, err
	}

	return result.Groups, nil
}