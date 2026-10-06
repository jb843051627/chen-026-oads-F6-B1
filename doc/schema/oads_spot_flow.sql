-- oads 点位名册五检查点流程 —— schema 扩展（chen-026）
-- 点位底册扩列、在走步序留痕、区局配置、岗位辖段授权、市里增量同步游标。
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_026

-- ---------------------------------------------------------------- 点位底册扩列
-- longitude/latitude/area_name/quarter_code/flow_step/build_kind：五检查点流程要用到的列。
-- flow_step 只作服务端裁决后的落账与兼容回显：这一行停在哪一步由系统按步序留痕自己数，
-- 页面填的档、经办人记的档都不作数；旧行此列为 NULL 即一步未走，不碰不报错。
ALTER TABLE t_oads_spot
    ADD COLUMN longitude decimal(12,8) DEFAULT NULL COMMENT '经度（现场采集，前端不得人工改写）' AFTER area_no,
    ADD COLUMN latitude decimal(12,8) DEFAULT NULL COMMENT '纬度（现场采集，前端不得人工改写）' AFTER longitude,
    ADD COLUMN area_name varchar(128) DEFAULT NULL COMMENT '辖段全称冗余（与目录有出入时以目录为准修正）' AFTER latitude,
    ADD COLUMN quarter_code varchar(8) DEFAULT NULL COMMENT '建行当季季度码 如2026Q4' AFTER area_name,
    ADD COLUMN flow_step int DEFAULT NULL COMMENT '在走步序 1待核 2已核 3压卡；NULL=旧行一步未走（步序以留痕重数为准）' AFTER quarter_code,
    ADD COLUMN build_kind varchar(16) DEFAULT NULL COMMENT '建行通道 MANUAL建档岗逐条 BATCH集中更换带出' AFTER flow_step;

CREATE INDEX idx_spot_no_quarter ON t_oads_spot (spot_no, quarter_code, del_flag);
CREATE INDEX idx_spot_area_quarter ON t_oads_spot (area_no, quarter_code, del_flag);

-- ---------------------------------------------------------------- 属地段册底册扩列
-- 围栏文本：矩形记 lng1,lat1;lng2,lat2（两对角点），判定取闭区间。
-- 坐标与辖段挂靠均来自现场采集，服务端只按这一栏与坐标做包含判定。
ALTER TABLE t_oads_area_book
    ADD COLUMN fence_text varchar(255) DEFAULT NULL COMMENT '辖段围栏 矩形 lng1,lat1;lng2,lat2 两对角点，闭区间';

-- 种子两段围栏补上（种子行已在 oads.sql，存在则忽略）
UPDATE t_oads_area_book SET fence_text = '100.000000,20.000000;101.000000,21.000000' WHERE id = 0 AND fence_text IS NULL;
UPDATE t_oads_area_book SET fence_text = '102.000000,22.000000;103.000000,23.000000' WHERE id = 1 AND fence_text IS NULL;

-- 补一条在册背街巷段册（与 oads.sql 两条种子配套：主干道在册、次干道已撤、背街巷在册）
INSERT INTO t_oads_area_book (id, area_no, area_name, area_kind, road_name, align_kind, status, fence_text, del_flag, create_by, create_time)
SELECT 2, 'DU02', '澜原省江水市云桥区青石巷（背街巷类，在册）', '背街巷', '澜原省—江水市—云桥区—青石巷门牌', '本局自录', 0,
       '110.000000,30.000000;111.000000,31.000000', 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_oads_area_book WHERE id = 2);

