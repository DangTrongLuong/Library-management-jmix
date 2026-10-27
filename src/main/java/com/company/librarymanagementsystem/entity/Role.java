package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JmixEntity
@Table(name = "ROLES", indexes = {
        @Index(name = "IDX_ROLES_ON_CODE", columnList = "CODE", unique = true)
})
@Entity
public class Role {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull(message = "Vui lòng nhập mã vai trò")
    @Column(name = "CODE", nullable = false, length = 30, unique = true)
    private String code;

    @NotNull(message = "Vui lòng nhập tên vai trò")
    @InstanceName
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @JoinTable(name = "ROLE_PERMISSIONS",
            joinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID", referencedColumnName = "ID"))
    @ManyToMany
    private List<Permission> permissions;

    @OneToMany(mappedBy = "role")
    private List<User> users;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
