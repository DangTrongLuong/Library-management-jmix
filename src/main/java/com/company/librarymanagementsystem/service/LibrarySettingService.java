package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.LibrarySetting;
import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class LibrarySettingService {

    private static final Logger log = LoggerFactory.getLogger(LibrarySettingService.class);

    private final DataManager dataManager;

    public LibrarySettingService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Lấy giá trị chuỗi của một tham số theo key, nếu không có trả về defaultValue
     */
    public String getString(String key, String defaultValue) {
        if (key == null || key.isBlank()) return defaultValue;
        try {
            Optional<LibrarySetting> setting = dataManager.load(LibrarySetting.class)
                    .query("select s from LibrarySetting s where s.key = :key")
                    .parameter("key", key.trim())
                    .optional();

            if (setting.isPresent() && setting.get().getValue() != null && !setting.get().getValue().isBlank()) {
                return setting.get().getValue().trim();
            }
        } catch (Exception e) {
            log.warn("Không thể tải tham số '{}' từ CSDL: {}", key, e.getMessage());
        }
        return defaultValue;
    }

    /**
     * Lấy giá trị số nguyên Integer
     */
    public int getInt(String key, int defaultValue) {
        String val = getString(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("Tham số '{}' có giá trị '{}' không phải số nguyên hợp lệ, dùng mặc định: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Lấy giá trị số thực BigDecimal (dùng cho tiền tệ, tỷ lệ)
     */
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String val = getString(key, null);
        if (val != null) {
            try {
                return new BigDecimal(val);
            } catch (Exception e) {
                log.warn("Tham số '{}' có giá trị '{}' không phải BigDecimal hợp lệ, dùng mặc định: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    // =========================================================================
    // CÁC HÀM GETTER NGHIỆP VỤ ĐÃ ĐƯỢC CHUẨN HÓA VÀ CÓ GIÁ TRỊ DỰ PHÒNG CHUẨN
    // =========================================================================

    /**
     * Ngưỡng nợ phạt tối đa cho phép trước khi chặn mượn sách (VNĐ)
     * Key: MAX_UNPAID_FINE_THRESHOLD (Mặc định: 50.000 VNĐ)
     */
    public BigDecimal getMaxUnpaidFineThreshold() {
        return getBigDecimal("MAX_UNPAID_FINE_THRESHOLD", BigDecimal.valueOf(50000));
    }

    /**
     * Số ngày ân hạn nộp phạt (0 = tính phạt ngay từ ngày trễ đầu tiên)
     * Key: FINE_GRACE_DAYS (Mặc định: 0 ngày)
     */
    public int getFineGraceDays() {
        return getInt("FINE_GRACE_DAYS", 0);
    }

    /**
     * Trần phạt quá hạn tối đa (% giá bìa sách)
     * Key: FINE_CAP_RATIO (Mặc định: 100%)
     */
    public BigDecimal getFineCapRatio() {
        return getBigDecimal("FINE_CAP_RATIO", BigDecimal.valueOf(100));
    }

    /**
     * Hệ số nhân với giá bìa sách để tính tiền bồi thường khi làm mất sách
     * Key: LOST_FEE_MULTIPLIER (Mặc định: 1.5)
     */
    public BigDecimal getLostFeeMultiplier() {
        return getBigDecimal("LOST_FEE_MULTIPLIER", BigDecimal.valueOf(1.5));
    }

    /**
     * Phí phụ thu xử lý biên bản và hành chính khi làm mất sách (VNĐ)
     * Key: LOST_PROCESSING_FEE (Mặc định: 20.000 VNĐ)
     */
    public BigDecimal getLostProcessingFee() {
        return getBigDecimal("LOST_PROCESSING_FEE", BigDecimal.valueOf(20000));
    }

    /**
     * Số ngày giữ chỗ sách đặt trước khi sách đã sẵn sàng (READY)
     * Key: HOLD_EXPIRY_DAYS (Mặc định: 3 ngày)
     */
    public int getHoldExpiryDays() {
        return getInt("HOLD_EXPIRY_DAYS", 3);
    }

    /**
     * Số ngày gửi thông báo nhắc hạn trước khi đến hạn trả sách (ngày)
     * Key: DUE_REMINDER_DAYS_BEFORE (Mặc định: 2 ngày)
     */
    public int getDueReminderDaysBefore() {
        return getInt("DUE_REMINDER_DAYS_BEFORE", 2);
    }
}
