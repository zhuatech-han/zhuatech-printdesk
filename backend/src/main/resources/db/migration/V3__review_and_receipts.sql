-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
-- 新增确切审签与分批收回字段，不伪造旧记录的确认人或时间。
alter table proof_file add column reviewer_id bigint null;
alter table proof_file add column reviewed_at timestamp(6) null;
alter table quote_revision add column decision varchar(20) null;
alter table quote_revision add column reviewer_id bigint null;
alter table quote_revision add column reviewed_at timestamp(6) null;
alter table business_event add column metadata_json varchar(4000) not null default '{}';
create table vendor_receipt (
 id bigint auto_increment primary key,
 order_id bigint not null,
 task_id bigint not null,
 quantity int not null,
 amount decimal(18,2) not null,
 reference varchar(160) not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 foreign key(task_id) references vendor_task(id),
 foreign key(actor_id) references account(id),
 unique(task_id,reference),
 index idx_vendor_receipt_order(order_id)
);
