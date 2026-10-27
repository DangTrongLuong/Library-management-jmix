package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JmixEntity
@Table(name = "PERMISSIONS", indexes = {
        @Index(name = "IDX_PERMISSIONS_ON_CODE", columnList = "CODE", unique = true)
})
@Entity
public class Permission {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull(message = "Vui lòng chọn hoặc nhập mã quyền")
    @Column(name = "CODE", nullable = false, length = 60, unique = true)
    private String code;

    @NotNull(message = "Vui lòng chọn hoặc nhập tên quyền")
    @Column(name = "NAME", length = 150)
    @InstanceName
    private String name;

    @NotNull(message = "Vui lòng chọn phân hệ")
    @Column(name = "MODULE", nullable = false, length = 30)
    private String module;

    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToMany(mappedBy = "permissions")
    private List<Role> roles;

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "CREATED_AT")
    private java.time.OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "UPDATED_AT")
    private java.time.OffsetDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private java.time.OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.OffsetDateTime.now();
        }
        updatedAt = java.time.OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = java.time.OffsetDateTime.now();
    }

    public java.time.OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.time.OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.time.OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
