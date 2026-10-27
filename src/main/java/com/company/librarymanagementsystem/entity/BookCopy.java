package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "BOOK_COPIES", indexes = {
        @Index(name = "IDX_BOOK_COPIES_ON_BARCODE", columnList = "BARCODE", unique = true),
        @Index(name = "IDX_BOOK_COPIES_ON_BOOK_ID", columnList = "BOOK_ID"),
        @Index(name = "IDX_BOOK_COPIES_ON_LOCATION_ID", columnList = "LOCATION_ID"),
        @Index(name = "IDX_BOOK_COPIES_ON_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_BOOK_COPIES_BOOK_STATUS", columnList = "BOOK_ID, STATUS")
})
@Entity
public class BookCopy {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @NotNull(message = "Vui lòng chọn đầu sách")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOK_ID", nullable = false)
    private Book book;

    @NotNull(message = "Vui lòng nhập hoặc sinh mã vạch")
    @Column(name = "BARCODE", nullable = false, length = 30, unique = true)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOCATION_ID")
    private Location location;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = BookCopyStatus.AVAILABLE.getId();

    @Column(name = "CONDITION_STATUS", nullable = false, length = 20)
    private String condition = BookCopyCondition.GOOD.getId();

    @Column(name = "ACQUIRED_DATE")
    private LocalDate acquiredDate;

    @Column(name = "ACQUISITION_PRICE", precision = 12, scale = 2)
    private BigDecimal acquisitionPrice;

    @Column(name = "ACQUISITION_SOURCE", length = 100)
    private String acquisitionSource;

    @Column(name = "NOTE", length = 255)
    private String note;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
        if (acquiredDate == null) {
            acquiredDate = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @InstanceName
    @DependsOnProperties({"barcode", "book"})
    public String getDisplayName() {
        return String.format("%s - %s", barcode != null ? barcode : "", book != null ? book.getTitle() : "");
    }

    public BookCopyStatus getStatusEnum() {
        return BookCopyStatus.fromId(status);
    }

    public void setStatusEnum(BookCopyStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.getId() : null;
    }

    public BookCopyCondition getConditionEnum() {
        return BookCopyCondition.fromId(condition);
    }

    public void setConditionEnum(BookCopyCondition conditionEnum) {
        this.condition = conditionEnum != null ? conditionEnum.getId() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public LocalDate getAcquiredDate() {
        return acquiredDate;
    }

    public void setAcquiredDate(LocalDate acquiredDate) {
        this.acquiredDate = acquiredDate;
    }

    public BigDecimal getAcquisitionPrice() {
        return acquisitionPrice;
    }

    public void setAcquisitionPrice(BigDecimal acquisitionPrice) {
        this.acquisitionPrice = acquisitionPrice;
    }

    public String getAcquisitionSource() {
        return acquisitionSource;
    }

    public void setAcquisitionSource(String acquisitionSource) {
        this.acquisitionSource = acquisitionSource;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
