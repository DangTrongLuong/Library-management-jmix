package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookCopyService {

    private static final Logger log = LoggerFactory.getLogger(BookCopyService.class);

    private final DataManager dataManager;

    public BookCopyService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Tự động sinh mã vạch tiếp theo cho đầu sách theo quy ước BK{bookId_padded}-{seq_padded}
     */
    public String generateNextBarcode(Book book) {
        long bookId = book.getId() != null ? book.getId() : 0L;
        String prefix = String.format("BK%06d-", bookId);

        // Đếm số lượng bản sao hiện có của đầu sách để tính số thứ tự
        List<String> existingBarcodes = dataManager.loadValue(
                        "select c.barcode from BookCopy c where c.book = :book", String.class)
                .parameter("book", book)
                .list();

        int maxSeq = 0;
        for (String bc : existingBarcodes) {
            if (bc != null && bc.startsWith(prefix)) {
                try {
                    String seqStr = bc.substring(prefix.length());
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int nextSeq = Math.max(maxSeq + 1, existingBarcodes.size() + 1);
        String candidate = String.format("%s%02d", prefix, nextSeq);

        // Đảm bảo không trùng với bất kỳ bản sao nào trong toàn hệ thống
        while (isBarcodeExists(candidate)) {
            nextSeq++;
            candidate = String.format("%s%02d", prefix, nextSeq);
        }

        return candidate;
    }

    public boolean isBarcodeExists(String barcode) {
        Long count = dataManager.loadValue(
                        "select count(c) from BookCopy c where c.barcode = :barcode", Long.class)
                .parameter("barcode", barcode)
                .one();
        return count != null && count > 0;
    }

    /**
     * Chức năng C05: Nhập bản sao hàng loạt cho một đầu sách
     */
    @Transactional
    public List<BookCopy> createBulkCopies(Book book,
                                           int quantity,
                                           Location location,
                                           BookCopyCondition condition,
                                           LocalDate acquiredDate,
                                           BigDecimal acquisitionPrice,
                                           String acquisitionSource,
                                           String note) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bản sao nhập về phải lớn hơn 0");
        }

        List<BookCopy> createdList = new ArrayList<>();
        LocalDate acqDate = acquiredDate != null ? acquiredDate : LocalDate.now();
        BigDecimal price = acquisitionPrice != null ? acquisitionPrice : (book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO);
        String condId = condition != null ? condition.getId() : BookCopyCondition.GOOD.getId();

        for (int i = 0; i < quantity; i++) {
            BookCopy copy = dataManager.create(BookCopy.class);
            copy.setBook(book);
            copy.setLocation(location);
            copy.setStatus(BookCopyStatus.AVAILABLE.getId());
            copy.setCondition(condId);
            copy.setAcquiredDate(acqDate);
            copy.setAcquisitionPrice(price);
            copy.setAcquisitionSource(acquisitionSource);
            copy.setNote(note);

            String barcode = generateNextBarcode(book);
            copy.setBarcode(barcode);

            dataManager.save(copy);
            createdList.add(copy);
        }

        log.info("Đã tạo thành công {} bản sao cho đầu sách '{}' (ID: {})", quantity, book.getTitle(), book.getId());
        return createdList;
    }

    /**
     * Chức năng C06: Chuyển vị trí kệ hàng loạt cho danh sách bản sao
     */
    @Transactional
    public void relocateCopies(List<BookCopy> copies, Location newLocation) {
        if (copies == null || copies.isEmpty() || newLocation == null) {
            return;
        }

        for (BookCopy copy : copies) {
            copy.setLocation(newLocation);
            dataManager.save(copy);
        }

        log.info("Đã chuyển {} bản sao sang vị trí kệ '{}' (Mã: {})", copies.size(), newLocation.getName(), newLocation.getCode());
    }

    /**
     * Cập nhật tình trạng hoặc đưa vào bảo trì / đề xuất thanh lý (C07)
     */
    @Transactional
    public void updateStatus(BookCopy copy, BookCopyStatus newStatus, String note) {
        if ("BORROWED".equals(copy.getStatus()) && newStatus == BookCopyStatus.AVAILABLE) {
            throw new IllegalStateException("Không thể chuyển trực tiếp bản sao đang mượn về AVAILABLE. Vui lòng thực hiện qua nghiệp vụ Trả sách.");
        }
        copy.setStatus(newStatus.getId());
        if (note != null && !note.isBlank()) {
            copy.setNote(note);
        }
        dataManager.save(copy);
    }
}
