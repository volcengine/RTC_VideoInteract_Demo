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
	"context"
	"errors"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/config"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/inform"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/redis_cli"
	"strings"
	"time"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/redis_cli/lock"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/token"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_db"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

const (
	InteractReplyTypeAccept  = 1
	InteractReplyTypeReject  = 2
	InteractReplyTypeTimeout = 3
)

const (
	InteractManageTypeLockSeat   = 1
	InteractManageTypeUnlockSeat = 2
	InteractManageTypeMute       = 3
	InteractManageTypeUnmute     = 4
	InteractManageTypeKick       = 5
)

const (
	InteractFinishTypeHost = 1
	InteractFinishTypeSelf = 2
)

const (
	InviteActhorReqPrefix = "invite:req:"
)

var interactServiceClient *InteractService

type InteractService struct {
	userFactory     *UserFactory
	seatFactory     *SeatFactory
	RelationFactory *RelationFactory
}

func GetInteractService() *InteractService {
	if interactServiceClient == nil {
		interactServiceClient = &InteractService{
			userFactory:     GetUserFactory(),
			seatFactory:     GetSeatFactory(),
			RelationFactory: GetRelationFactory(),
		}
	}
	return interactServiceClient
}

func (is *InteractService) InviteAnchor(ctx context.Context, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID, reqID string) error {
	if strings.Compare(inviterRoomID, inviteeRoomID) > 0 {
		ok, lt1 := lock.LockRoom(ctx, inviterRoomID)
		if !ok {
			return custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviterRoomID, lt1)
		ok, lt2 := lock.LockRoom(ctx, inviteeRoomID)
		if !ok {
			return custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviteeRoomID, lt2)
	} else {
		ok, lt1 := lock.LockRoom(ctx, inviteeRoomID)
		if !ok {
			return custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviteeRoomID, lt1)
		ok, lt2 := lock.LockRoom(ctx, inviterRoomID)
		if !ok {
			return custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviterRoomID, lt2)
	}

	inviterRelations, err := is.RelationFactory.GetRelationsByUser(ctx, inviterRoomID, inviterUserID)
	if err != nil {
		return err
	}
	for _, relation := range inviterRelations {
		if inArray(relation.Relation, []int{vi_db.RelationStatusAnchorInvite, vi_db.RelationStatusAnchorLinked, vi_db.RelationStatusAudienceInvite, vi_db.RelationStatusAudienceLinked}) {
			return custom_error.ErrUserStatusNotMatchAction
		}
	}
	inviteeRelations, err := is.RelationFactory.GetRelationsByUser(ctx, inviteeRoomID, inviteeUserID)
	if err != nil {
		return err
	}
	for _, relation := range inviteeRelations {
		if inArray(relation.Relation, []int{vi_db.RelationStatusAnchorInvite, vi_db.RelationStatusAnchorLinked, vi_db.RelationStatusAudienceInvite, vi_db.RelationStatusAudienceLinked}) {
			return custom_error.ErrUserStatusNotMatchAction
		}
	}

	is.ClearRoom(ctx, appID, inviterRoomID)
	is.ClearRoom(ctx, appID, inviteeRoomID)

	inviter, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, inviterRoomID, inviterUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if inviter == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	inviter.SetInteract(vi_db.UserInteractStatusInviting, 0)
	err = is.userFactory.Save(ctx, inviter)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return err
	}

	invitee, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, inviteeRoomID, inviteeUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if invitee == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	invitee.SetInteract(vi_db.UserInteractStatusInviting, 0)
	err = is.userFactory.Save(ctx, invitee)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return err
	}

	relation := is.RelationFactory.NewRelation(ctx, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
	relation.SetRelationStatus(vi_db.RelationStatusAnchorInvite)
	err = is.RelationFactory.Save(ctx, relation)
	if err != nil {
		return err
	}
	logs.CtxInfo(ctx, "set relation status:%s", relation.Relation)

	//todo 加锁

	informer := inform.GetInformService(appID)
	data := &InformAnchorInvite{
		FromRoomID:   inviterRoomID,
		FromUserID:   inviterUserID,
		FromUserName: inviter.GetUserName(),
	}
	informer.UnicastUser(ctx, inviteeRoomID, inviteeUserID, OnAnchorInvite, data)

	//time out
	go func(ctx context.Context, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID string) {
		time.Sleep(5 * time.Second)
		is.ReplyAnchor(ctx, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID, InteractReplyTypeTimeout, reqID)
	}(ctx, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)

	return nil
}

