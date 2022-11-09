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

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type inviteAnchorReq struct {
	InviterRoomID string `json:"inviter_room_id"`
	InviterUserID string `json:"inviter_user_id"`
	InviteeRoomID string `json:"invitee_room_id"`
	InviteeUserID string `json:"invitee_user_id"`
	LoginToken    string `json:"login_token"`
}

type inviteAnchorResp struct {
}

func (eh *EventHandler) InviteAnchor(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {

	logs.CtxInfo(ctx, "viAgreeApply param:%+v", param)
	var p inviteAnchorReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	//校验参数
	if p.InviterRoomID == "" || p.InviterUserID == "" || p.InviteeRoomID == "" || p.InviteeUserID == "" {
		logs.CtxError(ctx, "input error, param:%v", p)
		return nil, custom_error.ErrInput
	}

	//是否是主播校验
	roomFactory := vi_service.GetRoomFactory()
	inviterRoom, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, p.InviterRoomID)
	if err != nil || inviterRoom == nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, custom_error.ErrRoomNotExist
	}
	if inviterRoom.GetHostUserID() != p.InviterUserID {
		logs.CtxError(ctx, "user is not host of room")
		return nil, custom_error.ErrUserIsNotOwner
	}
	inviteeRoom, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, p.InviteeRoomID)
	if err != nil || inviteeRoom == nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, custom_error.ErrRoomNotExist
	}
	if inviteeRoom.GetHostUserID() != p.InviteeUserID {
		logs.CtxError(ctx, "user is not host of room")
		return nil, custom_error.ErrUserIsNotOwner
	}

	//房间状态判断
	if !inviterRoom.EnableAnchorInteract() {
		logs.CtxError(ctx, "room is not enable anchor interact")
		return nil, custom_error.ErrRoomStatusNotMatchAction
	}
	if !inviteeRoom.EnableAnchorInteract() {
		logs.CtxError(ctx, "room is not enable anchor interact")
		return nil, custom_error.ErrRoomStatusNotMatchAction

	}

	interactService := vi_service.GetInteractService()

	err = interactService.InviteAnchor(ctx, param.AppID, p.InviterRoomID, p.InviterUserID, p.InviteeRoomID, p.InviteeUserID, param.RequestID)
	interactService.SetInviteReq(ctx, param.AppID, p.InviterRoomID, p.InviterUserID, p.InviteeRoomID, p.InviteeUserID, param.RequestID)
	if err != nil {
		logs.CtxError(ctx, "invite anchor failed,error:%s", err)
		return nil, err
	}

	return nil, nil
}
