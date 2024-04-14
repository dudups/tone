alter table project
    add top_score bigint not null default 0 comment '置顶排序值（加入置顶时的时间戳， 0表示不置顶，值越大，排序越靠前。';