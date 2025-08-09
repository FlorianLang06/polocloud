package events

import (
	pb "github.com/httpmarco/polocloud/sdk/sdk-go/gen/polocloud/v1/proto"
)

type ServiceOnlineEvent struct {
	Service pb.ServiceSnapshot
}