func (is *InteractService) ReplyAnchor(ctx context.Context, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID string, reply int, curReqID string) (bool, error) {
	if strings.Compare(inviterRoomID, inviteeRoomID) > 0 {
		ok, lt1 := lock.LockRoom(ctx, inviterRoomID)
		if !ok {
			return false, custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviterRoomID, lt1)
		ok, lt2 := lock.LockRoom(ctx, inviteeRoomID)
		if !ok {
			return false, custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviteeRoomID, lt2)
	} else {
		ok, lt1 := lock.LockRoom(ctx, inviteeRoomID)
		if !ok {
			return false, custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviteeRoomID, lt1)
		ok, lt2 := lock.LockRoom(ctx, inviterRoomID)
		if !ok {
			return false, custom_error.ErrLockRedis
		}
		defer lock.UnLockRoom(ctx, inviterRoomID, lt2)
	}
	//if reply == InteractReplyTypeTimeout {
	//	reply = InteractReplyTypeReject
	//}

	inviter, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, inviterRoomID, inviterUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return false, err
	}
	if inviter == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return false, custom_error.ErrUserNotExist
	}

	invitee, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, inviteeRoomID, inviteeUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return false, err
	}
	if invitee == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return false, custom_error.ErrUserNotExist
	}

	infomer := inform.GetInformService(appID)

	switch reply {
	case InteractReplyTypeAccept:
		relation, err := is.RelationFactory.GetRelation(ctx, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
		if err != nil || relation == nil {
			return false, err
		}
		logs.CtxInfo(ctx, "get relation status:%s", relation.Relation)
		if relation.Relation != vi_db.RelationStatusAnchorInvite {
			logs.CtxInfo(ctx, "find err")
			return false, custom_error.ErrUserStatusNotMatchAction
		}
		relation.SetRelationStatus(vi_db.RelationStatusAnchorLinked)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return false, err
		}

		inviter.SetInteract(vi_db.UserInteractStatusInteracting, 0)
		err = is.userFactory.Save(ctx, inviter)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return false, err
		}

		invitee.SetInteract(vi_db.UserInteractStatusInteracting, 0)
		err = is.userFactory.Save(ctx, invitee)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return false, err
		}

		rtcToken, _ := token.GenerateToken(&token.GenerateParam{
			AppID:        appID,
			AppKey:       config.Configs().ViAppKey,
			RoomID:       inviteeRoomID,
			UserID:       inviterUserID,
			ExpireAt:     7 * 24 * 3600,
			CanPublish:   true,
			CanSubscribe: true,
		})
		data := &InformAnchorReply{
			ToRoomID:   inviteeRoomID,
			ToUserID:   inviteeUserID,
			ToUserName: invitee.GetUserName(),
			Reply:      reply,
			InteractInfo: &InteractInfo{
				User:  invitee,
				Token: rtcToken,
			},
		}
		infomer.UnicastUser(ctx, inviterRoomID, inviterUserID, OnAnchorReply, data)

		data1 := &InformNewAnchorJoin{
			User: inviter,
		}
		infomer.BroadcastRoom(ctx, inviteeRoomID, OnNewAnchorJoin, data1)

		data2 := &InformNewAnchorJoin{
			User: invitee,
		}
		infomer.BroadcastRoom(ctx, inviterRoomID, OnNewAnchorJoin, data2)
		return true, nil
	case InteractReplyTypeReject, InteractReplyTypeTimeout:
		relation, err := is.RelationFactory.GetRelation(ctx, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
		if err != nil || relation == nil {
			return false, err
		}
		logs.CtxInfo(ctx, "get relation status:%s", relation.Relation)
		if relation.Relation != vi_db.RelationStatusAnchorInvite {
			logs.CtxInfo(ctx, "find err")
			return false, custom_error.ErrUserStatusNotMatchAction
		}
		reqID := is.GetInviteReq(ctx, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
		if reply == InteractReplyTypeTimeout {
			if reqID != curReqID {
				logs.CtxInfo(ctx, "find err to replay")
				return false, nil
			} else {
				reply = InteractReplyTypeReject
			}
		}
		relation.SetRelationStatus(vi_db.RelationStatusNormalNothing)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return false, err
		}

		inviter.SetInteract(vi_db.UserInteractStatusNormal, 0)
		err = is.userFactory.Save(ctx, inviter)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return false, err
		}

		invitee.SetInteract(vi_db.UserInteractStatusNormal, 0)
		err = is.userFactory.Save(ctx, invitee)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return false, err
		}

		data := &InformAnchorReply{
			ToRoomID:   inviteeRoomID,
			ToUserID:   inviteeUserID,
			ToUserName: invitee.GetUserName(),
			Reply:      reply,
		}
		infomer.UnicastUser(ctx, inviterRoomID, inviterUserID, OnAnchorReply, data)

	}

	return false, nil
}

