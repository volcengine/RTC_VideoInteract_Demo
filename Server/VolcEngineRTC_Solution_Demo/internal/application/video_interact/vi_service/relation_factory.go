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

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_db"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_entity"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/models/custom_error"
)

var relationFactoryClient *RelationFactory

type RelationFactory struct {
	relationRepo RelationRepo
}

func GetRelationFactory() *RelationFactory {
	if relationFactoryClient == nil {
		relationFactoryClient = &RelationFactory{
			relationRepo: GetRelationRepo(),
		}
	}
	return relationFactoryClient
}

func (rf *RelationFactory) Save(ctx context.Context, relation *Relation) error {
	if relation.IsDirty() {
		err := rf.relationRepo.Save(ctx, relation.ViRelation)
		if err != nil {
			return custom_error.InternalError(err)
		}
		relation.SetIsDirty(false)
	}
	return nil
}

func (rf *RelationFactory) NewRelation(ctx context.Context, fromRoomID, fromUserID, toRoomID, toUserID string) *Relation {
	return &Relation{
		ViRelation: &vi_entity.ViRelation{
			FromRoomID: fromRoomID,
			FromUserID: fromUserID,
			ToRoomID:   toRoomID,
			ToUserID:   toUserID,
			Relation:   vi_db.RelationStatusNormalNothing,
		},
		isDirty: true,
	}
}

func (rf *RelationFactory) GetRelationsByFromUser(ctx context.Context, fromRoomID, fromUserID string) ([]*Relation, error) {
	dbRelations, err := rf.relationRepo.GetRelationsByFromUser(ctx, fromRoomID, fromUserID)
	if err != nil {
		return nil, custom_error.InternalError(err)
	}
	res := make([]*Relation, 0)
	if dbRelations == nil {
		return nil, nil
	}

	for _, dbRelation := range dbRelations {
		relation := &Relation{
			ViRelation: dbRelation,
			isDirty:    false,
		}
		res = append(res, relation)
	}

	return res, nil
}

func (rf *RelationFactory) GetRelationsByToUser(ctx context.Context, toRoomID, toUserID string) ([]*Relation, error) {
	dbRelations, err := rf.relationRepo.GetRelationsByToUser(ctx, toRoomID, toUserID)
	if err != nil {
		return nil, custom_error.InternalError(err)
	}
	res := make([]*Relation, 0)
	if dbRelations == nil {
		return nil, nil
	}

	for _, dbRelation := range dbRelations {
		relation := &Relation{
			ViRelation: dbRelation,
			isDirty:    false,
		}
		res = append(res, relation)
	}

	return res, nil
}

func (rf *RelationFactory) GetRelationsByUser(ctx context.Context, roomID, userID string) ([]*Relation, error) {
	dbRelations, err := rf.relationRepo.GetActiveRelationsByUser(ctx, roomID, userID)
	if err != nil {
		return nil, custom_error.InternalError(err)
	}
	res := make([]*Relation, 0)
	if dbRelations == nil {
		return nil, nil
	}

	for _, dbRelation := range dbRelations {
		relation := &Relation{
			ViRelation: dbRelation,
			isDirty:    false,
		}
		res = append(res, relation)
	}

	return res, nil
}

func (rf *RelationFactory) GetRelation(ctx context.Context, fromRoomID, fromUserID, toRoomID, toUserID string) (*Relation, error) {
	dbRelation, err := rf.relationRepo.GetRelation(ctx, fromRoomID, fromUserID, toRoomID, toUserID)
	if err != nil {
		return nil, custom_error.InternalError(err)
	}
	if dbRelation == nil {
		return nil, nil
	}
	relation := &Relation{
		ViRelation: dbRelation,
		isDirty:    false,
	}

	return relation, nil
}

func (rf *RelationFactory) UpdateRelationByRoomID(ctx context.Context, roomID string, ups map[string]interface{}) error {
	err := rf.relationRepo.UpdateRelationsWithMapByRoomID(ctx, roomID, ups)
	if err != nil {
		return custom_error.InternalError(err)
	}
	return nil
}
