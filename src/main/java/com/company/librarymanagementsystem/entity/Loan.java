package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@JmixEntity
@Table(name = "LOANS", indexes = {
        @Index(name = "IDX_LOANS_ON_CODE", columnList = "LOAN_CODE", unique = true),
        @Index(name = "IDX_LOANS_ON_MEMBER", columnList = "MEMBER_ID, LOAN_DATE"),
        @Index(name = "IDX_LOANS_ON_LOAN_DATE", columnList = "LOAN_DATE")
})
@Entity
public class Loan {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull(message = "Số phiếu mượn không được để trống")
    @Column(name = "LOAN_CODE", nullable = false, length = 30, unique = true)
    private String loanCode;

    @NotNull(message = "Vui lòng chọn độc giả")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    @NotNull(message = "Người lập phiếu không được để trống")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ISSUED_BY_ID", nullable = false)
    private User issuedBy;

    @NotNull
    @Column(name = "LOAN_DATE", nullable = false)
    private OffsetDateTime loanDate = OffsetDateTime.now();

    @Column(name = "NOTE", length = 255)
    private String note;

    @Composition
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanItem> loanItems = new ArrayList<>();

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @InstanceName
    @DependsOnProperties({"loanCode", "loanDate"})
    public String getInstanceName() {
        return String.format("%s (%s)", loanCode != null ? loanCode : "",
                loanDate != null ? loanDate.toLocalDate().toString() : "");
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

    public String getLoanCode() {
        return loanCode;
    }

    public void setLoanCode(String loanCode) {
        this.loanCode = loanCode;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }

    public OffsetDateTime getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(OffsetDateTime loanDate) {
        this.loanDate = loanDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<LoanItem> getLoanItems() {
        return loanItems;
    }

    public void setLoanItems(List<LoanItem> loanItems) {
        this.loanItems = loanItems;
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
