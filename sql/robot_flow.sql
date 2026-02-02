-- 机器人流程表
drop table if exists sys_robot_flow;
create table sys_robot_flow (
  flow_id           bigint(20)      not null auto_increment    comment '流程ID',
  flow_name         varchar(64)     not null                   comment '流程名称',
  remark            varchar(500)    default null               comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (flow_id)
) engine=innodb auto_increment=100 comment = '机器人流程表';

-- 机器人流程步骤表
drop table if exists sys_robot_flow_step;
create table sys_robot_flow_step (
  step_id           bigint(20)      not null auto_increment    comment '步骤ID',
  flow_id           bigint(20)      not null                   comment '流程ID',
  step_order        int(4)          not null                   comment '步骤顺序',
  step_type         varchar(20)     not null                   comment '步骤类型(position/voice)',
  point_name        varchar(64)     default null               comment '点位名称',
  x_pos             double          default null               comment 'X坐标',
  y_pos             double          default null               comment 'Y坐标',
  yaw               double          default null               comment '朝向',
  command           varchar(64)     default null               comment '语音指令',
  wait_time         int(11)         default null               comment '等待时间(秒)',
  primary key (step_id)
) engine=innodb auto_increment=100 comment = '机器人流程步骤表';
