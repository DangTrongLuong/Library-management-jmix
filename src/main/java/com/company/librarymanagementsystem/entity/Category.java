package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JmixEntity
@Table(name = "CATEGORIES", indexes = {
        @Index(name = "IDX_CATEGORIES_ON_SLUG", columnList = "SLUG", unique = true)
})
@Entity
public class Category {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull(message = "Vui lòng nhập tên thể loại")
    @InstanceName
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "SLUG", nullable = false, length = 120, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children;

    @Column(name = "DDC_CODE", length = 20)
    private String ddcCode;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @ManyToMany(mappedBy = "categories")
    private List<Book> books;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    public String getDdcCode() {
        return ddcCode;
    }

    public void setDdcCode(String ddcCode) {
        this.ddcCode = ddcCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
