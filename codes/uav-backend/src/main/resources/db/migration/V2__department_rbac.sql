CREATE TABLE IF NOT EXISTS departments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dept_code VARCHAR(64) NOT NULL,
    dept_name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_departments_code (dept_code)
);

ALTER TABLE users
    MODIFY COLUMN role VARCHAR(32) NOT NULL DEFAULT 'EXECUTOR';

UPDATE users SET role = 'EXECUTOR' WHERE role = 'OPERATOR';

ALTER TABLE users
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER name,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id;

ALTER TABLE uav_devices
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER model,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id,
    ADD COLUMN owner_username VARCHAR(64) NULL AFTER department_name;

ALTER TABLE alert_rule
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER template_category,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id,
    ADD COLUMN created_by VARCHAR(64) NULL AFTER department_name;

ALTER TABLE missions
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER mission_type,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id,
    ADD COLUMN pilot_username VARCHAR(64) NULL AFTER pilot_name;

ALTER TABLE mission_data_record
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER mission_type,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id;

ALTER TABLE task_executions
    ADD COLUMN department_id BIGINT UNSIGNED NULL AFTER mission_type,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER department_id;
