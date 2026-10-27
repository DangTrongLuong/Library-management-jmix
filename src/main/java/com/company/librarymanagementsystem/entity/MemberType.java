package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "MEMBER_TYPES", indexes = {
        @Index(name = "IDX_MEMBER_TYPES_ON_CODE", columnList = "CODE", unique = true)
})
@Entity
public class MemberType {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull(message = "Mã loại độc giả không được để trống")
    @Column(name = "CODE", nullable = false, length = 30, unique = true)
    private String code;

    @NotNull(message = "Tên loại độc giả không được để trống")
    @InstanceName
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @NotNull(message = "Số sách mượn tối đa không được để trống")
    @Min(value = 1, message = "Số sách mượn tối đa phải từ 1 đến 50 cuốn")
    @Max(value = 50, message = "Số sách mượn tối đa không quá 50 cuốn")
    @Column(name = "MAX_LOAN_BOOKS", nullable = false)
    private Integer maxLoanBooks = 5;

    @NotNull(message = "Số ngày mượn mặc định không được để trống")
    @Min(value = 1, message = "Số ngày mượn phải từ 1 đến 365 ngày")
    @Max(value = 365, message = "Số ngày mượn tối đa 365 ngày")
    @Column(name = "LOAN_DAYS", nullable = false)
    private Integer loanDays = 14;

    @NotNull(message = "Số lần gia hạn tối đa không được để trống")
    @Min(value = 0, message = "Số lần gia hạn không được âm")
    @Column(name = "MAX_RENEWALS", nullable = false)
    private Integer maxRenewals = 1;

    @NotNull(message = "Số ngày gia hạn mỗi lần không được để trống")
    @Min(value = 1, message = "Số ngày gia hạn mỗi lần phải từ 1 ngày trở lên")
    @Column(name = "RENEW_DAYS", nullable = false)
    private Integer renewDays = 7;

    @NotNull(message = "Số lượt đặt trước tối đa không được để trống")
    @Min(value = 0, message = "Số lượt đặt trước không được âm")
    @Column(name = "MAX_RESERVATIONS", nullable = false)
    private Integer maxReservations = 3;

    @NotNull(message = "Mức phạt mỗi ngày không được để trống")
    @DecimalMin(value = "0.0", message = "Mức phạt không được là số âm")
    @Column(name = "FINE_PER_DAY", nullable = false, precision = 12, scale = 2)
    private BigDecimal finePerDay = new BigDecimal("5000.00");

    @NotNull(message = "Thời hạn thẻ (tháng) không được để trống")
    @Min(value = 1, message = "Thời hạn thẻ phải từ 1 đến 120 tháng")
    @Max(value = 120, message = "Thời hạn thẻ tối đa 120 tháng")
    @Column(name = "CARD_VALID_MONTHS", nullable = false)
    private Integer cardValidMonths = 12;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public Integer getMaxLoanBooks() {
        return maxLoanBooks;
    }

    public void setMaxLoanBooks(Integer maxLoanBooks) {
        this.maxLoanBooks = maxLoanBooks;
    }

    public Integer getLoanDays() {
        return loanDays;
    }

    public void setLoanDays(Integer loanDays) {
        this.loanDays = loanDays;
    }

    public Integer getMaxRenewals() {
        return maxRenewals;
    }

    public void setMaxRenewals(Integer maxRenewals) {
        this.maxRenewals = maxRenewals;
    }

    public Integer getRenewDays() {
        return renewDays;
    }

    public void setRenewDays(Integer renewDays) {
        this.renewDays = renewDays;
    }

    public Integer getMaxReservations() {
        return maxReservations;
    }

    public void setMaxReservations(Integer maxReservations) {
        this.maxReservations = maxReservations;
    }

    public BigDecimal getFinePerDay() {
        return finePerDay;
    }

    public void setFinePerDay(BigDecimal finePerDay) {
        this.finePerDay = finePerDay;
    }

    public Integer getCardValidMonths() {
        return cardValidMonths;
    }

    public void setCardValidMonths(Integer cardValidMonths) {
        this.cardValidMonths = cardValidMonths;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
