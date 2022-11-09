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

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_db"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type finishAnchorInteractReq struct {
	RoomID     string `json:"room_id"`
	UserID     string `json:"user_id"`
	LoginToken string `json:"login_token"`
}

type finishAnchorInteractResp struct {
}

func (eh *EventHandler) FinishAnchorInteract(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "viAgreeApply param:%+v", param)
	var p finishAnchorInteractReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	//校验参数
	if p.RoomID == "" || p.UserID == "" {
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

	interactService := vi_service.GetInteractService()
	otherRoomID, err := interactService.FinishAnchor(ctx, param.AppID, p.RoomID, p.UserID)
	if err != nil {
		logs.CtxError(ctx, "invite failed,error:%s", err)
		return nil, err
	}

	room.Status = vi_db.RoomStatusStart
	roomFactory.Save(ctx, room)

	otherRoom, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, otherRoomID)
	if otherRoom != nil {
		otherRoom.Status = vi_db.RoomStatusStart
		roomFactory.Save(ctx, otherRoom)
	}

	return nil, nil
}
