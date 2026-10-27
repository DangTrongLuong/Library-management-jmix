package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JmixEntity
@Table(name = "AUTHORS")
@Entity
public class Author {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull(message = "Vui lòng nhập họ và tên tác giả")
    @InstanceName
    @Column(name = "FULL_NAME", nullable = false, length = 150)
    private String fullName;

    @Column(name = "BIRTH_YEAR", length = 50)
    private String birthYear;

    @Column(name = "NATIONALITY", length = 60)
    private String nationality;

    @Lob
    @Column(name = "BIOGRAPHY")
    private String biography;

    @ManyToMany(mappedBy = "authors")
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}

