CREATE TABLE IF NOT EXISTS allocation_adjustment_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    preferred_category_id BIGINT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NULL,
    decided_at DATETIME NULL,
    decided_by_id BIGINT NULL,
    CONSTRAINT fk_allocation_adj_task FOREIGN KEY (task_id) REFERENCES preparation_tasks(id),
    CONSTRAINT fk_allocation_adj_requested_by FOREIGN KEY (requested_by_id) REFERENCES students(id),
    CONSTRAINT fk_allocation_adj_preferred_category FOREIGN KEY (preferred_category_id) REFERENCES budget_categories(id),
    CONSTRAINT fk_allocation_adj_decided_by FOREIGN KEY (decided_by_id) REFERENCES users(id)
);

