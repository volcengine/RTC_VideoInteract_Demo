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
package vi_handler

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/inform"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_redis"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type updateMediaStatusReq struct {
	RoomID     string `json:"room_id"`
	UserID     string `json:"user_id"`
	Mic        int    `json:"mic"`
	Camera     int    `json:"camera"`
	LoginToken string `json:"login_token"`
}

type updateMediaStatusResp struct {
}

func (eh *EventHandler) UpdateMediaStatus(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "liveCreateLive param:%+v", param)
	var p updateMediaStatusReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	roomFactory := vi_service.GetRoomFactory()
	room, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, p.RoomID)
	if err != nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, err
	}
	if room == nil {
		logs.CtxError(ctx, "room is not exist")
		return nil, err
	}

	userFactory := vi_service.GetUserFactory()
	user, err := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, p.RoomID, p.UserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return nil, err
	}
	if user == nil {
		logs.CtxError(ctx, "user is not exist,error:%s", err)
		return nil, custom_error.ErrUserNotExist
	}

	if p.Mic == 0 {
		user.MuteMic()
	} else {
		user.UnmuteMic()
	}
	if p.Camera == 0 {
		user.MuteCamera()
	} else {
		user.UnmuteCamera()
	}
	err = userFactory.Save(ctx, user)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return nil, err
	}

	informer := inform.GetInformService(param.AppID)
	data := &vi_service.InformUpdateMediaStatus{
		UserInfo: user,
		SeatID:   user.GetSeatID(),
		Mic:      user.Mic,
		Camera:   user.Camera,
	}

	//pk时，下发到两个房间
	if room.IsAnchorInteract() && room.GetHostUserID() == p.UserID {
		relationFactory := vi_service.GetRelationFactory()
		relations, err := relationFactory.GetRelationsByUser(ctx, room.GetRoomID(), room.GetHostUserID())
		if err != nil {
			return nil, err
		}
		if len(relations) != 1 {
			return nil, custom_error.InternalError(errors.New("relations count error"))
		}
		relation := relations[0]

		audioStatusThisRoom, _ := vi_redis.GetRoomAnchorAudio(ctx, relation.FromRoomID, p.UserID)
		data.AudioStatusThisRoom = audioStatusThisRoom
		informer.BroadcastRoom(ctx, relation.FromRoomID, vi_service.OnMediaStatusChange, data)
		audioStatusThisRoom, _ = vi_redis.GetRoomAnchorAudio(ctx, relation.ToRoomID, p.UserID)
		data.AudioStatusThisRoom = audioStatusThisRoom
		informer.BroadcastRoom(ctx, relation.ToRoomID, vi_service.OnMediaStatusChange, data)
		return nil, nil
	}

	informer.BroadcastRoom(ctx, p.RoomID, vi_service.OnMediaStatusChange, data)

	return nil, nil

}
