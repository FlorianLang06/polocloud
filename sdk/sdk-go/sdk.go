package sdk

import (
	"os"

	providers "github.com/httpmarco/polocloud/sdk/sdk-go/providers"
	grpc "google.golang.org/grpc"
	insecure "google.golang.org/grpc/credentials/insecure"
)

type Polocloud struct {
	conn            *grpc.ClientConn
	groupProvider   providers.GroupProvider
	serviceProvider providers.ServiceProvider
	playerProvider  providers.PlayerProvider
	eventProvider   providers.EventProvider
}

func (p Polocloud) GroupProvider() *providers.GroupProvider {
	return &p.groupProvider
}

func (p Polocloud) ServiceProvider() *providers.ServiceProvider {
	return &p.serviceProvider
}

func (p Polocloud) PlayerProvider() *providers.PlayerProvider {
	return &p.playerProvider
}

func (p Polocloud) EventProvider() *providers.EventProvider {
	return &p.eventProvider
}

func CreatePolocloudClient() (Polocloud, error) {
	port := getEnvOrDefault("agent_port", "8932")

	conn, err := grpc.NewClient("127.0.0.1:"+port, grpc.WithTransportCredentials(insecure.NewCredentials()))

	groupProvider := providers.NewGroupProvider(conn)
	serviceProvider := providers.NewServiceProvider(conn)
	playerProvider := providers.NewPlayerProvider(conn)
	eventProvider := providers.NewEventProvider(conn, getEnvOrDefault("service-name", "default-service"))

	return Polocloud{conn, groupProvider, serviceProvider, playerProvider, eventProvider}, err
}

func getEnvOrDefault(key string, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}

	return value
}
