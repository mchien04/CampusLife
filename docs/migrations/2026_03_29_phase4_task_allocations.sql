CREATE TABLE IF NOT EXISTS task_allocations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    CONSTRAINT uk_task_allocations_task_category UNIQUE (task_id, category_id),
    CONSTRAINT fk_task_allocations_task FOREIGN KEY (task_id) REFERENCES preparation_tasks(id),
    CONSTRAINT fk_task_allocations_category FOREIGN KEY (category_id) REFERENCES budget_categories(id)
);

