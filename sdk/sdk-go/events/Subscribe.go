package events

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"reflect"

	pb "github.com/httpmarco/polocloud/sdk/sdk-go/gen/polocloud/v1/proto"
	providers "github.com/httpmarco/polocloud/sdk/sdk-go/providers"
)

func Subscribe[T any](ep *providers.EventProvider, callback func(T)) error {
	eventName := getEventName[T]()

	request := &pb.EventSubscribeRequest{
		ServiceName: ep.ServiceName(),
		EventName:   eventName,
	}

	// gRPC Stream erstellen
	stream, err := ep.Client().Subscribe(context.Background(), request)
	if err != nil {
		return fmt.Errorf("error subscribing to event: %w", err)
	}

	// Goroutine für Stream Processing
	go func() {
		defer func() {
			if r := recover(); r != nil {
				log.Printf("Recovered from panic in event stream: %v", r)
			}
		}()

		for {
			eventContext, err := stream.Recv()
			if err != nil {
				if err == io.EOF {
					log.Println("Event stream ended")
					return
				}
				log.Printf("Error while receiving event: %v", err)
				return
			}

			fmt.Println(eventContext.EventData)
			// JSON deserialisieren
			var event T
			if err := json.Unmarshal([]byte(eventContext.EventData), &event); err != nil {
				log.Printf("Error deserializing event: %v", err)
				continue
			}

			// Callback aufrufen
			callback(event)
		}
	}()

	return nil
}

func getEventName[T any]() string {
	var zero *T
	t := reflect.TypeOf(zero).Elem()
	return t.Name()
}
