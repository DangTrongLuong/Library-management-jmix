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
import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "FINES", indexes = {
        @Index(name = "IDX_FINES_ON_MEMBER_STATUS", columnList = "MEMBER_ID, STATUS"),
        @Index(name = "IDX_FINES_ON_LOAN_ITEM", columnList = "LOAN_ITEM_ID"),
        @Index(name = "IDX_FINES_ON_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_FINES_STATUS_CREATED", columnList = "STATUS, CREATED_AT")
})
@Entity
public class Fine {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull(message = "Vui lòng chọn độc giả")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_ITEM_ID")
    private LoanItem loanItem;

    @NotNull
    @Column(name = "FINE_TYPE", nullable = false, length = 20)
    private String fineType = FineType.OVERDUE.getId();

    @Column(name = "DAYS_OVERDUE")
    private Integer daysOverdue;

    @NotNull(message = "Số tiền phạt không được để trống")
    @Column(name = "AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "PAID_AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = FineStatus.UNPAID.getId();

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY_ID")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WAIVED_BY_ID")
    private User waivedBy;

    @Column(name = "WAIVED_AT")
    private OffsetDateTime waivedAt;

    @Column(name = "WAIVED_REASON", length = 255)
    private String waivedReason;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @InstanceName
    @DependsOnProperties({"fineType", "amount", "status"})
    public String getInstanceName() {
        return String.format("%s - %s VNĐ (%s)", fineType, amount, status);
    }

    public BigDecimal getRemainingAmount() {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        return amount.subtract(paid);
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

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public LoanItem getLoanItem() {
        return loanItem;
    }

    public void setLoanItem(LoanItem loanItem) {
        this.loanItem = loanItem;
    }

    public String getFineType() {
        return fineType;
    }

    public void setFineType(String fineType) {
        this.fineType = fineType;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Integer daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getWaivedBy() {
        return waivedBy;
    }

    public void setWaivedBy(User waivedBy) {
        this.waivedBy = waivedBy;
    }

    public OffsetDateTime getWaivedAt() {
        return waivedAt;
    }

    public void setWaivedAt(OffsetDateTime waivedAt) {
        this.waivedAt = waivedAt;
    }

    public String getWaivedReason() {
        return waivedReason;
    }

    public void setWaivedReason(String waivedReason) {
        this.waivedReason = waivedReason;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
