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
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/config"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_redis"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type joinLiveRoomReq struct {
	UserID     string `json:"user_id"`
	UserName   string `json:"user_name"`
	RoomID     string `json:"room_id"`
	LoginToken string `json:"login_token"`
}

type joinLiveRoomResp struct {
	RoomInfo      *vi_service.Room             `json:"room_info"`
	UserInfo      *vi_service.User             `json:"user_info"`
	HostInfo      *vi_service.User             `json:"host_info"`
	SeatList      map[int]*vi_service.SeatInfo `json:"seat_list"`
	RtcToken      string                       `json:"rtc_token"`
	AudienceCount int                          `json:"audience_count"`
	AnchorList    []*anchor                    `json:"anchor_list"`
}

type anchor struct {
	*vi_service.User
	AudioStatusThisRoom int64 `json:"audio_status_this_room"`
}

func (eh *EventHandler) JoinLiveRoom(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "viJoinLiveRoom param:%+v", param)
	var p joinLiveRoomReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	//校验参数
	if p.UserID == "" || p.UserName == "" || p.RoomID == "" {
		logs.CtxError(ctx, "input error, param:%v", p)
		return nil, custom_error.ErrInput
	}

	roomFactory := vi_service.GetRoomFactory()
	userFactory := vi_service.GetUserFactory()
	seatFactory := vi_service.GetSeatFactory()

	roomService := vi_service.GetRoomService()
	err = roomService.JoinRoom(ctx, param.AppID, p.RoomID, p.UserID, p.UserName, param.DeviceID)
	if err != nil {
		logs.CtxError(ctx, "join room failed,error:%s", err)
		return nil, err
	}

	room, err := roomFactory.GetRoomByRoomID(ctx, param.AppID, p.RoomID)
	if err != nil {
		logs.CtxError(ctx, "get room failed,error:%s", err)
		return nil, err
	}
	if room == nil {
		logs.CtxError(ctx, "room is not exist")
		return nil, custom_error.ErrRoomNotExist
	}
	host, _ := userFactory.GetUserByRoomIDUserID(ctx, param.AppID, room.GetRoomID(), room.GetHostUserID())
	user, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, p.RoomID, p.UserID)

	seats, _ := seatFactory.GetSeatsByRoomID(ctx, p.RoomID)
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

	userCountMap, err := roomFactory.GetRoomsAudienceCount(ctx, []string{room.GetRoomID()})
	if err != nil {
		logs.CtxError(ctx, "get user count failed,error:%s")
	}
	userCount := userCountMap[room.GetRoomID()]

	anchorList := make([]*anchor, 0)
	if room.IsAnchorInteract() {
		relationFactory := vi_service.GetRelationFactory()
		relations, err := relationFactory.GetRelationsByUser(ctx, room.GetRoomID(), room.GetHostUserID())
		if err != nil {
			return nil, err
		}
		if len(relations) != 1 {
			return nil, custom_error.InternalError(errors.New("relations count error"))
		}
		relation := relations[0]

		audioStatusThisRoom, _ := vi_redis.GetRoomAnchorAudio(ctx, p.RoomID, relation.FromUserID)
		fromUser, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, relation.FromRoomID, relation.FromUserID)
		anchorList = append(anchorList, &anchor{
			User:                fromUser,
			AudioStatusThisRoom: audioStatusThisRoom,
		})
		toUser, _ := userFactory.GetActiveUserByRoomIDUserID(ctx, param.AppID, relation.ToRoomID, relation.ToUserID)

		audioStatusThisRoom, _ = vi_redis.GetRoomAnchorAudio(ctx, p.RoomID, relation.ToUserID)
		anchorList = append(anchorList, &anchor{
			User:                toUser,
			AudioStatusThisRoom: audioStatusThisRoom,
		})
	}

	resp = &joinLiveRoomResp{
		RoomInfo:      room,
		HostInfo:      host,
		UserInfo:      user,
		SeatList:      seatList,
		RtcToken:      room.GenerateToken(ctx, p.UserID, config.Configs().ViAppID, config.Configs().ViAppKey),
		AudienceCount: userCount,
		AnchorList:    anchorList,
	}

	return resp, nil
}
