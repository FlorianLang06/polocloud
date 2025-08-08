package sdk

import (
	grpc "google.golang.org/grpc"
	insecure "google.golang.org/grpc/credentials/insecure"
	providers "polocloud/sdk/providers"
)

type Polocloud struct {
	conn			*grpc.ClientConn
	GroupProvider	providers.GroupProvider
}

func CreatePolocloudClient() (Polocloud, error) {
	conn, err := grpc.NewClient("127.0.0.1:8932", grpc.WithTransportCredentials(insecure.NewCredentials()))

	groupProvider := providers.NewGroupProvider(conn)

	return Polocloud { conn, groupProvider }, err
}

