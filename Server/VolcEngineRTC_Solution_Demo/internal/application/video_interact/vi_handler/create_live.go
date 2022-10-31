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
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/config"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type createRoomReq struct {
	UserID              string `json:"user_id"`
	UserName            string `json:"user_name"`
	RoomName            string `json:"room_name"`
	BackgroundImageName string `json:"background_image_name"`
	LoginToken          string `json:"login_token"`
}

type createRoomResp struct {
	RoomInfo *vi_service.Room `json:"room_info"`
	UserInfo *vi_service.User `json:"user_info"`
	RtcToken string           `json:"rtc_token"`
}

func (eh *EventHandler) CreateRoom(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "viStartLive param:%+v", param)
	var p createRoomReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	//校验参数
	if p.UserID == "" || p.UserName == "" || p.RoomName == "" {
		logs.CtxError(ctx, "input error, param:%v", p)
		return nil, custom_error.ErrInput
	}

	userFactory := vi_service.GetUserFactory()
	userOld, err := userFactory.GetActiveUserByUserID(ctx, param.AppID, p.UserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return nil, err
	}
	if userOld != nil {
		logs.CtxError(ctx, "user is active")
		return nil, custom_error.ErrUserInUse
	}

	//创建房间
	roomService := vi_service.GetRoomService()

	room, host, err := roomService.CreateRoom(ctx, param.AppID, p.RoomName, p.BackgroundImageName, p.UserID, p.UserName, param.DeviceID)
	if err != nil {
		logs.CtxError(ctx, "create room failed,error:%s", err)
		return nil, err
	}

	resp = &createRoomResp{
		RoomInfo: room,
		UserInfo: host,
		RtcToken: room.GenerateToken(ctx, host.GetUserID(), config.Configs().ViAppID, config.Configs().ViAppKey),
	}

	return resp, nil
}
