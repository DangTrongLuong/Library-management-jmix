package com.company.librarymanagementsystem.entity;

import io.jmix.core.annotation.DeletedBy;
import io.jmix.core.annotation.DeletedDate;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.List;

@JmixEntity
@Table(name = "LOCATIONS", indexes = {
        @Index(name = "IDX_LOCATIONS_ON_CODE", columnList = "CODE", unique = true),
        @Index(name = "IDX_LOCATIONS_ON_STATUS", columnList = "STATUS")
})
@Entity
public class Location {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @NotNull(message = "Vui lòng nhập mã vị trí kệ")
    @Size(min = 2, max = 30, message = "Mã vị trí kệ phải từ 2 đến 30 ký tự")
    @Column(name = "CODE", nullable = false, length = 30, unique = true)
    private String code;

    @NotNull(message = "Vui lòng nhập tên vị trí kệ")
    @Size(min = 2, max = 100, message = "Tên vị trí kệ phải từ 2 đến 100 ký tự")
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "FLOOR")
    private Integer floor;

    @Size(max = 50, message = "Tên khu vực không được vượt quá 50 ký tự")
    @Column(name = "ROOM_OR_ZONE", length = 50)
    private String roomOrZone;

    @Size(max = 30, message = "Mã/tên dãy kệ không được vượt quá 30 ký tự")
    @Column(name = "AISLE", length = 30)
    private String aisle;

    @Min(value = 1, message = "Ngăn kệ phải bắt đầu từ 1")
    @Max(value = 50, message = "Ngăn kệ tối đa là 50")
    @Column(name = "SHELF_LEVEL")
    private Integer shelfLevel;

    @Min(value = 1, message = "Sức chứa tối đa phải lớn hơn 0")
    @Column(name = "MAX_CAPACITY")
    private Integer maxCapacity;

    @NotNull(message = "Vui lòng chọn trạng thái kệ")
    @Size(max = 20, message = "Trạng thái không quá 20 ký tự")
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @OneToMany(mappedBy = "location")
    private List<BookCopy> copies;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @DeletedBy
    @Column(name = "DELETED_BY", length = 50)
    private String deletedBy;

    @DeletedDate
    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
        if (version == null) {
            version = 1;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @InstanceName
    @DependsOnProperties({ "code", "name" })
    public String getDisplayName() {
        String c = (code != null && !code.isBlank()) ? code.trim() : "KỆ";
        String n = (name != null && !name.isBlank()) ? name.trim() : "";
        return String.format("[%s] %s", c, n).trim();
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

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getRoomOrZone() {
        return roomOrZone;
    }

    public void setRoomOrZone(String roomOrZone) {
        this.roomOrZone = roomOrZone;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public Integer getShelfLevel() {
        return shelfLevel;
    }

    public void setShelfLevel(Integer shelfLevel) {
        this.shelfLevel = shelfLevel;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
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

    public List<BookCopy> getCopies() {
        return copies;
    }

    public void setCopies(List<BookCopy> copies) {
        this.copies = copies;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
