package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@JmixEntity
@Table(name = "BOOKS", indexes = {
        @Index(name = "IDX_BOOKS_ON_ISBN", columnList = "ISBN", unique = true),
        @Index(name = "IDX_BOOKS_ON_DDC", columnList = "DDC"),
        @Index(name = "IDX_BOOKS_ON_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_BOOKS_ON_TITLE", columnList = "TITLE"),
        @Index(name = "IDX_BOOKS_STATUS_ID", columnList = "STATUS, ID")
})
@Entity
public class Book {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Column(name = "ISBN", length = 20, unique = true)
    private String isbn;

    /**
     * Mã phân loại thập phân Dewey (DDC - Dewey Decimal Classification) chi tiết của sách.
     * Ví dụ: 005.1 (Kỹ thuật phần mềm), 895.9223 (Tiểu thuyết Việt Nam), 158.1 (Kỹ năng sống)
     */
    @Column(name = "DDC", length = 30)
    private String ddc;

    @NotNull(message = "Vui lòng nhập nhan đề sách")
    @InstanceName
    @Column(name = "TITLE", nullable = false, length = 255)
    private String title;

    @Column(name = "SUBTITLE", length = 255)
    private String subtitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PUBLISHER_ID")
    private Publisher publisher;

    @JoinTable(name = "BOOK_AUTHORS",
            joinColumns = @JoinColumn(name = "BOOK_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID", referencedColumnName = "ID"))
    @ManyToMany
    private List<Author> authors;

    @JoinTable(name = "BOOK_CATEGORIES",
            joinColumns = @JoinColumn(name = "BOOK_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "ID"))
    @ManyToMany
    private List<Category> categories;

    @Column(name = "PUBLISH_YEAR", length = 50)
    private String publishYear;

    @Column(name = "EDITION", length = 50)
    private String edition;

    @Column(name = "LANGUAGE", length = 20)
    private String language;

    @Column(name = "PAGE_COUNT")
    private Integer pageCount;

    @Column(name = "PRICE", precision = 12, scale = 2)
    private BigDecimal price;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COVER_IMAGE_URL", length = 500)
    private String coverImageUrl;

    @Column(name = "IS_REFERENCE")
    private Boolean isReference = false;

    @Column(name = "STATUS", length = 20)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "book")
    private List<BookCopy> copies;

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

    @io.jmix.core.metamodel.annotation.JmixProperty
    public Integer getTotalCopies() {
        return copies != null ? copies.size() : 0;
    }

    @io.jmix.core.metamodel.annotation.JmixProperty
    public Long getAvailableCopies() {
        if (copies == null) return 0L;
        return copies.stream()
                .filter(c -> "AVAILABLE".equals(c.getStatus()) && c.getDeletedAt() == null)
                .count();
    }

    @io.jmix.core.metamodel.annotation.JmixProperty
    public String getCopiesCountInfo() {
        long available = getAvailableCopies();
        int total = getTotalCopies();
        return String.format("%d / %d sẵn sàng", available, total);
    }

    public List<BookCopy> getCopies() {
        return copies;
    }

    public void setCopies(List<BookCopy> copies) {
        this.copies = copies;
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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(String publishYear) {
        this.publishYear = publishYear;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public Boolean getIsReference() {
        return isReference;
    }

    public void setIsReference(Boolean reference) {
        isReference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDdc() {
        return ddc;
    }

    public void setDdc(String ddc) {
        this.ddc = ddc;
    }
}
