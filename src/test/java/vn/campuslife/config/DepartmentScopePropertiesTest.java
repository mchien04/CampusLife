package vn.campuslife.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentScopePropertiesTest {

    @Test
    void defaultFlags_disableScopingAndDenial() {
        DepartmentScopeProperties properties = new DepartmentScopeProperties();

        assertFalse(properties.isScopingActive());
        assertFalse(properties.shouldDenyAccess());
    }

    @Test
    void auditOnly_enablesScopingWithoutDenial() {
        DepartmentScopeProperties properties = new DepartmentScopeProperties();
        properties.setAuditOnly(true);

        assertTrue(properties.isScopingActive());
        assertFalse(properties.shouldDenyAccess());
    }

    @Test
    void enforcementEnabled_enablesScopingAndDenial() {
        DepartmentScopeProperties properties = new DepartmentScopeProperties();
        properties.getEnforcement().setEnabled(true);

        assertTrue(properties.isScopingActive());
        assertTrue(properties.shouldDenyAccess());
    }

    @Test
    void enforcementWithAuditOnly_enablesScopingWithoutDenial() {
        DepartmentScopeProperties properties = new DepartmentScopeProperties();
        properties.getEnforcement().setEnabled(true);
        properties.setAuditOnly(true);

        assertTrue(properties.isScopingActive());
        assertFalse(properties.shouldDenyAccess());
    }
}
