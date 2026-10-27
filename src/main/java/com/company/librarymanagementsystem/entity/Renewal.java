package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "RENEWALS", indexes = {
        @Index(name = "IDX_RENEWALS_ON_LOAN_ITEM", columnList = "LOAN_ITEM_ID")
})
@Entity
public class Renewal {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LOAN_ITEM_ID", nullable = false)
    private LoanItem loanItem;

    @NotNull
    @Column(name = "OLD_DUE_DATE", nullable = false)
    private LocalDate oldDueDate;

    @NotNull
    @Column(name = "NEW_DUE_DATE", nullable = false)
    private LocalDate newDueDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RENEWED_BY_ID", nullable = false)
    private User renewedBy;

    @NotNull
    @Column(name = "CHANNEL", nullable = false, length = 20)
    private String channel = RenewalChannel.COUNTER.getId();

    @NotNull
    @Column(name = "RENEWED_AT", nullable = false)
    private OffsetDateTime renewedAt = OffsetDateTime.now();

    @Column(name = "NOTE", length = 255)
    private String note;

    @InstanceName
    @DependsOnProperties({"loanItem", "newDueDate"})
    public String getInstanceName() {
        return String.format("Gia hạn đến %s", newDueDate);
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

    public LoanItem getLoanItem() {
        return loanItem;
    }

    public void setLoanItem(LoanItem loanItem) {
        this.loanItem = loanItem;
    }

    public LocalDate getOldDueDate() {
        return oldDueDate;
    }

    public void setOldDueDate(LocalDate oldDueDate) {
        this.oldDueDate = oldDueDate;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public User getRenewedBy() {
        return renewedBy;
    }

    public void setRenewedBy(User renewedBy) {
        this.renewedBy = renewedBy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public OffsetDateTime getRenewedAt() {
        return renewedAt;
    }

    public void setRenewedAt(OffsetDateTime renewedAt) {
        this.renewedAt = renewedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
