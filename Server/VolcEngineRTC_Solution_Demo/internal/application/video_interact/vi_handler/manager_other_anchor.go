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
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/inform"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_redis"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

const (
	manageTypeMute   = 0
	manageTypeUnmute = 1
)

type manageOtherAnchorReq struct {
	RoomID            string `json:"room_id"`
	UserID            string `json:"user_id"`
	OtherAnchorUserID string `json:"other_anchor_user_id"`
	Type              int    `json:"type"`
	LoginToken        string `json:"login_token"`
}

type manageOtherAnchorResp struct {
}

func (eh *EventHandler) ManageOtherAnchor(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {

	logs.CtxInfo(ctx, "liveCreateLive param:%+v", param)
	var p manageOtherAnchorReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	//校验参数
	if p.RoomID == "" || p.UserID == "" || p.OtherAnchorUserID == "" {
		logs.CtxError(ctx, "input error, param:%v", p)
		return nil, custom_error.ErrInput
	}

	//是否是主播校验
	roomFactory := vi_service.GetRoomFactory()
	room, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, p.RoomID)
	if err != nil || room == nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, custom_error.ErrRoomNotExist
	}
	if room.GetHostUserID() != p.UserID {
		logs.CtxError(ctx, "user is not host of room")
		return nil, custom_error.ErrUserIsNotOwner
	}

	informer := inform.GetInformService(param.AppID)
	if p.Type == manageTypeMute {
		err = vi_redis.MuteRoomAnchor(ctx, p.RoomID, p.OtherAnchorUserID)
		if err != nil {
			logs.CtxError(ctx, "mute room anchor failed,error:%s", err)
			return nil, err
		}
	} else if p.Type == manageTypeUnmute {
		err = vi_redis.UnmuteRoomAnchor(ctx, p.RoomID, p.OtherAnchorUserID)
		if err != nil {
			logs.CtxError(ctx, "mute room anchor failed,error:%s", err)
			return nil, err
		}
	}

	informer.BroadcastRoom(ctx, p.RoomID, vi_service.OnManageOtherAnchor, &vi_service.InformManageOtherAnchor{
		RoomID:            p.RoomID,
		OtherAnchorUserID: p.OtherAnchorUserID,
		Type:              p.Type,
	})

	return nil, nil
}
