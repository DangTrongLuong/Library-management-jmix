package com.company.librarymanagementsystem.service;

import io.jmix.data.impl.NumberIdCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Tự động đồng bộ hóa bảng SYS_SEQUENCE của Jmix với MAX(ID) thực tế trong cơ sở dữ liệu.
 * Khắc phục triệt để lỗi khi thêm mới bản ghi (ví dụ: Đầu sách, Tác giả, Nhà xuất bản...)
 * Jmix sinh ID trùng với ID đã được nạp sẵn qua Liquibase seed data,
 * dẫn đến việc màn hình "Thêm mới" tự động load nhầm một bản ghi cũ đã có trong database.
 */
@Component
public class DatabaseSequenceSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSequenceSynchronizer.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private NumberIdCache numberIdCache;

    public DatabaseSequenceSynchronizer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final Map<String, String> ENTITY_TABLE_MAP = Map.ofEntries(
            Map.entry("Book", "books"),
            Map.entry("BookCopy", "book_copies"),
            Map.entry("Author", "authors"),
            Map.entry("Category", "categories"),
            Map.entry("Publisher", "publishers"),
            Map.entry("Location", "locations"),
            Map.entry("LibraryMember", "members"),
            Map.entry("MemberType", "member_types"),
            Map.entry("Loan", "loans"),
            Map.entry("LoanItem", "loan_items"),
            Map.entry("Fine", "fines"),
            Map.entry("Payment", "payments"),
            Map.entry("Reservation", "reservations"),
            Map.entry("Renewal", "renewals"),
            Map.entry("AuditLog", "audit_logs"),
            Map.entry("Role", "roles"),
            Map.entry("Permission", "permissions")
    );

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeSequences() {
        log.info("Bắt đầu kiểm tra và đồng bộ hóa SYS_SEQUENCE với dữ liệu hiện có trong Database...");
        int updatedCount = 0;

        for (Map.Entry<String, String> entry : ENTITY_TABLE_MAP.entrySet()) {
            String entityName = entry.getKey();
            String tableName = entry.getValue();
            String seqName = "seq_id_" + entityName;

            try {
                Long maxId = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM " + tableName, Long.class);
                if (maxId == null) {
                    maxId = 0L;
                }

                // Cần đảm bảo sequence hiện tại lớn hơn maxId ít nhất 1000 và chia hết cho 100
                long safeSequenceValue = Math.max(1000L, ((maxId / 100L) + 10L) * 100L);

                Long currentSeq = null;
                try {
                    currentSeq = jdbcTemplate.queryForObject(
                            "SELECT CURR_VALUE FROM sys_sequence WHERE NAME = ?",
                            Long.class, seqName);
                } catch (Exception ignored) {
                }

                if (currentSeq == null) {
                    jdbcTemplate.update(
                            "INSERT INTO sys_sequence (NAME, INCREMENT, CURR_VALUE) VALUES (?, 100, ?)",
                            seqName, safeSequenceValue);
                    log.info("Tạo mới sequence '{}' với giá trị an toàn: {} (Max ID trong {}: {})",
                            seqName, safeSequenceValue, tableName, maxId);
                    updatedCount++;
                } else if (currentSeq <= maxId) {
                    jdbcTemplate.update(
                            "UPDATE sys_sequence SET CURR_VALUE = ? WHERE NAME = ?",
                            safeSequenceValue, seqName);
                    log.warn("Sequence '{}' hiện tại ({}) <= Max ID ({}) trong bảng {}. Đã nâng lên: {}",
                            seqName, currentSeq, maxId, tableName, safeSequenceValue);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.debug("Bỏ qua đồng bộ bảng {}: {}", tableName, e.getMessage());
            }
        }

        if (numberIdCache != null) {
            numberIdCache.reset();
            log.info("Đã reset NumberIdCache để nạp dải ID an toàn mới từ SYS_SEQUENCE.");
        }

        log.info("Đồng bộ SYS_SEQUENCE hoàn tất (Cập nhật {} sequences).", updatedCount);
    }
}