func (is *InteractService) FinishAnchor(ctx context.Context, appID, roomID, userID string) (string, error) {
	relations, err := is.RelationFactory.GetRelationsByUser(ctx, roomID, userID)
	if err != nil {
		return "", err
	}
	if len(relations) != 1 {
		return "", custom_error.InternalError(errors.New("relations count error"))
	}

	relation := relations[0]
	if relation.Relation != vi_db.RelationStatusAnchorLinked {
		return "", custom_error.ErrUserStatusNotMatchAction
	}
	relation.SetRelationStatus(vi_db.RelationStatusNormalNothing)
	err = is.RelationFactory.Save(ctx, relation)
	if err != nil {
		return "", err
	}

	inviter, err := is.userFactory.GetUserByRoomIDUserID(ctx, appID, relation.FromRoomID, relation.FromUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return "", err
	}
	if inviter == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return "", custom_error.ErrUserNotExist
	}
	inviter.SetInteract(vi_db.UserInteractStatusNormal, 0)
	err = is.userFactory.Save(ctx, inviter)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return "", err
	}

	invitee, err := is.userFactory.GetUserByRoomIDUserID(ctx, appID, relation.ToRoomID, relation.ToUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return "", err
	}
	if invitee == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return "", custom_error.ErrUserNotExist
	}
	invitee.SetInteract(vi_db.UserInteractStatusNormal, 0)
	err = is.userFactory.Save(ctx, invitee)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return "", err
	}

	informer := inform.GetInformService(appID)
	data := &InformAnchorInteractFinish{}
	informer.BroadcastRoom(ctx, inviter.GetRoomID(), OnAnchorInteractFinish, data)
	informer.BroadcastRoom(ctx, invitee.GetRoomID(), OnAnchorInteractFinish, data)

	otherRoomID := relation.FromRoomID
	if otherRoomID == roomID {
		otherRoomID = relation.ToRoomID
	}
	return otherRoomID, nil
}

func (is *InteractService) Invite(ctx context.Context, appID, roomID, hostUserID, audienceUserID string, seatID int) error {
	ok, lt := lock.LockRoom(ctx, roomID)
	if !ok {
		return custom_error.ErrLockRedis
	}
	defer lock.UnLockRoom(ctx, roomID, lt)

	hostRelations, err := is.RelationFactory.GetRelationsByUser(ctx, roomID, hostUserID)
	if err != nil {
		return err
	}
	for _, relation := range hostRelations {
		if inArray(relation.Relation, []int{vi_db.RelationStatusAnchorInvite, vi_db.RelationStatusAnchorLinked}) {
			return custom_error.ErrUserStatusNotMatchAction
		}
	}

	seats, err := is.seatFactory.GetSeatsByRoomID(ctx, roomID)
	if err != nil {
		logs.CtxError(ctx, "get seats failed,error:%s", err)
		return err
	}
	seatAvailable := false
	for _, s := range seats {
		if s.IsEnableAlloc() {
			seatAvailable = true
			break
		}
	}
	if !seatAvailable {
		return custom_error.ErrUserOnMicExceedLimit
	}

	audience, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, audienceUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if audience == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	if !audience.IsEnableInvite() {
		logs.CtxError(ctx, "user is interacting,roomID:%s,userID:%s", roomID, audienceUserID)
		return custom_error.InternalError(errors.New("user is interacting"))
	}

	audience.SetInteract(vi_db.UserInteractStatusInviting, seatID)
	err = is.userFactory.Save(ctx, audience)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return err
	}

	relation := is.RelationFactory.NewRelation(ctx, roomID, hostUserID, roomID, audienceUserID)
	relation.SetRelationStatus(vi_db.RelationStatusAudienceInvite)
	err = is.RelationFactory.Save(ctx, relation)
	if err != nil {
		return err
	}

	//inform
	informer := inform.GetInformService(appID)

	host, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, hostUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if host == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	data := &InformInviteInteract{
		HostInfo: host,
		SeatID:   seatID,
	}
	informer.UnicastUser(ctx, audience.GetRoomID(), audience.GetUserID(), OnInviteInteract, data)

	//time out
	go func(ctx context.Context, roomID, hostUserID, audienceUserID string) {
		time.Sleep(5 * time.Second)
		is.AudienceReply(ctx, appID, roomID, hostUserID, audienceUserID, InteractReplyTypeTimeout)
	}(ctx, roomID, hostUserID, audienceUserID)

	return nil
}

