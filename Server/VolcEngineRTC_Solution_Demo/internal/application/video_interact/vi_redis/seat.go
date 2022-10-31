/*
 * Copyright 2022 CloudWeGo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vi_redis

import (
	"context"
	"encoding/json"
	"github.com/go-redis/redis/v8"
	"strconv"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_entity"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/redis_cli"
)

const (
	keyRoomSeatPrefix = "vi:vccontrol:room_seat:"
)

type RedisSeatRepo struct{}

func (repo *RedisSeatRepo) Save(ctx context.Context, seat *vi_entity.ViSeat) error {
	data, _ := json.Marshal(seat)
	err := redis_cli.Client.HSet(ctx, roomSeatKey(seat.RoomID), strconv.Itoa(seat.SeatID), data).Err()
	if err != nil {
		return err
	}
	redis_cli.Client.Expire(ctx, roomSeatKey(seat.RoomID), expireTime)
	return nil
}

func (repo *RedisSeatRepo) GetSeatByRoomIDSeatID(ctx context.Context, roomID string, seatID int) (*vi_entity.ViSeat, error) {
	data, err := redis_cli.Client.HGet(ctx, roomSeatKey(roomID), strconv.Itoa(seatID)).Result()
	if err != nil {
		if err == redis.Nil {
			return nil, nil
		}
		return nil, err
	}
	rs := &vi_entity.ViSeat{}
	err = json.Unmarshal([]byte(data), rs)
	if err != nil {
		return nil, err
	}
	return rs, nil
}

func (repo *RedisSeatRepo) GetSeatsByRoomID(ctx context.Context, roomID string) ([]*vi_entity.ViSeat, error) {
	dataMap, err := redis_cli.Client.HGetAll(ctx, roomSeatKey(roomID)).Result()
	if err != nil {
		if err == redis.Nil {
			return nil, nil
		}
		return nil, err
	}

	res := make([]*vi_entity.ViSeat, 0)
	for _, data := range dataMap {
		rs := &vi_entity.ViSeat{}
		err = json.Unmarshal([]byte(data), rs)
		if err != nil {
			return nil, err
		}
		res = append(res, rs)
	}
	return res, nil
}

func roomSeatKey(roomID string) string {
	return keyRoomSeatPrefix + roomID
}
