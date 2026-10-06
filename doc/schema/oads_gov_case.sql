-- oads 监管办件四步流程 —— schema 扩展（chen-026）
-- 监管办件跟着一处在册点位走，一条只管一处；固定四步：1申报 2核准 3设置 4验收。
-- 旧的 t_oads_gov_flow（stage 0..3 那套残缺流转单）不添列、不改名、不动数据；
-- 本套另起一组等价表承接，与"三档在走流程 t_oads_spot_flow_log"互不串用。
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_026

-- ---------------------------------------------------------------- 办件主档
-- 一条办件一行。停在哪一步不看这一列的面子值，只按 t_oads_gov_case_log 留痕重数；
-- step_no 仅作裁决后的落账与兼容回显，报文带了步次也只作参考，写库读库都以服务端重数为准。
-- outcome 收尾形态：RUNNING在走 / SEALED验收封存 / WITHDRAWN申报后撤回 / REALIGNED路段划归邻段 / VOID验收步作废。
CREATE TABLE IF NOT EXISTS t_oads_gov_case (
  id bigint NOT NULL COMMENT '主键',
  case_no varchar(64) DEFAULT NULL COMMENT '办件编号（服务端发，报送不自带）',
  spot_id bigint DEFAULT NULL COMMENT '跟着的那一处在册点位行（t_oads_spot.id）',
  spot_no varchar(64) NOT NULL COMMENT '点位编码（建立四项输入之一）',
  area_no varchar(64) NOT NULL COMMENT '所属路段代号（建立四项输入之一）',
  place_count int NOT NULL COMMENT '处数（建立四项输入之一）',
  check_score int NOT NULL COMMENT '拟判定的校核分值（建立四项输入之一）',
  step_no int DEFAULT NULL COMMENT '当前步次 1申报 2核准 3设置 4验收（服务端按留痕重数落账，报文值不作数）',
  outcome varchar(16) NOT NULL DEFAULT 'RUNNING' COMMENT '收尾形态 RUNNING在走 SEALED已封存 WITHDRAWN已撤回 REALIGNED已划归邻段 VOID已作废',
  sealed_at datetime(3) DEFAULT NULL COMMENT '封存/收尾达成那一刻',
  recheck_cert_no varchar(64) DEFAULT NULL COMMENT '复验凭证编码（验收步两名见证齐备同次回写本行）',
  idem_key varchar(128) DEFAULT NULL COMMENT '同批并办幂等键（同键重放只回首条结果，不立第二条）',
  -- 生成列兜底：同一处点位任意时刻至多一条 RUNNING；收尾后此键置 NULL，同点位可另立新件。
  spot_running_key varchar(64) GENERATED ALWAYS AS (IF(outcome = 'RUNNING', spot_no, NULL)) STORED COMMENT '在走独占键（库内生成，勿手填）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除（撤销行不进主档，不写本表）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gov_case_no (case_no),
  UNIQUE KEY uk_gov_case_idem (idem_key),
  UNIQUE KEY uk_gov_case_spot_running (spot_running_key),
  KEY idx_gov_case_spot (spot_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监管办件主档（一处点位任意时刻至多一条在走）';

-- ---------------------------------------------------------------- 办件逐笔留痕（append-only）
-- 每步那句意见连同提交时刻，按时序压在办件后面；日后追溯只取这一串。
-- 一次动作一行只许追加：REACH走到 / RETURN退回 / WITHDRAW撤回 / REALIGN划归邻段 / VOID作废。
-- 后退只把被退那一步当轮在档行 active 置 0（行不删，按原先后转作追溯）；重走该步另起一轮重新累计。
CREATE TABLE IF NOT EXISTS t_oads_gov_case_log (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '哪条办件',
  spot_no varchar(64) DEFAULT NULL COMMENT '点位码（落账当时冗余）',
  step_no int NOT NULL COMMENT '步次 1申报 2核准 3设置 4验收；撤回/划归/作废记发生时所在步',
  round_no int NOT NULL COMMENT '该步轮次 退一次加一轮 老轮文本留本表 active=0',
  seq_no int NOT NULL COMMENT '本件本步本轮第几笔（逐笔累加，同一时刻两通道各一笔也不并）',
  action varchar(16) NOT NULL COMMENT '动作 REACH走到 RETURN退回 WITHDRAW撤回 REALIGN划归邻段 VOID作废',
  note varchar(500) DEFAULT NULL COMMENT '这一步当回那句意见（原文，退回不抹）',
  operator varchar(64) DEFAULT NULL COMMENT '值守岗（只记是谁，步次不由他点）',
  channel varchar(16) DEFAULT NULL COMMENT '送意见的通道（两通道同刻各送各的，只用于逐笔对流水）',
  idem_key varchar(128) DEFAULT NULL COMMENT '动作幂等键：同键重放不添第二行（同批并发两回只落一笔）',
  active int DEFAULT '1' COMMENT '本轮是否在档 1当前轮 0随退回转入追溯（不删行，继续可查）',
  acted_at datetime(3) NOT NULL COMMENT '提交时刻（毫秒）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gov_log_idem (idem_key),
  KEY idx_gov_log_case (case_id, active),
  KEY idx_gov_log_step (case_id, step_no, round_no, active),
  KEY idx_gov_log_time (acted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监管办件逐笔留痕（append-only，追溯只取这一串）';

-- ---------------------------------------------------------------- 验收见证意见
-- 验收需两名见证岗各提交一次：同一名见证岗第二笔保留首次、不覆盖不追加。
-- 复验凭证编码在两名都齐的同一次处理里回写主档；缺一样验收都不算走完。
CREATE TABLE IF NOT EXISTS t_oads_gov_case_witness (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '哪条办件',
  round_no int NOT NULL COMMENT '验收步轮次（退回重走加一轮，老轮行保留）',
  witness_post varchar(64) NOT NULL COMMENT '见证岗代号',
  opinion varchar(500) NOT NULL COMMENT '见证意见原文',
  cert_no varchar(64) DEFAULT NULL COMMENT '该笔随附复验凭证编码（两名齐备后取一回写主档）',
  submit_at datetime(3) NOT NULL COMMENT '提交时刻（毫秒，同刻两笔各记各的）',
  active int DEFAULT '1' COMMENT '本轮是否在档 1当前轮 0验收步退回后随老轮退出',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gov_witness_once (case_id, round_no, witness_post),
  KEY idx_gov_witness_case (case_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监管办件验收见证意见（两名各一笔，二次不覆盖不追加）';

-- ---------------------------------------------------------------- 设置步核收登记
-- 设置一步的核收是否记为同意只看这一张：agree=1 才准进验收；未记/记为不同意，验收不得开始。
CREATE TABLE IF NOT EXISTS t_oads_gov_case_receipt (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '哪条办件',
  round_no int NOT NULL COMMENT '设置步轮次（退回重走加一轮，老轮行保留）',
  agree int NOT NULL COMMENT '核收是否记为同意 1同意 0未同意',
  note varchar(500) DEFAULT NULL COMMENT '核收交代原文',
  operator varchar(64) DEFAULT NULL COMMENT '经办岗',
  received_at datetime(3) NOT NULL COMMENT '核收落账时刻（毫秒）',
  active int DEFAULT '1' COMMENT '本轮是否在档 1当前轮 0设置步退回后随老轮退出',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_gov_receipt_case (case_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监管办件设置步核收登记（未记同意不得验收）';

-- ---------------------------------------------------------------- 立件前报入暂存
-- 撤销只可能出现在第一条办件正式立起来之前那一小段：报入落本行（占未走完位置），
-- 立件成功后随主档转走，撤销只在本行标 REVOKED——被撤记录不进主档、不占未走完位置。
CREATE TABLE IF NOT EXISTS t_oads_gov_case_intake (
  id bigint NOT NULL COMMENT '主键',
  intake_no varchar(64) NOT NULL COMMENT '报入流水号（服务端发）',
  spot_no varchar(64) NOT NULL COMMENT '点位编码',
  area_no varchar(64) NOT NULL COMMENT '所属路段代号',
  place_count int NOT NULL COMMENT '处数',
  check_score int NOT NULL COMMENT '拟判定的校核分值',
  state varchar(16) NOT NULL COMMENT '暂存形态 OPEN报入在档 OPENED已立件 REVOKED已撤销',
  opened_case_id bigint DEFAULT NULL COMMENT '立件后指向的办件',
  idem_key varchar(128) DEFAULT NULL COMMENT '报入幂等键（同键重放取首条）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gov_intake_no (intake_no),
  UNIQUE KEY uk_gov_intake_idem (idem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监管办件立件前报入暂存（撤销只发生在这里）';

-- ---------------------------------------------------------------- 菜单与权限（幂等，已存在同 id 则跳过）
-- 权限码与 OadsGovCaseController 上的 @RequiresPermissions 逐字对齐。
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9110, '监管办件', '在册点位四步办件 申报/核准/设置/验收', '/oadsGovCase/view', 0, 0, 'oads:oadsGovCase:view', 1, 'layui-icon-form', 92, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9110);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9111, '办件台账', '在走与已封存照常出现，封存不给操作入口', '/oadsGovCase/list', 0, 9110, 'oads:oadsGovCase:list', 2, '', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9111);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9112, '办件立件', '四项输入齐才立，同点位在走唯一', '/oadsGovCase/open', 0, 9110, 'oads:oadsGovCase:open', 2, 'entypo-plus-squared', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9112);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9113, '办件推退与收尾', '紧邻推进/退一步/撤回/划归/作废/核收/见证', '/oadsGovCase/advance', 0, 9110, 'oads:oadsGovCase:flow', 2, 'fa fa-exchange', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9113);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9114, '在走数量', '未走完件数：立一件加一、封一件减一，后端一次查询', '/oadsGovCase/runningCount', 0, 9110, 'oads:oadsGovCase:list', 2, 'fa fa-calculator', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9114);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9115, '报入暂存', '立件前报入，正式立件前可撤销', '/oadsGovCase/intake', 0, 9110, 'oads:oadsGovCase:open', 2, 'fa fa-inbox', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9115);
