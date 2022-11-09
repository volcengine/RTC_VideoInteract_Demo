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

import (
	"time"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_db"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_entity"
)

type User struct {
	*vi_entity.ViUser
	isDirty bool
}

func (u *User) IsDirty() bool {
	return u.isDirty
}

func (u *User) GetRoomID() string {
	return u.RoomID
}

func (u *User) GetUserID() string {
	return u.UserID
}

func (u *User) GetUserName() string {
	return u.UserName
}

func (u *User) GetInteractStatus() int {
	return u.InteractStatus
}

func (u *User) GetSeatID() int {
	return u.SeatID
}

func (u *User) IsEnableInvite() bool {
	return u.NetStatus == vi_db.UserNetStatusOnline && u.InteractStatus == vi_db.UserInteractStatusNormal && u.SeatID == 0
}

func (u *User) IsEnableApply() bool {
	return u.NetStatus == vi_db.UserNetStatusOnline && u.InteractStatus == vi_db.UserInteractStatusNormal && u.SeatID == 0
}

func (u *User) IsEnableInteract() bool {
	return u.NetStatus == vi_db.UserNetStatusOnline && u.InteractStatus == vi_db.UserInteractStatusInviting || u.InteractStatus == vi_db.UserInteractStatusApplying
}

func (u *User) IsInteract() bool {
	return u.InteractStatus == vi_db.UserInteractStatusInteracting
}

func (u *User) IsReconnecting() bool {
	return u.NetStatus == vi_db.UserNetStatusReconnecting
}

func (u *User) IsHost() bool {
	return u.UserRole == vi_db.UserRoleHost
}

func (u *User) IsAudience() bool {
	return u.UserRole == vi_db.UserRoleAudience
}

func (u *User) StartLive() {
	u.NetStatus = vi_db.UserNetStatusOnline
	u.JoinTime = time.Now().UnixNano() / 1e6
	u.isDirty = true
}

func (u *User) JoinRoom(roomID string) {
	u.RoomID = roomID
	u.NetStatus = vi_db.UserNetStatusOnline
	u.JoinTime = time.Now().UnixNano() / 1e6
	u.isDirty = true
}

func (u *User) LeaveRoom() {
	u.NetStatus = vi_db.UserNetStatusOffline
	u.InteractStatus = vi_db.UserInteractStatusNormal
	u.SeatID = 0
	u.LeaveTime = time.Now().UnixNano() / 1e6
	u.isDirty = true
}

func (u *User) SetInteract(interactStatus, seatID int) {
	u.InteractStatus = interactStatus
	u.SeatID = seatID
	u.isDirty = true
}

func (u *User) Disconnect() {
	u.NetStatus = vi_db.UserNetStatusReconnecting
	u.isDirty = true
}

func (u *User) Reconnect(deviceID string) {
	u.NetStatus = vi_db.UserNetStatusOnline
	u.DeviceID = deviceID
	u.isDirty = true
}

func (u *User) SetUpdateTime(time time.Time) {
	u.UpdateTime = time
	u.isDirty = true
}

func (u *User) SetIsDirty(status bool) {
	u.isDirty = status
}

func (u *User) MuteMic() {
	u.Mic = 0
	u.isDirty = true
}

func (u *User) UnmuteMic() {
	u.Mic = 1
	u.isDirty = true
}

func (u *User) MuteCamera() {
	u.Camera = 0
	u.isDirty = true
}

func (u *User) UnmuteCamera() {
	u.Camera = 1
	u.isDirty = true
}
