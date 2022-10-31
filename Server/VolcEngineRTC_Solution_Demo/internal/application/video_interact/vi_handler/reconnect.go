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
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_redis"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/config"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/token"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/login/login_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type reconnectReq struct {
	LoginToken string `json:"login_token"`
}

type reconnectResp struct {
	RoomInfo         *vi_service.Room             `json:"room_info"`
	UserInfo         *vi_service.User             `json:"user_info"`
	HostInfo         *vi_service.User             `json:"host_info"`
	SeatList         map[int]*vi_service.SeatInfo `json:"seat_list"`
	RtcToken         string                       `json:"rtc_token"`
	AnchorList       []*anchor                    `json:"anchor_list"`
	InteractInfoList []*vi_service.InteractInfo   `json:"interact_info_list"`
}

func (eh *EventHandler) Reconnect(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "viReconect param:%+v", param)
	var p reconnectReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	loginUserService := login_service.GetUserService()
	userID := loginUserService.GetUserID(ctx, p.LoginToken)

	roomFactory := vi_service.GetRoomFactory()
	userFactory := vi_service.GetUserFactory()
	seatFactory := vi_service.GetSeatFactory()

	user, err := userFactory.GetActiveUserByUserID(ctx, param.AppID, userID)
	if err != nil || user == nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return nil, custom_error.ErrUserIsInactive
	}

	user.Reconnect(param.DeviceID)
	err = userFactory.Save(ctx, user)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s")
		return nil, err
	}

	room, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, user.GetRoomID())
	if err != nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, err
	}
	if room == nil {
		logs.CtxError(ctx, "room is not exist")
		return nil, custom_error.ErrRoomNotExist
	}
	host, err := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, room.GetRoomID(), room.GetHostUserID())
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
	}

	seats, err := seatFactory.GetSeatsByRoomID(ctx, user.GetRoomID())
	if err != nil {
		logs.CtxError(ctx, "get seats failed,error:%s", err)
	}
	seatList := make(map[int]*vi_service.SeatInfo)
	for _, seat := range seats {
		if seat.GetOwnerUserID() != "" {
			u, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, seat.GetRoomID(), seat.GetOwnerUserID())
			seatList[seat.SeatID] = &vi_service.SeatInfo{
				Status:    seat.Status,
				GuestInfo: u,
			}
		} else {
			seatList[seat.SeatID] = &vi_service.SeatInfo{
				Status:    seat.Status,
				GuestInfo: nil,
			}
		}
	}

	res := &reconnectResp{}
	if room.IsAnchorInteract() {
		relationFactory := vi_service.GetRelationFactory()
		relations, err := relationFactory.GetRelationsByUser(ctx, room.GetRoomID(), room.GetHostUserID())
		if err != nil {
			return nil, err
		}
		if len(relations) != 1 {
			return nil, custom_error.ErrRelationCount
		}
		relation := relations[0]
		res.AnchorList = make([]*anchor, 0)
		audioStatusThisRoom, _ := vi_redis.GetRoomAnchorAudio(ctx, relation.FromRoomID, relation.FromUserID)
		fromUser, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, relation.FromRoomID, relation.FromUserID)
		res.AnchorList = append(res.AnchorList, &anchor{
			User:                fromUser,
			AudioStatusThisRoom: audioStatusThisRoom,
		})
		toUser, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, relation.ToRoomID, relation.ToUserID)

		audioStatusThisRoom, _ = vi_redis.GetRoomAnchorAudio(ctx, relation.ToRoomID, relation.ToUserID)
		res.AnchorList = append(res.AnchorList, &anchor{
			User:                toUser,
			AudioStatusThisRoom: audioStatusThisRoom,
		})

		if user.IsHost() {
			otherRoomID := relation.FromRoomID
			otherUserID := relation.FromUserID
			if relation.FromUserID == user.GetUserID() {
				otherRoomID = relation.ToRoomID
				otherUserID = relation.ToUserID
			}
			res.InteractInfoList = make([]*vi_service.InteractInfo, 0)
			rtcToken, _ := token.GenerateToken(&token.GenerateParam{
				AppID:        config.Configs().ViAppID,
				AppKey:       config.Configs().ViAppKey,
				RoomID:       otherRoomID,
				UserID:       user.GetUserID(),
				ExpireAt:     7 * 24 * 3600,
				CanPublish:   true,
				CanSubscribe: true,
			})
			other, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, otherRoomID, otherUserID)
			data := &vi_service.InteractInfo{
				User:  other,
				Token: rtcToken,
			}
			res.InteractInfoList = append(res.InteractInfoList, data)
		}
	}
	res.RoomInfo = room
	res.HostInfo = host
	res.UserInfo = user
	res.SeatList = seatList
	res.RtcToken = room.GenerateToken(ctx, user.GetUserID(), config.Configs().ViAppID, config.Configs().ViAppKey)
	return res, nil

}
