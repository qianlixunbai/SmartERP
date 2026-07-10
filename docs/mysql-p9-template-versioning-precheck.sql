-- ============================================================
-- SmartERP P9 — 模板版本化预检脚本（只读）
-- ============================================================
-- 用法：mysql -u root -p smarterp < docs/mysql-p9-template-versioning-precheck.sql
-- 安全要求：
--   1. 只能执行 SELECT 查询，不得修改数据
--   2. 不能使用 UPDATE/DELETE/INSERT/ALTER
--   3. 不假设生产库为空
--   4. 不自动生成 template_key
--   5. 不自动修复脏数据
-- ============================================================

USE smarterp;

SELECT '========================================' AS '';
SELECT 'P9 模板版本化 — 数据预检报告' AS '';
SELECT '========================================' AS '';

-- -------------------------------------------------------
-- 1. approval_template 总数及 enabled 分布
-- -------------------------------------------------------
SELECT '--- 1. approval_template 总数及 enabled 分布 ---' AS '';

SELECT
    COUNT(*)                                   AS total_templates,
    SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) AS enabled_count,
    SUM(CASE WHEN enabled = 0 THEN 1 ELSE 0 END) AS disabled_count
FROM approval_template;

SELECT id, name, enabled, create_time, update_time
FROM approval_template
ORDER BY id;

-- -------------------------------------------------------
-- 2. 重复模板名称（仅供人工参考，不得自动合并）
-- -------------------------------------------------------
SELECT '--- 2. 重复模板名称（仅供参考） ---' AS '';

SELECT name, COUNT(*) AS cnt
FROM approval_template
GROUP BY name
HAVING COUNT(*) > 1;

-- -------------------------------------------------------
-- 3. approval_node 的孤立 template_id
-- -------------------------------------------------------
SELECT '--- 3. approval_node 的孤立 template_id ---' AS '';

SELECT an.id        AS node_id,
       an.template_id,
       an.node_name
FROM approval_node an
         LEFT JOIN approval_template at ON at.id = an.template_id
WHERE at.id IS NULL;

-- -------------------------------------------------------
-- 4. template_field 的孤立 template_id
-- -------------------------------------------------------
SELECT '--- 4. template_field 的孤立 template_id ---' AS '';

SELECT tf.id        AS field_id,
       tf.template_id,
       tf.field_name
FROM template_field tf
         LEFT JOIN approval_template at ON at.id = tf.template_id
WHERE at.id IS NULL;

-- -------------------------------------------------------
-- 5. leave_request 空 template_id
-- -------------------------------------------------------
SELECT '--- 5. leave_request 空 template_id ---' AS '';

SELECT COUNT(*) AS empty_template_id_count
FROM leave_request
WHERE template_id IS NULL;

SELECT id, applicant_id, leave_type, status, template_id
FROM leave_request
WHERE template_id IS NULL;

-- -------------------------------------------------------
-- 6. leave_request template_id 不存在
-- -------------------------------------------------------
SELECT '--- 6. leave_request template_id 不存在 ---' AS '';

SELECT lr.id          AS leave_id,
       lr.template_id,
       lr.status
FROM leave_request lr
         LEFT JOIN approval_template at ON at.id = lr.template_id
WHERE lr.template_id IS NOT NULL
  AND at.id IS NULL;

-- -------------------------------------------------------
-- 7. leave_request.current_node_id 不存在
-- -------------------------------------------------------
SELECT '--- 7. leave_request.current_node_id 不存在 ---' AS '';

SELECT lr.id              AS leave_id,
       lr.current_node_id,
       lr.status
FROM leave_request lr
         LEFT JOIN approval_node an ON an.id = lr.current_node_id
WHERE lr.current_node_id IS NOT NULL
  AND an.id IS NULL;

-- -------------------------------------------------------
-- 8. currentNode 所属模板与 request.template_id 不一致
-- -------------------------------------------------------
SELECT '--- 8. currentNode 所属模板与 request.template_id 不一致 ---' AS '';

SELECT lr.id              AS leave_id,
       lr.template_id,
       lr.current_node_id,
       an.template_id     AS node_template_id
FROM leave_request lr
         JOIN approval_node an ON an.id = lr.current_node_id
WHERE lr.template_id IS NOT NULL
  AND lr.template_id != an.template_id;

