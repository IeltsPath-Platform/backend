package com.group01.commonsecurity.role;

import java.util.Set;

/**
 * Cac vai tro (roles) chuan cua he thong, su dung trong JWT token va kiem tra phan quyen.
 * <p>
 * Lop nay chi chua cac hang so thuan Java, khong phu thuoc vao Spring hoac Servlet.
 * An toan de su dung trong ca WebFlux (gateway) va Servlet (user-service).
 * <p>
 * Luu y: Day la cac ten role dung trong <em>security token</em>.
 * RoleName enum trong user-service la nguon su that cho domain/DB va khong nhat thiet phai
 * trung khop truc tiep voi cac constant nay.
 */
public final class CanonicalRoles {

    /** Vai tro quan tri vien he thong. */
    public static final String ADMIN = "ADMIN";

    /** Vai tro nguoi hoc. */
    public static final String LEARNER = "LEARNER";

    /**
     * Tap hop tat ca cac vai tro hop le cua he thong.
     * Su dung de xac thuc token: tu choi bat ky role nao khong co trong tap hop nay.
     */
    public static final Set<String> ALL = Set.of(ADMIN, LEARNER);

    private CanonicalRoles() {
    }
}

