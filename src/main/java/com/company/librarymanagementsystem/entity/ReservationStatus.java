package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum ReservationStatus implements EnumClass<String> {

    PENDING("PENDING", "Đang chờ sách"),
    READY("READY", "Sẵn sàng nhận"),
    FULFILLED("FULFILLED", "Đã nhận sách"),
    CANCELLED("CANCELLED", "Đã hủy"),
    EXPIRED("EXPIRED", "Hết hạn nhận");

    private final String id;
    private final String caption;

    ReservationStatus(String id, String caption) {
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
    public static ReservationStatus fromId(String id) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
