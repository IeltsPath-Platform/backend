package com.group01.commonsecurity.jwt;

/**
 * Cac ten claim JWT su dung trong internal token giua Gateway va cac backend service.
 * Tat ca cac ben (gateway, common-security, user-service) dung lop nay thay vi hard-code chuoi literal.
 */
public final class InternalJwtClaims {

    /** Ten claim chua danh sach vai tro (roles) cua nguoi dung. */
    public static final String ROLES = "roles";

    /** Ten claim chuan: issuer cua token. */
    public static final String ISSUER = "iss";

    /** Ten claim chuan: subject (userId dang la UUID). */
    public static final String SUBJECT = "sub";

    /** Ten claim chuan: expiration time cua token. */
    public static final String EXPIRATION = "exp";

    private InternalJwtClaims() {
    }
}