-- -------------------------------------------------------
-- 9. approval_record.node_id 孤立
-- -------------------------------------------------------
SELECT '--- 9. approval_record.node_id 孤立 ---' AS '';

SELECT ar.id      AS record_id,
       ar.node_id,
       ar.action
FROM approval_record ar
         LEFT JOIN approval_node an ON an.id = ar.node_id
WHERE ar.node_id IS NOT NULL
  AND an.id IS NULL;

-- -------------------------------------------------------
-- 10. approval_task.node_id 孤立
-- -------------------------------------------------------
SELECT '--- 10. approval_task.node_id 孤立 ---' AS '';

SELECT at.id      AS task_id,
       at.node_id,
       at.status
FROM approval_task at
         LEFT JOIN approval_node an ON an.id = at.node_id
WHERE at.node_id IS NOT NULL
  AND an.id IS NULL;

-- -------------------------------------------------------
-- 11. expense_approval_task.node_id 孤立
-- -------------------------------------------------------
SELECT '--- 11. expense_approval_task.node_id 孤立 ---' AS '';

SELECT eat.id      AS task_id,
       eat.node_id,
       eat.status
FROM expense_approval_task eat
         LEFT JOIN approval_node an ON an.id = eat.node_id
WHERE eat.node_id IS NOT NULL
  AND an.id IS NULL;

-- -------------------------------------------------------
-- 12. expense_request.current_node_id 孤立
-- -------------------------------------------------------
SELECT '--- 12. expense_request.current_node_id 孤立 ---' AS '';

SELECT er.id              AS expense_id,
       er.current_node_id,
       er.status
FROM expense_request er
         LEFT JOIN approval_node an ON an.id = er.current_node_id
WHERE er.current_node_id IS NOT NULL
  AND an.id IS NULL;

-- -------------------------------------------------------
-- 13. 固定模板 ID 1 是否存在、名称、enabled 状态、节点数
-- -------------------------------------------------------
SELECT '--- 13. 固定模板 ID 1 信息 ---' AS '';

SELECT at.id,
       at.name,
       at.description,
       at.enabled,
       at.create_time,
       at.update_time,
       COUNT(an.id) AS node_count
FROM approval_template at
         LEFT JOIN approval_node an ON an.template_id = at.id
WHERE at.id = 1
GROUP BY at.id, at.name, at.description, at.enabled, at.create_time, at.update_time;

SELECT id, node_name, sort_order, approver_type, approver_id
FROM approval_node
WHERE template_id = 1
ORDER BY sort_order;

-- -------------------------------------------------------
-- 14. 现有经费请求能否从 currentNode 或 task node 推断模板
-- -------------------------------------------------------
SELECT '--- 14. 经费请求能否从 currentNode 推断模板 ---' AS '';

SELECT er.id                  AS expense_id,
       er.current_node_id,
       an.template_id         AS inferred_template_id
FROM expense_request er
         LEFT JOIN approval_node an ON an.id = er.current_node_id
ORDER BY er.id;

SELECT '--- 14a. 经费请求能否从 expense_approval_task node 推断模板 ---' AS '';

SELECT er.id                  AS expense_id,
       eat.node_id,
       an.template_id         AS inferred_template_id
FROM expense_request er
         JOIN expense_approval_task eat ON eat.expense_request_id = er.id
         LEFT JOIN approval_node an ON an.id = eat.node_id
ORDER BY er.id, eat.id;

-- -------------------------------------------------------
-- 15. 无法推断模板的经费请求数量
-- -------------------------------------------------------
SELECT '--- 15. 无法推断模板的经费请求数量 ---' AS '';

-- 既无 currentNode 也无 task 关联的经费请求
SELECT COUNT(*) AS expense_without_template_hint
FROM expense_request er
WHERE er.current_node_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM expense_approval_task eat WHERE eat.expense_request_id = er.id
  );

-- 按状态分组的无法推断模板的经费请求
SELECT er.status,
       COUNT(*) AS cnt
FROM expense_request er
WHERE er.current_node_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM expense_approval_task eat WHERE eat.expense_request_id = er.id
  )
GROUP BY er.status;

SELECT '========================================' AS '';
SELECT 'P9 预检脚本执行完毕。' AS '';
SELECT '========================================' AS '';
