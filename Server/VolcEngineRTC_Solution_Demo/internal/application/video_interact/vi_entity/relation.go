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
package vi_entity

type ViRelation struct {
	ID         int64  `gorm:"column:id" json:"id"`
	FromRoomID string `gorm:"column:from_room_id" json:"from_room_id"`
	FromUserID string `gorm:"column:from_user_id" json:"from_user_id"`
	ToRoomID   string `gorm:"column:to_room_id" json:"to_room_id"`
	ToUserID   string `gorm:"column:to_user_id" json:"to_user_id"`
	Relation   int    `gorm:"column:relation" json:"relation"`
}
