package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "AUDIT_LOGS", indexes = {
        @Index(name = "IDX_AUDIT_ON_ACTION", columnList = "ACTION"),
        @Index(name = "IDX_AUDIT_ON_ENTITY", columnList = "ENTITY_NAME"),
        @Index(name = "IDX_AUDIT_ON_USER", columnList = "USERNAME"),
        @Index(name = "IDX_AUDIT_ON_CREATED_AT", columnList = "CREATED_AT")
})
@Entity
public class AuditLog {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull
    @Column(name = "ACTION", nullable = false, length = 50)
    private String action;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false, length = 50)
    private String entityName;

    @Column(name = "ENTITY_ID", length = 100)
    private String entityId;

    @NotNull
    @Column(name = "USERNAME", nullable = false, length = 50)
    private String username;

    @Column(name = "DETAILS", length = 2000)
    private String details;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @InstanceName
    public String getInstanceName() {
        return String.format("[%s] %s bởi %s", action, entityName, username);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
