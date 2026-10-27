package com.company.librarymanagementsystem.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@JmixEntity(name = "LibraryMember")
@Table(name = "MEMBERS", indexes = {
        @Index(name = "IDX_MEMBERS_ON_CODE", columnList = "MEMBER_CODE", unique = true),
        @Index(name = "IDX_MEMBERS_ON_USER_ID", columnList = "USER_ID", unique = true),
        @Index(name = "IDX_MEMBERS_ON_ID_NUMBER", columnList = "ID_NUMBER"),
        @Index(name = "IDX_MEMBERS_ON_STATUS", columnList = "STATUS")
})
@Entity(name = "LibraryMember")
public class Member {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", unique = true)
    private User user;

    @NotNull(message = "Họ và tên độc giả không được để trống")
    @Column(name = "FULL_NAME", nullable = false, length = 150)
    private String fullName;

    @NotNull(message = "Mã thẻ độc giả không được để trống")
    @Column(name = "MEMBER_CODE", nullable = false, length = 20, unique = true)
    private String memberCode;

    @NotNull(message = "Vui lòng chọn loại độc giả")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_TYPE_ID", nullable = false)
    private MemberType memberType;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "GENDER", length = 10)
    private String gender;

    @Column(name = "ID_NUMBER", length = 20)
    private String idNumber;

    @Column(name = "ADDRESS", length = 255)
    private String address;

    @NotNull(message = "Ngày cấp thẻ không được để trống")
    @Column(name = "CARD_ISSUED_DATE", nullable = false)
    private LocalDate cardIssuedDate;

    @NotNull(message = "Ngày hết hạn thẻ không được để trống")
    @Column(name = "CARD_EXPIRY_DATE", nullable = false)
    private LocalDate cardExpiryDate;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = MemberStatus.ACTIVE.getId();

    @Column(name = "STATUS_REASON", length = 255)
    private String statusReason;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;

    @InstanceName
    @DependsOnProperties({"memberCode", "fullName"})
    public String getInstanceName() {
        return String.format("[%s] %s", memberCode != null ? memberCode : "", fullName != null ? fullName : "");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Gender getGenderEnum() {
        return gender != null ? Gender.fromId(gender) : null;
    }

    public void setGenderEnum(Gender gender) {
        this.gender = gender != null ? gender.getId() : null;
    }

    public MemberStatus getStatusEnum() {
        return status != null ? MemberStatus.fromId(status) : null;
    }

    public void setStatusEnum(MemberStatus status) {
        this.status = status != null ? status.getId() : null;
    }

    public boolean isExpired() {
        return cardExpiryDate != null && cardExpiryDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return MemberStatus.ACTIVE.getId().equalsIgnoreCase(status) && !isExpired();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
        if (cardIssuedDate == null) {
            cardIssuedDate = LocalDate.now();
        }
        if (status == null) {
            status = MemberStatus.ACTIVE.getId();
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getCardIssuedDate() {
        return cardIssuedDate;
    }

    public void setCardIssuedDate(LocalDate cardIssuedDate) {
        this.cardIssuedDate = cardIssuedDate;
    }

    public LocalDate getCardExpiryDate() {
        return cardExpiryDate;
    }

    public void setCardExpiryDate(LocalDate cardExpiryDate) {
        this.cardExpiryDate = cardExpiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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
