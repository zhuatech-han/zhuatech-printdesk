-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

-- 独立实现的按单印刷经营结构，初始化不含业务示例。

create table customer (
 id bigint auto_increment primary key,
 department_id bigint not null,
 account_id bigint,
 name varchar(120) not null,
 contact varchar(120) not null,
 email varchar(160) not null,
 address varchar(600) not null,
 enabled boolean not null,
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(account_id) references account(id),
 unique(account_id)
);

create table material (
 id bigint auto_increment primary key,
 department_id bigint not null,
 code varchar(60) not null,
 name varchar(120) not null,
 unit varchar(20) not null,
 currency varchar(3) not null,
 stock decimal(18,4) not null,
 inventory_value decimal(18,4) not null,
 cost decimal(18,4) not null,
 enabled boolean not null,
 version bigint not null,
 foreign key(department_id) references department(id),
 unique(department_id,code)
);

create table supplier (
 id bigint auto_increment primary key,
 department_id bigint not null,
 name varchar(120) not null,
 contact varchar(160) not null,
 enabled boolean not null,
 version bigint not null,
 foreign key(department_id) references department(id)
);

create table print_order (
 id bigint auto_increment primary key,
 department_id bigint not null,
 customer_id bigint not null,
 owner_id bigint not null,
 assignee_id bigint,
 title varchar(160) not null,
 customer_name varchar(120) not null,
 category varchar(60) not null,
 currency varchar(3) not null,
 state varchar(30) not null,
 instructions varchar(2000) not null,
 due_date date not null,
 discount decimal(18,2) not null,
 tax_percent decimal(10,4) not null,
 subtotal decimal(18,2) not null,
 total decimal(18,2) not null,
 revision int not null,
 version bigint not null,
 created_at timestamp(6) not null,
 updated_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(customer_id) references customer(id),
 foreign key(owner_id) references account(id),
 foreign key(assignee_id) references account(id)
);

create table print_line (
 id bigint auto_increment primary key,
 order_id bigint not null,
 material_id bigint not null,
 name varchar(160) not null,
 unit varchar(20) not null,
 material_name varchar(120) not null,
 quantity int not null,
 width_mm int not null,
 height_mm int not null,
 material_usage decimal(18,4) not null,
 material_cost decimal(18,4) not null,
 print_rate decimal(18,2) not null,
 finish_rate decimal(18,2) not null,
 setup decimal(18,2) not null,
 markup_percent decimal(10,4) not null,
 quoted decimal(18,2) not null,
 good int not null,
 scrap int not null,
 accepted int not null,
 shipped int not null,
 foreign key(order_id) references print_order(id),
 index idx_print_line_order(order_id),
 foreign key(material_id) references material(id)
);

create table quote_revision (
 id bigint auto_increment primary key,
 order_id bigint not null,
 number int not null,
 snapshot longtext not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_quote_revision_order(order_id),
 unique(order_id,number)
);

create table proof_file (
 id bigint auto_increment primary key,
 order_id bigint not null,
 kind varchar(20) not null,
 filename varchar(160) not null,
 media_type varchar(80) not null,
 sha256 varchar(64) not null,
 status varchar(20) not null,
 feedback varchar(1000) not null,
 size int not null,
 content longblob not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_proof_file_order(order_id)
);

create table stock_txn (
 id bigint auto_increment primary key,
 material_id bigint not null,
 order_id bigint,
 line_id bigint,
 source_id bigint,
 kind varchar(20) not null,
 quantity decimal(18,4) not null,
 cost decimal(18,4) not null,
 amount decimal(18,4) not null,
 reference varchar(160) not null,
 note varchar(600) not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_stock_txn_order(order_id),
 foreign key(material_id) references material(id),
 foreign key(source_id) references stock_txn(id)
);

create table work_entry (
 id bigint auto_increment primary key,
 order_id bigint not null,
 line_id bigint not null,
 actor_id bigint not null,
 good int not null,
 scrap int not null,
 minutes int not null,
 hourly_rate decimal(18,2) not null,
 amount decimal(18,2) not null,
 note varchar(600) not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_work_entry_order(order_id)
);

create table vendor_task (
 id bigint auto_increment primary key,
 order_id bigint not null,
 line_id bigint not null,
 supplier_id bigint not null,
 supplier_name varchar(120) not null,
 description varchar(600) not null,
 reference varchar(160) not null,
 state varchar(20) not null,
 quantity int not null,
 received int not null,
 amount decimal(18,2) not null,
 recognized decimal(18,2) not null,
 due_date date not null,
 foreign key(order_id) references print_order(id),
 index idx_vendor_task_order(order_id),
 foreign key(supplier_id) references supplier(id)
);

create table dispatch (
 id bigint auto_increment primary key,
 order_id bigint not null,
 reference varchar(160) not null,
 method varchar(20) not null,
 address varchar(600) not null,
 lines_json longtext not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_dispatch_order(order_id)
);

create table money_entry (
 id bigint auto_increment primary key,
 order_id bigint not null,
 source_id bigint,
 kind varchar(20) not null,
 reference varchar(160) not null,
 note varchar(600) not null,
 amount decimal(18,2) not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_money_entry_order(order_id),
 foreign key(source_id) references money_entry(id),
 unique(order_id,kind,reference)
);

create table business_event (
 id bigint auto_increment primary key,
 order_id bigint not null,
 actor_id bigint not null,
 action varchar(40) not null,
 note varchar(1000) not null,
 request_id varchar(36) not null,
 fingerprint varchar(64) not null,
 created_at timestamp(6) not null,
 foreign key(order_id) references print_order(id),
 index idx_business_event_order(order_id),
 unique(request_id)
);
