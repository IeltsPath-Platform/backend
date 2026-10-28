package com.ieltspath.commonsecurity.role;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalRolesTest {
    @Test
    void listsEveryRoleATokenMayCarry() {
        assertEquals(
                Set.of("ADMIN", "CUSTOMER", "CONTENT_AUTHOR", "EXAMINER", "SALES_STAFF", "STAFF_MANAGER", "STAFF"),
                CanonicalRoles.ALL);
    }
}
