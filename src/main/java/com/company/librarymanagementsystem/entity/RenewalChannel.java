package com.company.librarymanagementsystem.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum RenewalChannel implements EnumClass<String> {

    COUNTER("COUNTER", "Tại quầy"),
    ONLINE("ONLINE", "Trực tuyến");

    private final String id;
    private final String caption;

    RenewalChannel(String id, String caption) {
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
    public static RenewalChannel fromId(String id) {
        for (RenewalChannel channel : RenewalChannel.values()) {
            if (channel.getId().equals(id)) {
                return channel;
            }
        }
        return null;
    }
}
