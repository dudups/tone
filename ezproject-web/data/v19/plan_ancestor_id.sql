UPDATE plan SET ancestor_id=parent_id WHERE ancestor_id=0 and parent_id > 0;
update plan c left join plan p on c.ancestor_id = p.id set c.is_active=false WHERE c.is_active=true and p.is_active=false;