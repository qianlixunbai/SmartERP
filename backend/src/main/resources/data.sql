-- 用户：王经理(id=1)是直属领导，张总监(id=4)/李总监(id=5)是部门总监
INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES ('admin', '123456', '王经理', 'MANAGER', '技术部', NULL, 4);
INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES ('zhangsan', '123456', '张三', 'EMPLOYEE', '技术部', 1, 4);
INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES ('lisi', '123456', '李四', 'EMPLOYEE', '产品部', 1, 5);
INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES ('zongjian1', '123456', '张总监', 'MANAGER', '技术部', NULL, NULL);
INSERT INTO sys_user (username, password, real_name, role, department, direct_leader_id, department_head_id) VALUES ('zongjian2', '123456', '李总监', 'MANAGER', '产品部', NULL, NULL);
