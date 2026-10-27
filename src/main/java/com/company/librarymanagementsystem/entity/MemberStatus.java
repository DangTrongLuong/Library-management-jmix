package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum MemberStatus implements EnumClass<String> {

    ACTIVE("ACTIVE", "Đang hoạt động"),
    PENDING("PENDING", "Chờ duyệt"),
    SUSPENDED("SUSPENDED", "Tạm khóa"),
    EXPIRED("EXPIRED", "Hết hạn thẻ"),
    BLOCKED("BLOCKED", "Bị khóa");

    private final String id;
    private final String caption;

    MemberStatus(String id, String caption) {
        this.id = id;
        this.caption = caption;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    @Nullable
    public static MemberStatus fromId(String id) {
        for (MemberStatus s : MemberStatus.values()) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
