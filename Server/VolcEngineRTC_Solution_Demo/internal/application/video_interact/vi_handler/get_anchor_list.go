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

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/login/login_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_service"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/public"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/logs"
)

type getAnchorListReq struct {
	LoginToken string `json:"login_token"`
}

type getAnchorListResp struct {
	AnchorList []*vi_service.User `json:"anchor_list"`
}

func (eh *EventHandler) GetAnchorList(ctx context.Context, param *public.EventParam) (resp interface{}, err error) {
	logs.CtxInfo(ctx, "viAgreeApply param:%+v", param)
	var p getAnchorListReq
	if err := json.Unmarshal([]byte(param.Content), &p); err != nil {
		logs.CtxWarn(ctx, "input format error, err: %v", err)
		return nil, custom_error.ErrInput
	}

	userID := login_service.GetUserService().GetUserID(ctx, p.LoginToken)

	userFactory := vi_service.GetUserFactory()

	anchors, err := userFactory.GetActiveAnchorList(ctx, param.AppID)
	if err != nil {
		logs.CtxError(ctx, "get anchors failed,error:%s", err)
		return nil, err
	}

	anchorList := make([]*vi_service.User, 0)
	for _, anchor := range anchors {
		if anchor.UserID != userID {
			anchorList = append(anchorList, anchor)
		}
	}

	resp = &getAnchorListResp{
		AnchorList: anchorList,
	}

	return resp, nil
}
