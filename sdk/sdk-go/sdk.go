package sdk

import (
	grpc "google.golang.org/grpc"
	providers "polocloud/sdk/providers"
)

type Polocloud struct {
	conn			*grpc.ClientConn
	groupProvider	providers.GroupProvider
}

func CreatePolocloudClient() (Polocloud, error) {
	var opts []grpc.DialOption
	conn, err := grpc.NewClient("http://127.0.0.1:8932", opts...)

	groupProvider := providers.NewGroupProvider(conn)

	return Polocloud { conn, groupProvider }, err
}

