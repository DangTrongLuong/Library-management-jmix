package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "RESERVATIONS", indexes = {
        @Index(name = "IDX_RESERVATIONS_ON_BOOK_STATUS", columnList = "BOOK_ID, STATUS, RESERVED_AT"),
        @Index(name = "IDX_RESERVATIONS_ON_MEMBER_STATUS", columnList = "MEMBER_ID, STATUS"),
        @Index(name = "IDX_RESERVATIONS_ON_COPY", columnList = "COPY_ID"),
        @Index(name = "IDX_RESERVATIONS_ON_HOLD_EXPIRES", columnList = "STATUS, HOLD_EXPIRES_AT")
})
@Entity
public class Reservation {

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

    @NotNull(message = "Vui lòng chọn đầu sách")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BOOK_ID", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COPY_ID")
    private BookCopy bookCopy;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = ReservationStatus.PENDING.getId();

    @NotNull
    @Column(name = "RESERVED_AT", nullable = false)
    private OffsetDateTime reservedAt = OffsetDateTime.now();

    @Column(name = "NOTIFIED_AT")
    private OffsetDateTime notifiedAt;

    @Column(name = "HOLD_EXPIRES_AT")
    private OffsetDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FULFILLED_LOAN_ITEM_ID")
    private LoanItem fulfilledLoanItem;

    @Column(name = "CANCEL_REASON", length = 255)
    private String cancelReason;

    @Column(name = "CANCELLED_AT")
    private OffsetDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CANCELLED_BY_ID")
    private User cancelledBy;

    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
        if (reservedAt == null) {
            reservedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @InstanceName
    @DependsOnProperties({"member", "book", "status"})
    public String getInstanceName() {
        String memberName = member != null ? member.getFullName() : "";
        String bookTitle = book != null ? book.getTitle() : "";
        return String.format("[%s] %s - %s", status, bookTitle, memberName);
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

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(OffsetDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public OffsetDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(OffsetDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public OffsetDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(OffsetDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public LoanItem getFulfilledLoanItem() {
        return fulfilledLoanItem;
    }

    public void setFulfilledLoanItem(LoanItem fulfilledLoanItem) {
        this.fulfilledLoanItem = fulfilledLoanItem;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public User getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(User cancelledBy) {
        this.cancelledBy = cancelledBy;
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
}