func (is *InteractService) Apply(ctx context.Context, appID, roomID, hostUserID, audienceUserID string, seatID int) error {
	ok, lt := lock.LockRoom(ctx, roomID)
	if !ok {
		return custom_error.ErrLockRedis
	}
	defer lock.UnLockRoom(ctx, roomID, lt)

	hostRelations, err := is.RelationFactory.GetRelationsByUser(ctx, roomID, hostUserID)
	if err != nil {
		return err
	}
	for _, relation := range hostRelations {
		if inArray(relation.Relation, []int{vi_db.RelationStatusAnchorInvite, vi_db.RelationStatusAnchorLinked}) {
			return custom_error.ErrUserStatusNotMatchAction
		}
	}

	audience, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, audienceUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if audience == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	if !audience.IsEnableApply() {
		logs.CtxError(ctx, "user is interacting,roomID:%s,userID:%s", roomID, audienceUserID)
		return custom_error.InternalError(errors.New("user is interacting"))
	}

	audience.SetInteract(vi_db.UserInteractStatusApplying, seatID)
	err = is.userFactory.Save(ctx, audience)
	if err != nil {
		logs.CtxError(ctx, "save user failed,error:%s", err)
		return err
	}

	relation := is.RelationFactory.NewRelation(ctx, roomID, audienceUserID, roomID, hostUserID)
	relation.SetRelationStatus(vi_db.RelationStatusAudienceApply)
	err = is.RelationFactory.Save(ctx, relation)
	if err != nil {
		return err
	}

	return nil
}

