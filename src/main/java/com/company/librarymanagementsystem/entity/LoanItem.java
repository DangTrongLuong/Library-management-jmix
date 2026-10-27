package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@JmixEntity
@Table(name = "LOAN_ITEMS", indexes = {
        @Index(name = "IDX_LOAN_ITEMS_ON_LOAN_ID", columnList = "LOAN_ID"),
        @Index(name = "IDX_LOAN_ITEMS_ON_COPY_STATUS", columnList = "COPY_ID, STATUS"),
        @Index(name = "IDX_LOAN_ITEMS_ON_STATUS_DUE", columnList = "STATUS, DUE_DATE"),
        @Index(name = "IDX_LOAN_ITEMS_STATUS_ID", columnList = "STATUS, ID")
})
@Entity
public class LoanItem {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LOAN_ID", nullable = false)
    private Loan loan;

    @NotNull(message = "Vui lòng chọn bản sao sách")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COPY_ID", nullable = false)
    private BookCopy bookCopy;

    @NotNull(message = "Hạn trả sách không được để trống")
    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "RETURNED_AT")
    private OffsetDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECEIVED_BY_ID")
    private User receivedBy;

    @NotNull
    @Column(name = "RENEW_COUNT", nullable = false)
    private Integer renewCount = 0;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = LoanItemStatus.BORROWED.getId();

    @Column(name = "RETURN_CONDITION", length = 20)
    private String returnCondition;

    @Column(name = "NOTE", length = 255)
    private String note;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @InstanceName
    @DependsOnProperties({"bookCopy", "dueDate", "status"})
    public String getInstanceName() {
        String barcode = (bookCopy != null && bookCopy.getBarcode() != null) ? bookCopy.getBarcode() : "";
        return String.format("%s [Hạn: %s - %s]", barcode, dueDate, status);
    }

    @JmixProperty
    @DependsOnProperties({"status", "dueDate"})
    public Boolean getIsOverdue() {
        if (!LoanItemStatus.BORROWED.getId().equals(status) || dueDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    @JmixProperty
    @DependsOnProperties({"status", "dueDate"})
    public Long getDaysOverdue() {
        if (!LoanItemStatus.BORROWED.getId().equals(status) || dueDate == null) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, today);
        }
        return 0L;
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

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OffsetDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(OffsetDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public User getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(User receivedBy) {
        this.receivedBy = receivedBy;
    }

    public Integer getRenewCount() {
        return renewCount;
    }

    public void setRenewCount(Integer renewCount) {
        this.renewCount = renewCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReturnCondition() {
        return returnCondition;
    }

    public void setReturnCondition(String returnCondition) {
        this.returnCondition = returnCondition;
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
}
