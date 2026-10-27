package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum BookCopyStatus implements EnumClass<String> {

    AVAILABLE("AVAILABLE", "Sẵn sàng trên kệ"),
    BORROWED("BORROWED", "Đang cho mượn"),
    RESERVED("RESERVED", "Đang giữ chỗ"),
    LOST("LOST", "Đã mất"),
    DAMAGED("DAMAGED", "Hư hỏng"),
    MAINTENANCE("MAINTENANCE", "Đang bảo trì"),
    WITHDRAWN("WITHDRAWN", "Đã thanh lý");

    private final String id;
    private final String caption;

    BookCopyStatus(String id, String caption) {
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
    public static BookCopyStatus fromId(String id) {
        for (BookCopyStatus status : BookCopyStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