func (is *InteractService) AudienceReply(ctx context.Context, appID, roomID, hostUserID, audienceUserID string, replyType int) error {
	ok, lt := lock.LockRoom(ctx, roomID)
	if !ok {
		return custom_error.ErrLockRedis
	}
	defer lock.UnLockRoom(ctx, roomID, lt)
	audience, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, audienceUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if audience == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}

	if replyType == InteractReplyTypeTimeout {
		replyType = InteractReplyTypeReject
	}

	if replyType == InteractReplyTypeAccept {
		relation, err := is.RelationFactory.GetRelation(ctx, roomID, hostUserID, roomID, audienceUserID)
		if err != nil || relation == nil {
			return err
		}
		if relation.Relation != vi_db.RelationStatusAudienceInvite {
			return custom_error.ErrUserStatusNotMatchAction
		}
		relation.SetRelationStatus(vi_db.RelationStatusAudienceLinked)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return err
		}

		if !audience.IsEnableInteract() {
			logs.CtxError(ctx, "user can not interact")
			return custom_error.InternalError(errors.New("user can not interact"))
		}

		seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, audience.SeatID)
		if err != nil {
			logs.CtxError(ctx, "get seat failed,error:%s", err)
			return err
		}
		if seat == nil {
			logs.CtxError(ctx, "seat is not exist")
			return custom_error.InternalError(errors.New("seat is not exist"))
		}

		if !seat.IsEnableAlloc() {
			seat = nil
			seats, err := is.seatFactory.GetSeatsByRoomID(ctx, roomID)
			if err != nil {
				logs.CtxError(ctx, "get seats failed,error:%s", err)
				return err
			}
			for _, s := range seats {
				if s.IsEnableAlloc() {
					seat = s
					break
				}
			}
			if seat == nil {
				audience.SetInteract(vi_db.UserInteractStatusNormal, 0)
				err = is.userFactory.Save(ctx, audience)
				if err != nil {
					logs.CtxError(ctx, "save user failed,error:%s", err)
					return err
				}
				logs.CtxWarn(ctx, "no seat can be alloc")
				return custom_error.ErrUserOnMicExceedLimit
			}
		}

		audience.SetInteract(vi_db.UserInteractStatusInteracting, seat.GetSeatID())
		err = is.userFactory.Save(ctx, audience)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return err
		}
		seat.SetOwnerUserID(audience.GetUserID())
		err = is.seatFactory.Save(ctx, seat)
		if err != nil {
			logs.CtxError(ctx, "save seat failed,error:%s", err)
			return err
		}

		informer := inform.GetInformService(appID)
		data := &InformJoinInteract{
			UserInfo: audience,
			SeatID:   audience.GetSeatID(),
		}
		informer.BroadcastRoom(ctx, roomID, OnJoinInteract, data)

	} else {
		relation, err := is.RelationFactory.GetRelation(ctx, roomID, hostUserID, roomID, audienceUserID)
		if err != nil || relation == nil {
			return err
		}
		if relation.Relation != vi_db.RelationStatusAudienceInvite {
			return custom_error.ErrUserStatusNotMatchAction
		}
		relation.SetRelationStatus(vi_db.RelationStatusNormalNothing)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return err
		}
		audience.SetInteract(vi_db.UserInteractStatusNormal, 0)
		err = is.userFactory.Save(ctx, audience)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return err
		}
	}

	informer := inform.GetInformService(appID)
	host, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, hostUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if host == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}
	data := &InformInviteResult{
		UserInfo: audience,
		Reply:    replyType,
	}
	informer.UnicastUser(ctx, host.GetRoomID(), host.GetUserID(), OnInviteResult, data)

	return nil
}

func (is *InteractService) HostReply(ctx context.Context, appID, roomID, hostUserID, audienceUserID string, replyType int) error {
	ok, lt := lock.LockRoom(ctx, roomID)
	if !ok {
		return custom_error.ErrLockRedis
	}
	defer lock.UnLockRoom(ctx, roomID, lt)
	audience, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, audienceUserID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if audience == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}

	if replyType == InteractReplyTypeAccept {
		relation, err := is.RelationFactory.GetRelation(ctx, roomID, audienceUserID, roomID, hostUserID)
		if err != nil || relation == nil {
			return err
		}
		if relation.Relation != vi_db.RelationStatusAudienceApply {
			return custom_error.ErrUserStatusNotMatchAction
		}
		if !audience.IsEnableInteract() {
			logs.CtxError(ctx, "user can not interact")
			return custom_error.InternalError(errors.New("user can not interact"))
		}

		seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, audience.SeatID)
		if err != nil {
			logs.CtxError(ctx, "get seat failed,error:%s", err)
			return err
		}
		if seat == nil {
			logs.CtxError(ctx, "seat is not exist")
			return custom_error.InternalError(errors.New("seat is not exist"))
		}

		if !seat.IsEnableAlloc() {
			seat = nil
			seats, err := is.seatFactory.GetSeatsByRoomID(ctx, roomID)
			if err != nil {
				logs.CtxError(ctx, "get seats failed,error:%s", err)
				return err
			}
			for _, s := range seats {
				if s.IsEnableAlloc() {
					seat = s
					break
				}
			}
			if seat == nil {
				audience.SetInteract(vi_db.UserInteractStatusNormal, 0)
				err = is.userFactory.Save(ctx, audience)
				if err != nil {
					logs.CtxError(ctx, "save user failed,error:%s", err)
					return err
				}
				logs.CtxWarn(ctx, "no seat can be alloc")
				return custom_error.ErrUserOnMicExceedLimit
			}
		}
		relation.SetRelationStatus(vi_db.RelationStatusAudienceLinked)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return err
		}

		audience.SetInteract(vi_db.UserInteractStatusInteracting, seat.GetSeatID())
		err = is.userFactory.Save(ctx, audience)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return err
		}
		seat.SetOwnerUserID(audience.GetUserID())
		err = is.seatFactory.Save(ctx, seat)
		if err != nil {
			logs.CtxError(ctx, "save seat failed,error:%s", err)
			return err
		}

		informer := inform.GetInformService(appID)
		data := &InformJoinInteract{
			UserInfo: audience,
			SeatID:   audience.GetSeatID(),
		}
		informer.BroadcastRoom(ctx, roomID, OnJoinInteract, data)
	} else {
		relation, err := is.RelationFactory.GetRelation(ctx, roomID, hostUserID, roomID, audienceUserID)
		if err != nil || relation == nil {
			return err
		}
		if relation.Relation != vi_db.RelationStatusAudienceInvite {
			return custom_error.ErrUserStatusNotMatchAction
		}
		relation.SetRelationStatus(vi_db.RelationStatusNormalNothing)
		err = is.RelationFactory.Save(ctx, relation)
		if err != nil {
			return err
		}

		audience.SetInteract(vi_db.UserInteractStatusNormal, 0)
		err = is.userFactory.Save(ctx, audience)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return err
		}
	}

	return nil
}

