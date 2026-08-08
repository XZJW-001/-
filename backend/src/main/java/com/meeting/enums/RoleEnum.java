package com.meeting.enums;

public enum RoleEnum {
    SUPER_ADMIN(1, "超级管理员"),
    ADMIN(2, "管理员"),
    LEADER(3, "会议领导/老师"),
    USER(4, "普通用户");

    private final Integer code;
    private final String name;

    RoleEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static RoleEnum getByCode(Integer code) {
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}
