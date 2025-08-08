package sdk

import (
	"os"

	grpc "google.golang.org/grpc"
	insecure "google.golang.org/grpc/credentials/insecure"
	providers "polocloud/sdk/providers"
)

type Polocloud struct {
	conn			*grpc.ClientConn
	GroupProvider	providers.GroupProvider
	ServiceProvider	providers.ServiceProvider
	PlayerProvider	providers.PlayerProvider
}

func CreatePolocloudClient() (Polocloud, error) {
	port := os.Getenv("agent_port")
	if port == "" {
		port = "8932"
	}

	conn, err := grpc.NewClient("127.0.0.1:" + port, grpc.WithTransportCredentials(insecure.NewCredentials()))

	groupProvider := providers.NewGroupProvider(conn)
	serviceProvider := providers.NewServiceProvider(conn)
	playerProvider := providers.NewPlayerProvider(conn)

	return Polocloud { conn, groupProvider, serviceProvider, playerProvider }, err
}

