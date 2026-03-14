-- Make approval nullable to support WAITING_APPROVAL state (approved = NULL)

ALTER TABLE expenses
    MODIFY COLUMN is_approved BIT NULL;

