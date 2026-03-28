package vn.campuslife.exception;

import vn.campuslife.model.preparation.OverBudgetInfoDto;

public class OverBudgetException extends RuntimeException {
    private final OverBudgetInfoDto info;

    public OverBudgetException(String message, OverBudgetInfoDto info) {
        super(message);
        this.info = info;
    }

    public OverBudgetInfoDto getInfo() {
        return info;
    }
}

