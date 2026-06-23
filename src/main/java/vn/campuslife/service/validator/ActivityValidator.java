package vn.campuslife.service.validator;

public interface ActivityValidator<T> {
    /**
     * Validates the request.
     * @param request The request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validate(T request);
}