func (is *InteractService) Mute(ctx context.Context, appID, roomID string, seatID int) error {
	seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, seatID)
	if err != nil {
		logs.CtxError(ctx, "get seat failed,error:%s", err)
		return err
	}
	if seat == nil {
		logs.CtxError(ctx, "seat is not exist")
		return custom_error.InternalError(errors.New("seat is not exist"))
	}

	userID := seat.GetOwnerUserID()
	if userID == "" {
		logs.CtxError(ctx, "no user in this seat")
		return custom_error.InternalError(errors.New("no user in this seat"))
	}

	user, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, userID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if user == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}

	informer := inform.GetInformService(appID)
	data := &InformMediaOperate{
		Mic:    0,
		Camera: user.Camera,
	}
	informer.UnicastUser(ctx, roomID, userID, OnMediaOperate, data)
	return nil

}

func (is *InteractService) Unmute(ctx context.Context, appID, roomID string, seatID int) error {
	seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, seatID)
	if err != nil {
		logs.CtxError(ctx, "get seat failed,error:%s", err)
		return err
	}
	if seat == nil {
		logs.CtxError(ctx, "seat is not exist")
		return custom_error.InternalError(errors.New("seat is not exist"))
	}

	userID := seat.GetOwnerUserID()
	if userID == "" {
		logs.CtxError(ctx, "no user in this seat")
		return custom_error.InternalError(errors.New("no user in this seat"))
	}

	user, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, userID)
	if err != nil {
		logs.CtxError(ctx, "get user failed,error:%s", err)
		return err
	}
	if user == nil {
		logs.CtxError(ctx, "user is not exist", err)
		return custom_error.ErrUserNotExist
	}

	informer := inform.GetInformService(appID)
	data := &InformMediaOperate{
		Mic:    1,
		Camera: user.Camera,
	}
	informer.UnicastUser(ctx, roomID, userID, OnMediaOperate, data)
	return nil
}

func (is *InteractService) LockSeat(ctx context.Context, appID, roomID string, seatID int) error {
	seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, seatID)
	if err != nil {
		logs.CtxError(ctx, "get seat failed,error:%s", err)
		return err
	}
	if seat == nil {
		logs.CtxError(ctx, "seat is not exist")
		return custom_error.InternalError(errors.New("seat is not exist"))
	}

	userID := seat.GetOwnerUserID()
	if userID != "" {
		err = is.FinishInteract(ctx, appID, roomID, seatID, InteractFinishTypeHost)
	}

	seat.Lock()
	seat.SetOwnerUserID("")
	err = is.seatFactory.Save(ctx, seat)
	if err != nil {
		logs.CtxError(ctx, "save seat failed,error:%s", err)
		return err
	}

	informer := inform.GetInformService(appID)
	data := &InformSeatStatusChange{
		SeatID: seatID,
		Type:   0,
	}
	informer.BroadcastRoom(ctx, roomID, OnSeatStatusChange, data)
	return nil

}

