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
package vi_db

import (
	"context"

	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/application/video_interact/vi_entity"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/db"
	"github.com/volcengine/VolcEngineRTC_Solution_Demo/internal/pkg/util"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

const RelationTable = "vi_relation"

const (
	RelationStatusNormalNothing  = 1
	RelationStatusAnchorInvite   = 2
	RelationStatusAnchorLinked   = 3
	RelationStatusAudienceInvite = 4
	RelationStatusAudienceApply  = 5
	RelationStatusAudienceLinked = 6
)

type DbRelationRepo struct{}

func (repo *DbRelationRepo) Save(ctx context.Context, relation *vi_entity.ViRelation) error {
	util.CheckPanic()
	err := db.Client.WithContext(ctx).Debug().Table(RelationTable).
		Clauses(clause.OnConflict{
			Columns:   []clause.Column{{Name: "from_room_id"}, {Name: "from_user_id"}, {Name: "to_room_id"}, {Name: "to_user_id"}},
			UpdateAll: true,
		}).Create(&relation).Error
	return err
}

func (repo *DbRelationRepo) GetRelationsByFromUser(ctx context.Context, fromRoomID, fromUserID string) ([]*vi_entity.ViRelation, error) {
	util.CheckPanic()

	var rs []*vi_entity.ViRelation
	err := db.Client.WithContext(ctx).Debug().Table(RelationTable).Where("from_room_id = ? and from_user_id = ?", fromRoomID, fromUserID).Find(&rs).Error
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	return rs, nil
}

func (repo *DbRelationRepo) GetRelationsByToUser(ctx context.Context, toRoomID, toUserID string) ([]*vi_entity.ViRelation, error) {
	util.CheckPanic()

	var rs []*vi_entity.ViRelation
	err := db.Client.WithContext(ctx).Debug().Table(RelationTable).Where("to_room_id = ? and to_user_id = ?", toRoomID, toUserID).Find(&rs).Error
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	return rs, nil
}

func (repo *DbRelationRepo) GetActiveRelationsByUser(ctx context.Context, roomID, userID string) ([]*vi_entity.ViRelation, error) {
	util.CheckPanic()

	var rs []*vi_entity.ViRelation
	err := db.Client.WithContext(ctx).Debug().Table(RelationTable).Where("((from_room_id = ? and from_user_id = ?) or (to_room_id = ? and to_user_id = ?)) and relation <> ?", roomID, userID, roomID, userID, RelationStatusNormalNothing).Find(&rs).Error
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	return rs, nil
}

func (repo *DbRelationRepo) GetRelation(ctx context.Context, fromRoomID, fromUserID, toRoomID, toUserID string) (*vi_entity.ViRelation, error) {
	util.CheckPanic()

	var rs *vi_entity.ViRelation
	err := db.Client.WithContext(ctx).Debug().Table(RelationTable).Where("from_room_id = ? and from_user_id = ? and to_room_id = ? and to_user_id = ?", fromRoomID, fromUserID, toRoomID, toUserID).First(&rs).Error
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	return rs, nil
}

func (repo *DbRelationRepo) UpdateRelationsWithMapByRoomID(ctx context.Context, roomID string, ups map[string]interface{}) error {
	util.CheckPanic()
	return db.Client.WithContext(ctx).Debug().Table(RelationTable).Where("from_room_id = ? or to_room_id = ?", roomID, roomID).Updates(ups).Error
}
