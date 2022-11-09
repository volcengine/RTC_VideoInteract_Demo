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
package handler

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/login/login_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_handler"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/login/login_handler"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/endpoint"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type EventHandlerDispatch struct {
	mws      []endpoint.Middleware
	eps      endpoint.Endpoint
	handlers map[string]endpoint.Endpoint
}

func NewEventHandlerDispatch() *EventHandlerDispatch {
	ehd := &EventHandlerDispatch{
		mws:      make([]endpoint.Middleware, 0),
		handlers: make(map[string]endpoint.Endpoint),
	}
	ehd.init()
	return ehd
}

func (ehd *EventHandlerDispatch) Handle(ctx context.Context, params *public.EventParam) (resp interface{}, err error) {
	return ehd.eps(ctx, params)
}

func (ehd *EventHandlerDispatch) init() {
	ehd.initHandlers()
	ehd.initMws()
	ehd.buildInvokeChain()
}

func (ehd *EventHandlerDispatch) initHandlers() {
	//disconnect
	ehd.register("disconnect", disconnectHandler)

	//login
	loginHandler := login_handler.NewEventHandler()
	ehd.register("passwordFreeLogin", loginHandler.PasswordFreeLogin)
	ehd.register("verifyLoginToken", loginHandler.VerifyLoginToken)
	ehd.register("changeUserName", loginHandler.ChangeUserName)
	ehd.register("joinRTM", loginHandler.JoinRtm)

	//video_interact
	viHandler := vi_handler.NewEventHandler()
	ehd.register("viAgreeApply", viHandler.AgreeApply)
	ehd.register("viApplyInteract", viHandler.ApplyInteract)
	ehd.register("viFinishInteract", viHandler.FinishInteract)
	ehd.register("viFinishLive", viHandler.FinishLive)
	ehd.register("viGetActiveLiveRoomList", viHandler.GetActiveLiveRoomList)
	ehd.register("viGetApplyAudienceList", viHandler.GetApplyAudienceList)
	ehd.register("viGetAudienceList", viHandler.GetAudienceList)
	ehd.register("viInviteInteract", viHandler.InviteInteract)
	ehd.register("viJoinLiveRoom", viHandler.JoinLiveRoom)
	ehd.register("viLeaveLiveRoom", viHandler.LeaveLiveRoom)
	ehd.register("viManageInteractApply", viHandler.ManageInteractApply)
	ehd.register("viManageSeat", viHandler.ManageSeat)
	ehd.register("viReplyInvite", viHandler.ReplyInvite)
	ehd.register("viSendMessage", viHandler.SendMessage)
	ehd.register("viCreateRoom", viHandler.CreateRoom)
	ehd.register("viStartLive", viHandler.StartLive)
	ehd.register("viUpdateMediaStatus", viHandler.UpdateMediaStatus)
	ehd.register("viReconnect", viHandler.Reconnect)
	ehd.register("viClearUser", viHandler.ClearUser)
	ehd.register("viGetAnchorList", viHandler.GetAnchorList)
	ehd.register("viCloseChatRoom", viHandler.CloseChatRoom)
	ehd.register("viFinishAnchorInteract", viHandler.FinishAnchorInteract)
	ehd.register("viInviteAnchor", viHandler.InviteAnchor)
	ehd.register("viManageOtherAnchor", viHandler.ManageOtherAnchor)
	ehd.register("viReplyAnchor", viHandler.ReplyAnchor)
	//other
}

func (ehd *EventHandlerDispatch) register(eventName string, handlerFunc endpoint.Endpoint) {
	ehd.handlers[eventName] = handlerFunc
}

func (ehd *EventHandlerDispatch) initMws() {
	ehd.mws = append(ehd.mws, mwCheckParam)
	ehd.mws = append(ehd.mws, mwCheckLogin)
}

func (ehd *EventHandlerDispatch) buildInvokeChain() {
	handler := ehd.invokeHandleEndpoint()
	ehd.eps = endpoint.Chain(ehd.mws...)(handler)
}

func (ehd *EventHandlerDispatch) invokeHandleEndpoint() endpoint.Endpoint {
	return func(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
		f, ok := ehd.handlers[param.EventName]
		if !ok {
			return nil, custom_error.InternalError(errors.New("event not exist"))
		}
		return f(ctx, param)
	}
}

func mwCheckParam(next endpoint.Endpoint) endpoint.Endpoint {
	return func(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
		sourceApi, _ := ctx.Value(public.CtxSourceApi).(string)
		switch sourceApi {
		case "http":
			if param.EventName == "" || param.Content == "" || param.DeviceID == "" {
				return nil, custom_error.ErrInput
			}
		case "rtm":
			if param.AppID == "" || param.UserID == "" || param.EventName == "" || param.Content == "" || param.DeviceID == "" {
				return nil, custom_error.ErrInput
			}
		case "rpc":

		default:
			return nil, custom_error.InternalError(errors.New("source api missing"))
		}
		return next(ctx, param)
	}

}

type checkParam struct {
	LoginToken string `json:"login_token"`
}

func mwCheckLogin(next endpoint.Endpoint) endpoint.Endpoint {
	return func(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
		userService := login_service.GetUserService()
		sourceApi, _ := ctx.Value(public.CtxSourceApi).(string)
		switch sourceApi {
		case "rtm":
			p := &checkParam{}
			err = json.Unmarshal([]byte(param.Content), p)
			if err != nil {
				logs.CtxError(ctx, "json unmarshal failed,error:%s", err)
				return nil, custom_error.InternalError(err)
			}
			err = userService.CheckLoginToken(ctx, p.LoginToken)
			if err != nil {
				logs.CtxError(ctx, "login_token expired")
				return nil, err
			}
			loginUserID := userService.GetUserID(ctx, p.LoginToken)
			if loginUserID != param.UserID {
				return nil, custom_error.ErrorTokenUserNotMatch
			}
		}

		return next(ctx, param)
	}
}