func (is *InteractService) UnlockSeat(ctx context.Context, appID, roomID string, seatID int) error {
	seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, seatID)
	if err != nil {
		logs.CtxError(ctx, "get seat failed,error:%s", err)
		return err
	}
	if seat == nil {
		logs.CtxError(ctx, "seat is not exist")
		return custom_error.InternalError(errors.New("seat is not exist"))
	}

	seat.Unlock()
	err = is.seatFactory.Save(ctx, seat)
	if err != nil {
		logs.CtxError(ctx, "save seat failed,error:%s", err)
		return err
	}

	informer := inform.GetInformService(appID)
	data := &InformSeatStatusChange{
		SeatID: seatID,
		Type:   1,
	}
	informer.BroadcastRoom(ctx, roomID, OnSeatStatusChange, data)
	return nil
}

func (is *InteractService) FinishInteract(ctx context.Context, appID, roomID string, seatID int, finishType int) error {
	seat, err := is.seatFactory.GetSeatByRoomIDSeatID(ctx, roomID, seatID)
	if err != nil {
		logs.CtxError(ctx, "get seat failed,error:%s", err)
		return err
	}
	if seat == nil {
		logs.CtxError(ctx, "seat is not exist")
		return custom_error.InternalError(errors.New("seat is not exist"))
	}

	userID := seat.GetOwnerUserID()
	if userID != "" {
		user, err := is.userFactory.GetActiveUserByRoomIDUserID(ctx, appID, roomID, userID)
		if err != nil {
			logs.CtxError(ctx, "get user failed,error:%s", err)
			return err
		}
		if user == nil {
			logs.CtxError(ctx, "user is not exist", err)
			return custom_error.ErrUserNotExist
		}
		user.SetInteract(vi_db.UserInteractStatusNormal, 0)
		user.UnmuteMic()
		user.UnmuteCamera()
		err = is.userFactory.Save(ctx, user)
		if err != nil {
			logs.CtxError(ctx, "save user failed,error:%s", err)
			return err
		}

		informer := inform.GetInformService(appID)
		data := &InformFinishInteract{
			UserInfo: user,
			SeatID:   seatID,
			Type:     finishType,
		}
		informer.BroadcastRoom(ctx, roomID, OnFinishInteract, data)
	}

	seat.SetOwnerUserID("")
	err = is.seatFactory.Save(ctx, seat)
	if err != nil {
		logs.CtxError(ctx, "save seat failed,error:%s", err)
		return err
	}

	return nil
}

func (is *InteractService) ClearRoom(ctx context.Context, appID, roomID string) {
	/*
		1. update user status by roomID
		2. update relations by roomID
		3. reset seat
	*/
	is.userFactory.UpdateUsersByRoomID(ctx, appID, roomID, map[string]interface{}{
		"interact_status": vi_db.UserInteractStatusNormal,
		"seat_id":         0,
	})

	is.RelationFactory.UpdateRelationByRoomID(ctx, roomID, map[string]interface{}{
		"relation": vi_db.RelationStatusNormalNothing,
	})

	seats, _ := is.seatFactory.GetSeatsByRoomID(ctx, roomID)
	for _, seat := range seats {
		seat.SetOwnerUserID("")
		seat.Unlock()
		is.seatFactory.Save(ctx, seat)
	}
}

func (is *InteractService) SetInviteReq(ctx context.Context, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID, reqID string) {
	key := getInviteKey(ctx, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
	redis_cli.Client.Set(ctx, key, reqID, 10*time.Second)
}

func (is *InteractService) GetInviteReq(ctx context.Context, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID string) string {
	key := getInviteKey(ctx, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID)
	reqID := redis_cli.Client.Get(ctx, key).Val()
	return reqID
}

func getInviteKey(ctx context.Context, appID, inviterRoomID, inviterUserID, inviteeRoomID, inviteeUserID string) string {
	key := InviteActhorReqPrefix + appID + inviterRoomID + inviterUserID + inviteeRoomID + inviteeUserID
	return key
}

func inArray(target int, source []int) bool {
	for _, t := range source {
		if target == t {
			return true
		}
	}
	return false
}
