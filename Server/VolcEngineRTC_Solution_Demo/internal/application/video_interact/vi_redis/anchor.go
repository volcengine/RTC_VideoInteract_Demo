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
	"github.com/go-redis/redis/v8"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/redis_cli"
)

const (
	roomAnchorStatusPrefix = "vi:room_anchor:"
)

const (
	fieldAudio = "field_audio"
)

func MuteRoomAnchor(ctx context.Context, roomID, userID string) error {
	err := redis_cli.Client.HSet(ctx, roomAnchorKey(roomID, userID), fieldAudio, 0).Err()
	if err != nil {
		return custom_error.InternalError(err)
	}
	return nil
}

func UnmuteRoomAnchor(ctx context.Context, roomID, userID string) error {
	err := redis_cli.Client.HSet(ctx, roomAnchorKey(roomID, userID), fieldAudio, 1).Err()
	if err != nil {
		return custom_error.InternalError(err)
	}
	return nil
}

func GetRoomAnchorAudio(ctx context.Context, roomID, userID string) (int64, error) {
	res, err := redis_cli.Client.HGet(ctx, roomAnchorKey(roomID, userID), fieldAudio).Int64()
	if err != nil {
		//没有或出错默认unmute
		if err == redis.Nil {
			return 1, nil
		}
		return 1, custom_error.InternalError(err)
	}
	return res, nil
}

func roomAnchorKey(roomID, userID string) string {
	return roomAnchorStatusPrefix + roomID + userID
}