-- ---------------------------------------------------------------- 在走步序留痕
-- 一级一级推：1待核 2已核 3压卡。一次动作一行，只许追加。
-- 退回只清在走那一步的当轮（active 置 0，行不删）；重走另起一轮，上一回的东西保留、不与后补的并在一起。
-- 走到压卡这行便到头：冻结，不加不改不撤；非动不可只能另开一行。
CREATE TABLE IF NOT EXISTS t_oads_spot_flow_log (
  id bigint NOT NULL COMMENT '主键',
  spot_id bigint NOT NULL COMMENT '哪一处点位行',
  spot_no varchar(64) DEFAULT NULL COMMENT '点位码（落账当时冗余）',
  step_no int NOT NULL COMMENT '步序 1待核 2已核 3压卡',
  round_no int NOT NULL COMMENT '该步轮次 退一步加一轮 老轮文本留在本表 active=0',
  action varchar(16) NOT NULL COMMENT '动作 REACH走到 RETURN退回 HOLD就地停住',
  note varchar(500) DEFAULT NULL COMMENT '这一步当回记下的东西（原文，退回不抹）',
  operator varchar(64) DEFAULT NULL COMMENT '经办岗（只记是谁，档不由他点）',
  active int DEFAULT '1' COMMENT '本轮是否在档 1当前轮 0随退回退出（不删行，继续可查）',
  acted_at datetime(3) NOT NULL COMMENT '落账时刻（毫秒）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_flow_spot (spot_id, active),
  KEY idx_flow_step (spot_id, step_no, round_no, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点位在走步序留痕（append-only）';

-- ---------------------------------------------------------------- 区局名册配置
-- 单键单行 cfg_key='book'：每页条数等由区局在这里给出，页面与库两边照同一份配置分页。
CREATE TABLE IF NOT EXISTS t_oads_book_cfg (
  id bigint NOT NULL COMMENT '主键',
  cfg_key varchar(32) NOT NULL COMMENT '配置键（book=名册配置，单键单行）',
  page_size int NOT NULL COMMENT '名册每页条数（区局给值）',
  quarter_code varchar(8) NOT NULL COMMENT '当前在册季度码 如2026Q4',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_cfg_key (cfg_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区局名册配置（每页条数等，只一处给值）';

INSERT IGNORE INTO t_oads_book_cfg (id, cfg_key, page_size, quarter_code, del_flag, create_by, create_time)
VALUES (0, 'book', 10, '2026Q4', 0, 'seed', NOW());

-- ---------------------------------------------------------------- 岗位辖段授权
-- 可见范围按辖段切：本段经办岗只见本段挂靠的行；区局岗三类路段都可见，但范围仍限于划归它的辖段。
-- post_kind：SECTION本段经办岗 / BUREAU区局岗。
CREATE TABLE IF NOT EXISTS t_oads_post_area (
  id bigint NOT NULL COMMENT '主键',
  post_no varchar(64) NOT NULL COMMENT '岗位代号',
  post_name varchar(128) DEFAULT NULL COMMENT '岗位名',
  post_kind varchar(16) NOT NULL COMMENT '岗位类别 SECTION本段经办岗 BUREAU区局岗',
  area_no varchar(64) NOT NULL COMMENT '划归辖段代号（可多行）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_post_area (post_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位辖段授权（名册可见范围）';

-- ---------------------------------------------------------------- 市里增量同步游标
-- 全库唯一一行 cfg_key='city'：比对起点唯一。游标之前不复核，游标之后才参与本次比较。
-- 游标随本次同步一并推进，且与本次结果出自同一份计算；别处不得自行调整。
CREATE TABLE IF NOT EXISTS t_oads_spot_sync_cursor (
  id bigint NOT NULL COMMENT '主键',
  cfg_key varchar(32) NOT NULL COMMENT '配置键（city=市里增量，单键单行）',
  cursor_id bigint DEFAULT '0' COMMENT '本次起点：点位主键小于等于此值的行不复核',
  synced_at datetime(3) DEFAULT NULL COMMENT '最近一次同步时刻',
  conflict_note varchar(500) DEFAULT NULL COMMENT '最近一次同步交代',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sync_cursor_key (cfg_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='市里增量同步游标（全库唯一一行）';

INSERT IGNORE INTO t_oads_spot_sync_cursor (id, cfg_key, cursor_id, synced_at, del_flag)
VALUES (0, 'city', 0, NULL, 0);

-- ---------------------------------------------------------------- 菜单与权限（幂等，已存在同 id 则跳过）
-- 权限码与 OadsSpotBookController 上的 @RequiresPermissions 逐字对齐。
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9100, '点位名册', '点位名册五检查点', '/oadsSpotBook/view', 0, 0, 'oads:oadsSpotBook:view', 1, 'layui-icon-cols', 91, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9100);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9101, '名册分页', '三入口全等/前缀、按辖段可见', '/oadsSpotBook/list', 0, 9100, 'oads:oadsSpotBook:list', 2, '', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9101);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9102, '名册建行', '建档岗逐条/集中更换带出，同一套五检查点', '/oadsSpotBook/build', 0, 9100, 'oads:oadsSpotBook:build', 2, 'entypo-plus-squared', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9102);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9103, '数量更正改挂', '已核验处数更正、改挂路段，写库前重算待补', '/oadsSpotBook/correctGot', 0, 9100, 'oads:oadsSpotBook:edit', 2, 'fa fa-wrench', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9103);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9104, '在走推退停', '待核/已核/压卡推进、退回重来、就地停住', '/oadsSpotBook/advance', 0, 9100, 'oads:oadsSpotBook:flow', 2, 'fa fa-exchange', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9104);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9105, '市里增量同步', '游标唯一同次推进、冲突逐条以本局为准', '/oadsSpotBook/syncCity', 0, 9100, 'oads:oadsSpotBook:sync', 2, 'layui-icon-refresh-3', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9105);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9106, '名册条数配置', '每页条数由区局给出', '/oadsSpotBook/configurePageSize', 0, 9100, 'oads:oadsSpotBook:cfg', 2, 'fa fa-cog', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9106);

-- 岗位辖段授权种子示例：P-SECTION-DU00 为临江路(DU00)本段经办岗；P-BUREAU 为区局岗，划归 DU00、DU02 两段。
INSERT INTO t_oads_post_area (id, post_no, post_name, post_kind, area_no, del_flag, create_by, create_time)
SELECT 9200, 'P-SECTION-DU00', '临江路本段经办岗', 'SECTION', 'DU00', 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_oads_post_area WHERE id = 9200);
INSERT INTO t_oads_post_area (id, post_no, post_name, post_kind, area_no, del_flag, create_by, create_time)
SELECT 9201, 'P-BUREAU', '区局建档岗', 'BUREAU', 'DU00', 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_oads_post_area WHERE id = 9201);
INSERT INTO t_oads_post_area (id, post_no, post_name, post_kind, area_no, del_flag, create_by, create_time)
SELECT 9202, 'P-BUREAU', '区局建档岗', 'BUREAU', 'DU02', 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_oads_post_area WHERE id = 9202);
