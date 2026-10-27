package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "PAYMENTS", indexes = {
        @Index(name = "IDX_PAYMENTS_ON_FINE", columnList = "FINE_ID"),
        @Index(name = "IDX_PAYMENTS_ON_MEMBER", columnList = "MEMBER_ID"),
        @Index(name = "IDX_PAYMENTS_ON_PAID_AT", columnList = "PAID_AT")
})
@Entity
public class Payment {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull(message = "Số biên lai không được để trống")
    @Column(name = "RECEIPT_NO", nullable = false, unique = true, length = 30)
    private String receiptNo;

    @NotNull(message = "Vui lòng chọn khoản phạt")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FINE_ID", nullable = false)
    private Fine fine;

    @NotNull(message = "Vui lòng chọn độc giả")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Column(name = "AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "METHOD", nullable = false, length = 20)
    private String method = PaymentMethod.CASH.getId();

    @Column(name = "TRANSACTION_REF", length = 100)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECEIVED_BY_ID")
    private User receivedBy;

    @NotNull
    @Column(name = "PAID_AT", nullable = false)
    private OffsetDateTime paidAt = OffsetDateTime.now();

    @Column(name = "NOTE", length = 255)
    private String note;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (paidAt == null) {
            paidAt = OffsetDateTime.now();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @InstanceName
    @DependsOnProperties({"receiptNo", "amount", "method"})
    public String getInstanceName() {
        return String.format("%s - %,.0f VNĐ (%s)", receiptNo, amount, method);
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

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Fine getFine() {
        return fine;
    }

    public void setFine(Fine fine) {
        this.fine = fine;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public User getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(User receivedBy) {
        this.receivedBy = receivedBy;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
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
}
