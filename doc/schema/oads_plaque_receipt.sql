-- oads 牌匾更换名册收讫 —— schema（chen-026）
-- 区局收讫环节新起的一条：专接各单位按季度归并上送的牌匾更换名册。
-- 一次报送为一册，一册含若干行，一行对应一处点位；册不拆开送，行不跨册走。
-- 收讫手上只有四步：点行数 → 验数据 → 定去向 → 出回执，四步连着做完，中间没有人工兜底。
-- 原集中更换核验行（t_oads_check_row）与点位名册五检查点那一套本体一律不动；
-- 日后按页收、按季收的新名目由本条担，不拿旧本体开刀。
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_026

-- ---------------------------------------------------------------- 册账：一次报送一行，册不拆
-- 单册行数上限只有一个来源——本册抬头那一格（row_limit_raw 原文 / row_limit 认下的值）：
-- 抬头空着按 200 认；写成负数或大得没边（非数/越过硬界）的按没填认，也认 200。
-- 进门点数只数物理行：同一处点位在一册里写两行的，年年有——账上各落一条，
-- 不按点位去重，不并笔，一条不多一条不少，回执三个数都按物理行数（依据只此一个，不一处一个说法）。
CREATE TABLE IF NOT EXISTS t_oads_plaque_book (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '批次码（册的身份；区级报送与市级复核两条通道递的是同一串）',
  book_no varchar(64) DEFAULT NULL COMMENT '名册册号（报送单位+季度合成，仅记账用）',
  unit_no varchar(64) DEFAULT NULL COMMENT '归并上送的单位代号（各单位先归并到区里）',
  quarter_code varchar(8) DEFAULT NULL COMMENT '季度码 按季合成一册 如2026Q4',
  put_kind varchar(16) NOT NULL COMMENT '收自哪条通道 AREA区级报送 CITY市级复核（只记第一回那一条）',
  row_limit_raw varchar(32) DEFAULT NULL COMMENT '抬头那格原文（空/负数/大得没边都照原文落，事后翻得出当初写了什么）',
  row_limit int NOT NULL COMMENT '认下的单册行数上限（抬头唯一来源；认不得的按200）',
  counted_rows int NOT NULL COMMENT '进门点数所得物理行数（与上限一并落账，事后能翻出当初数了多少）',
  accepted_rows int NOT NULL DEFAULT 0 COMMENT '入库行数（通过+待核，都在册里占位；回库逐行数，不抄页面临值）',
  returned_rows int NOT NULL DEFAULT 0 COMMENT '退回行数（回库逐行数）；入库+退回=本册行数',
  over_limit int NOT NULL DEFAULT 0 COMMENT '是否越过抬头上限 0否 1是（照常收讫，只在账上与回执点名，不拦册）',
  dup_count int NOT NULL DEFAULT 0 COMMENT '同一批次码重复送达回数（第二回起每回加一，行不重复入）',
  received_at datetime(3) DEFAULT NULL COMMENT '收讫那一刻（四步同次做完）',
  status int DEFAULT 0 COMMENT '册情形 0已收讫',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注（上限认下200等说法逐字落此）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plaque_book_batch (batch_no),
  KEY idx_plaque_book_quarter (quarter_code, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾更换名册册账（一册一行，不拆）';

-- ---------------------------------------------------------------- 行账：物理行各一条，同码两行各一条
-- 结论只有三个取值：PENDING待核 PASS通过 REJECT退回，由收讫环节服务端裁决，
-- 报文自称的结论不算数；第四种写法不予写入（原文扫进 remark 留个说法，不进结论格）。
-- 缺照片不空退：photo_no 留空、结论 PENDING，行照样占位，下一轮核看补挂（attachPhoto）。
-- 卷宗层挂在行上：老化只挪"连着满两个年度停在待核"的那几行，行不游移到别册、一条不删，
-- 挪到历史卷宗层后现行名单不再列出，历史层名单照常可查。
CREATE TABLE IF NOT EXISTS t_oads_plaque_row (
  id bigint NOT NULL COMMENT '主键',
  book_id bigint NOT NULL COMMENT '落在哪一册（行不许游移到别册；归档只挪卷宗层不换册）',
  batch_no varchar(64) NOT NULL COMMENT '批次码（冗余自册账）',
  row_no int NOT NULL COMMENT '原报物理行次（进门点数的次序，同码两行各占各的行次）',
  spot_no varchar(64) DEFAULT NULL COMMENT '点位编码（身份只认它；缺填退回，不按版面名近似去配）',
  view_name varchar(255) DEFAULT NULL COMMENT '报文里的版面名称那串字符（只用来与主档比对，不作数）',
  road_code varchar(64) DEFAULT NULL COMMENT '报文里的路段代号那串字符（只用来与主档比对，不作数）',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行应更换处数（须与核验合格处数同时到达；不代填不默零）',
  chk_num decimal(12,2) DEFAULT NULL COMMENT '本行核验合格处数（两项只到一项即验收不过）',
  photo_no varchar(128) DEFAULT NULL COMMENT '现场照片附件编号（随册一起递；缺则空着占位待核，下轮补挂）',
  verdict varchar(16) NOT NULL COMMENT '行结论 PENDING待核 PASS通过 REJECT退回（只此三值，服务端裁决）',
  reject_reason varchar(500) DEFAULT NULL COMMENT '退回理由逐句原文（多条分号隔，哪行哪条一句句回查）',
  master_spot_no varchar(64) DEFAULT NULL COMMENT '主档点中的点位编码（全等配上才带出；配不上留空）',
  master_view_name varchar(255) DEFAULT NULL COMMENT '主档带出的版面名称（牌面题名）',
  master_road_code varchar(64) DEFAULT NULL COMMENT '主档带出的路段代号',
  master_diff int NOT NULL DEFAULT 0 COMMENT '报文与主档有无出入 0无 1有（以主档为准，回执列出本行）',
  photo_attached_at datetime(3) DEFAULT NULL COMMENT '照片补挂那一刻（补挂前为空）',
  layer int NOT NULL DEFAULT 0 COMMENT '卷宗层 0现行层 1历史卷宗层（老化归档逐行挪，行一条不减）',
  archived_at datetime(3) DEFAULT NULL COMMENT '本行转入历史卷宗层那一刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注（第四种结论写法原文等扫在此处）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plaque_row (book_id, row_no),
  KEY idx_plaque_row_layer (book_id, layer),
  KEY idx_plaque_row_verdict (verdict, layer),
  KEY idx_plaque_row_spot (spot_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾更换名册行账（物理行各一条）';

-- ---------------------------------------------------------------- 当地核验上限（第3步"当地那条线"）
-- 应更换处数高于当地这条线的行判退回。按辖段给值；area_no='DEFAULT' 为全区缺省那条，
-- 辖段没单列时取它。线只有这一处来源，页面那串不作数。
CREATE TABLE IF NOT EXISTS t_oads_plaque_limit (
  id bigint NOT NULL COMMENT '主键',
  area_no varchar(64) NOT NULL COMMENT '辖段代号；DEFAULT=全区缺省核验上限',
  area_name varchar(128) DEFAULT NULL COMMENT '辖段全称（记账用）',
  qty_limit decimal(12,2) NOT NULL COMMENT '当地核验上限：本行应更换处数高于此值即退回',
  status int DEFAULT 0 COMMENT '情形 0在用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plaque_limit_area (area_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾更换当地核验上限（按辖段，缺省一条）';

INSERT IGNORE INTO t_oads_plaque_limit (id, area_no, area_name, qty_limit, status, del_flag, create_by, create_time, remark)
VALUES (0, 'DEFAULT', '全区缺省核验上限', 100.00, 0, 0, 'seed', NOW(), '辖段未单列时取这条'),
       (1, 'DU00', '临江路段（主干道类，在册）', 50.00, 0, 0, 'seed', NOW(), '主干道单点位牌匾密度低，上限收紧');

-- ---------------------------------------------------------------- 收讫流水（append-only；销账与流水联动）
-- 一次收讫同事务先落销账（册账/行账）再落本流水，成则同成、败则同回；
-- 重送、回执、补挂改了行去处、对账校正、老化归档各记一行，只许追加，不改不删。
-- 账实差以流水为准：对账发现两边对不上，销账（册/行汇总）认，按流水把销账抹平，
-- 校正本身再记一笔 ADJUST，归收讫环节对账认。这个取舍由本环节在收讫规矩里定死，不由页面挑。
CREATE TABLE IF NOT EXISTS t_oads_plaque_flow (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '哪一册（批次码）',
  action varchar(16) NOT NULL COMMENT '动作 RECEIVE收讫 RESEND重复送达 RECEIPT出回执 ATTACH照片补挂 ADJUST对账校正 ARCHIVE老化归档',
  put_kind varchar(16) DEFAULT NULL COMMENT '本笔走的通道 AREA/CITY（RESEND 记第二回走的那条）',
  total_rows int DEFAULT NULL COMMENT '本笔记的本册行数（快照，回库逐行数出来的那个）',
  accepted_rows int DEFAULT NULL COMMENT '本笔记的入库行数',
  returned_rows int DEFAULT NULL COMMENT '本笔记的退回行数',
  content varchar(500) DEFAULT NULL COMMENT '这一笔的交代（逐行点名/上限认值/校正归谁认，原文）',
  operator varchar(64) DEFAULT NULL COMMENT '经办岗（系统对账时记收讫环节）',
  acted_at datetime(3) NOT NULL COMMENT '落账时刻（毫秒）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_plaque_flow_book (batch_no, acted_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾更换名册收讫流水（append-only）';

-- ---------------------------------------------------------------- 菜单与权限（幂等，已存在同 id 则跳过）
-- 权限码与 OadsPlaqueReceiptController 上的 @RequiresPermissions 逐字对齐。
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9300, '牌匾更换名册收讫', '区局收讫环节：点行数/验数据/定去向/出回执', '/oadsPlaqueReceipt/view', 0, 0, 'oads:oadsPlaqueReceipt:view', 1, 'layui-icon-form', 93, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9300);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9301, '区级报送收讫', '区级通道整册接收（一次一册，不接零散逐行）', '/oadsPlaqueReceipt/receiveArea', 0, 9300, 'oads:oadsPlaqueReceipt:receive', 2, 'fa fa-inbox', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9301);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9302, '市级复核通道收讫', '同一批次码市级复核再递一回：不重复入行，回执同册重出', '/oadsPlaqueReceipt/receiveCity', 0, 9300, 'oads:oadsPlaqueReceipt:review', 2, 'fa fa-share-square-o', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9302);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9303, '收讫回执', '本册行数/入库行数/退回行数，一律回库逐行数', '/oadsPlaqueReceipt/receipt', 0, 9300, 'oads:oadsPlaqueReceipt:view', 2, 'fa fa-file-text-o', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9303);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9304, '现行/历史名单', '现行层名册与历史卷宗层名单（存量一条不减）', '/oadsPlaqueReceipt/listRows', 0, 9300, 'oads:oadsPlaqueReceipt:list', 2, 'layui-icon-cols', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9304);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9305, '缺照片补挂', '下一轮核看时为待核行补挂照片，行不游移到别册', '/oadsPlaqueReceipt/attachPhoto', 0, 9300, 'oads:oadsPlaqueReceipt:attach', 2, 'fa fa-picture-o', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9305);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9306, '销账流水对账', '两边对不上以流水为准校正销账，校正归收讫环节认', '/oadsPlaqueReceipt/reconcile', 0, 9300, 'oads:oadsPlaqueReceipt:reconcile', 2, 'fa fa-balance-scale', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9306);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9307, '老化归档', '待核连续满两个年度的行转入历史卷宗层', '/oadsPlaqueReceipt/archiveAging', 0, 9300, 'oads:oadsPlaqueReceipt:archive', 2, 'fa fa-archive', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9307);
