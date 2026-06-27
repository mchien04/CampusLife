package vn.campuslife.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Centralized notification message template utility.
 * Loads message templates from messages_vi.properties and formats them with parameters.
 */
@Component
public class NotificationMessageTemplate {

    private final MessageSource messageSource;

    public NotificationMessageTemplate(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Auto-registration
    public String autoRegisterImportantTitle() {
        return getMessage("notification.auto-register.important.title");
    }

    public String autoRegisterImportantContent(String activityName) {
        return getMessage("notification.auto-register.important.content", activityName);
    }

    public String autoRegisterMandatoryTitle() {
        return getMessage("notification.auto-register.mandatory.title");
    }

    public String autoRegisterMandatoryContent(String activityName) {
        return getMessage("notification.auto-register.mandatory.content", activityName);
    }

    public String autoRegisterDefaultTitle() {
        return getMessage("notification.auto-register.default.title");
    }

    public String autoRegisterDefaultContent(String activityName) {
        return getMessage("notification.auto-register.default.content", activityName);
    }

    // Registration status
    public String registrationApprovedTitle() {
        return getMessage("notification.registration.approved.title");
    }

    public String registrationApprovedContent(String activityName) {
        return getMessage("notification.registration.approved.content", activityName);
    }

    public String registrationRejectedTitle() {
        return getMessage("notification.registration.rejected.title");
    }

    public String registrationRejectedContent(String activityName) {
        return getMessage("notification.registration.rejected.content", activityName);
    }

    // Reminders
    public String reminder1DayTitle() {
        return getMessage("notification.reminder.1day.title");
    }

    public String reminder1DayContent(String activityName) {
        return getMessage("notification.reminder.1day.content", activityName);
    }

    public String reminder1HourTitle() {
        return getMessage("notification.reminder.1hour.title");
    }

    public String reminder1HourContent(String activityName) {
        return getMessage("notification.reminder.1hour.content", activityName);
    }

    // Tasks
    public String taskAssignedTitle() {
        return getMessage("notification.task.assigned.title");
    }

    public String taskAssignedContent(String taskName) {
        return getMessage("notification.task.assigned.content", taskName);
    }

    public String taskOverdueTitle() {
        return getMessage("notification.task.overdue.title");
    }

    public String taskOverdueContent(String taskName) {
        return getMessage("notification.task.overdue.content", taskName);
    }

    // Activity
    public String activityUpdatedTitle() {
        return getMessage("notification.activity.updated.title");
    }

    public String activityUpdatedContent(String activityName) {
        return getMessage("notification.activity.updated.content", activityName);
    }

    public String activityCancelledTitle() {
        return getMessage("notification.activity.cancelled.title");
    }

    public String activityCancelledContent(String activityName) {
        return getMessage("notification.activity.cancelled.content", activityName);
    }

    private String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            // Fallback: return the code itself if message not found
            return code;
        }
    }
}
