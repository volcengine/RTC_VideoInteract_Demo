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
package vi_service

import . "github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/inform"

const (
	OnAudienceJoinRoom     InformEvent = "viOnAudienceJoinRoom"
	OnAudienceLeaveRoom    InformEvent = "viOnAudienceLeaveRoom"
	OnFinishLive           InformEvent = "viOnFinishLive"
	OnInviteInteract       InformEvent = "viOnInviteInteract"
	OnApplyInteract        InformEvent = "viOnApplyInteract"
	OnInviteResult         InformEvent = "viOnInviteResult"
	OnJoinInteract         InformEvent = "viOnJoinInteract"
	OnFinishInteract       InformEvent = "viOnFinishInteract"
	OnMessage              InformEvent = "viOnMessage"
	OnMediaStatusChange    InformEvent = "viOnMediaStatusChange"
	OnMediaOperate         InformEvent = "viOnMediaOperate"
	OnSeatStatusChange     InformEvent = "viOnSeatStatusChange"
	OnClearUser            InformEvent = "viOnClearUser"
	OnCloseChatRoom        InformEvent = "viOnCloseChatRoom"
	OnAnchorInvite         InformEvent = "viOnAnchorInvite"
	OnAnchorReply          InformEvent = "viOnAnchorReply"
	OnNewAnchorJoin        InformEvent = "viOnNewAnchorJoin"
	OnAnchorInteractFinish InformEvent = "viOnAnchorInteractFinish"
	OnManageOtherAnchor    InformEvent = "viOnManageOtherAnchor"
)

type InformGeneral struct {
	RoomID   string `json:"room_id,omitempty"`
	UserID   string `json:"user_id,omitempty"`
	UserName string `json:"user_name,omitempty"`
}

type InformJoinRoom struct {
	UserInfo      *User `json:"user_info"`
	AudienceCount int   `json:"audience_count"`
}

type InformLeaveRoom struct {
	UserInfo      *User `json:"user_info"`
	AudienceCount int   `json:"audience_count"`
}

type InformFinishLive struct {
	RoomID string `json:"room_id"`
	Type   int    `json:"type"`
}

type InformInviteInteract struct {
	HostInfo *User `json:"host_info"`
	SeatID   int   `json:"seat_id"`
}

type InformApplyInteract struct {
	UserInfo *User `json:"user_info"`
	SeatID   int   `json:"seat_id"`
}

type InformInviteResult struct {
	UserInfo *User `json:"user_info"`
	Reply    int   `json:"reply"`
}

type InformJoinInteract struct {
	UserInfo *User `json:"user_info"`
	SeatID   int   `json:"seat_id"`
}

type InformFinishInteract struct {
	UserInfo *User `json:"user_info"`
	SeatID   int   `json:"seat_id"`
	Type     int   `json:"type"`
}

type InformMessage struct {
	UserInfo *User  `json:"user_info"`
	Message  string `json:"message"`
}

type InformUpdateMediaStatus struct {
	UserInfo            *User `json:"user_info"`
	SeatID              int   `json:"seat_id"`
	Mic                 int   `json:"mic"`
	Camera              int   `json:"camera"`
	AudioStatusThisRoom int64 `json:"audio_status_this_room"`
}

type InformMediaOperate struct {
	Mic    int `json:"mic"`
	Camera int `json:"camera"`
}

type InformSeatStatusChange struct {
	SeatID int `json:"seat_id"`
	Type   int `json:"type"`
}

type InformCloseChatRoom struct {
	RoomID string `json:"room_id"`
}

type InformAnchorInvite struct {
	FromRoomID   string `json:"from_room_id"`
	FromUserID   string `json:"from_user_id"`
	FromUserName string `json:"from_user_name"`
}

type InteractInfo struct {
	*User
	Token string `json:"token"`
}

type InformAnchorReply struct {
	ToRoomID     string        `json:"to_room_id"`
	ToUserID     string        `json:"to_user_id"`
	ToUserName   string        `json:"to_user_name"`
	Reply        int           `json:"reply"`
	InteractInfo *InteractInfo `json:"interact_info"`
}

type InformNewAnchorJoin struct {
	*User
}

type InformAnchorInteractFinish struct {
}

type InformManageOtherAnchor struct {
	RoomID            string `json:"room_id"`
	OtherAnchorUserID string `json:"other_anchor_user_id"`
	Type              int    `json:"type"`
}
