package vn.campuslife.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "department.scope")
public class DepartmentScopeProperties {
    private Enforcement enforcement = new Enforcement();
    private boolean auditOnly = false;

    public boolean isEnforcementEnabled() {
        return enforcement.isEnabled();
    }

    public boolean isScopingActive() {
        return isEnforcementEnabled() || auditOnly;
    }

    public boolean shouldDenyAccess() {
        return isEnforcementEnabled() && !auditOnly;
    }

    @Getter
    @Setter
    public static class Enforcement {
        private boolean enabled = false;
    }
}
