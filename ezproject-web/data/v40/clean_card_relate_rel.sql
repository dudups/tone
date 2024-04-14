# 清除已经物理删除卡片，但在card_relate_rel表中未清除的数据。
delete
from card_relate_rel
where card_id in (select card_id
                  from (select t1.card_id, t2.id as `deletedId`
                        from card_relate_rel t1
                                 left join card t2 on t2.id = t1.card_id) t3
                  where t3.`deletedId` is null
)
   or related_card_id in (select card_id
                          from (select t1.card_id, t2.id as `deletedId`
                                from card_relate_rel t1
                                         left join card t2 on t2.id = t1.card_id) t3
                          where t3.`deletedId` is null
);