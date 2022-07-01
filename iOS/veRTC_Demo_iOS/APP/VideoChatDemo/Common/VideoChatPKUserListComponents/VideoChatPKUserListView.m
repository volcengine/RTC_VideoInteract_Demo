//
//  VideoChatPKUserListView.m
//  veRTC_Demo
//
//  Created by bytedance on 2022/3/14.
//  Copyright © 2022 bytedance. All rights reserved.
//

#import "VideoChatPKUserListView.h"
#import "VideoChatPKUserListTableViewCell.h"
#import "VideoChatEmptyCompoments.h"

@interface VideoChatPKUserListView ()
<
UITableViewDelegate,
UITableViewDataSource,
VideoChatPKUserListTableViewCellDelegate
>

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UIView *lineView;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) VideoChatEmptyCompoments *emptyCompoments;

@end

@implementation VideoChatPKUserListView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self setupViews];
    }
    return self;
}

- (void)setupViews {
    self.backgroundColor = [UIColor colorFromRGBHexString:@"#0E0825" andAlpha:0.95 * 255];
    [self addSubview:self.titleLabel];
    [self addSubview:self.lineView];
    [self addSubview:self.tableView];
    [self.titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.top.right.equalTo(self);
        make.height.mas_equalTo(48);
    }];
    [self.lineView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self);
        make.top.equalTo(self.titleLabel.mas_bottom);
        make.height.mas_equalTo(1);
    }];
    [self.tableView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.bottom.equalTo(self);
        make.top.equalTo(self.lineView.mas_bottom);
    }];
}

- (void)setDataArray:(NSArray<VideoChatUserModel *> *)dataArray {
    _dataArray = dataArray;
    [self.tableView reloadData];
    if (dataArray.count <= 0) {
        [self.emptyCompoments show];
    } else {
        [self.emptyCompoments dismiss];
    }
}

#pragma mark - UITableViewDataSource
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.dataArray.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    VideoChatPKUserListTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:NSStringFromClass([VideoChatPKUserListTableViewCell class]) forIndexPath:indexPath];
    cell.model = [self.dataArray objectAtIndex:indexPath.row];
    cell.delegate = self;
    return cell;
}

#pragma mark - VideoChatPKUserListTableViewCellDelegate
- (void)videoChatPKUserListTableViewCell:(VideoChatPKUserListTableViewCell *)cell didClickUserModel:(VideoChatUserModel *)model {
    if ([self.delegate respondsToSelector:@selector(videoChatPKUserListView:didClickUserModel:)]) {
        [self.delegate videoChatPKUserListView:self didClickUserModel:model];
    }
}

#pragma mark - getter
- (UILabel *)titleLabel {
    if (!_titleLabel) {
        _titleLabel = [[UILabel alloc] init];
        _titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
        _titleLabel.textColor = UIColor.whiteColor;
        _titleLabel.textAlignment = NSTextAlignmentCenter;
        _titleLabel.text = @"主播连线";
    }
    return _titleLabel;
}

- (UIView *)lineView {
    if (!_lineView) {
        _lineView = [[UIView alloc] init];
        _lineView.backgroundColor = [UIColor colorFromRGBHexString:@"#2A2441"];
    }
    return _lineView;
}

- (UITableView *)tableView {
    if (!_tableView) {
        _tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
        _tableView.backgroundColor = UIColor.clearColor;
        _tableView.delegate = self;
        _tableView.dataSource = self;
        _tableView.rowHeight = 60;
        _tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
        
        [_tableView registerClass:[VideoChatPKUserListTableViewCell class] forCellReuseIdentifier:NSStringFromClass([VideoChatPKUserListTableViewCell class])];
    }
    return _tableView;
}

- (VideoChatEmptyCompoments *)emptyCompoments {
    if (!_emptyCompoments) {
        _emptyCompoments = [[VideoChatEmptyCompoments alloc] initWithView:self.tableView
                                                                  message:@"暂无主播在线"];
    }
    return _emptyCompoments;
}


@end
