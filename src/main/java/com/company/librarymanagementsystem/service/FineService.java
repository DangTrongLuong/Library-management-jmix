package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class FineService {

    private static final Logger log = LoggerFactory.getLogger(FineService.class);

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    public record FineStats(
            BigDecimal totalOutstanding,
            long unpaidFinesCount,
            BigDecimal collectedToday,
            long collectedTodayCount,
            BigDecimal totalWaived,
            long waivedCount,
            long indebtedMembersCount
    ) {}

    /**
     * Thu tiền phạt cho 1 khoản phạt cụ thể (L09, BR13)
     */
    @Transactional
    public Payment payFine(Long fineId, BigDecimal amount, PaymentMethod method, String transactionRef, String note) {
        if (fineId == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoản phạt cần thanh toán!");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0 VNĐ!");
        }
        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán!");
        }

        Fine fine = dataManager.load(Fine.class)
                .id(fineId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("loanItem", FetchPlan.BASE))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản phạt ID: " + fineId));

        if (FineStatus.PAID.getId().equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalStateException("Khoản phạt này đã được thanh toán đủ trước đó!");
        }
        if (FineStatus.WAIVED.getId().equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalStateException("Khoản phạt này đã được miễn giảm, không thể thu tiền!");
        }

        BigDecimal remaining = fine.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Số tiền thu (%,.0f VNĐ) vượt quá số tiền còn nợ của khoản phạt này (%,.0f VNĐ)!",
                    amount, remaining));
        }

        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        String receiptNo = generateReceiptNo();

        Payment payment = dataManager.create(Payment.class);
        payment.setReceiptNo(receiptNo);
        payment.setFine(fine);
        payment.setMember(fine.getMember());
        payment.setAmount(amount);
        payment.setMethod(method.getId());
        payment.setTransactionRef(transactionRef != null && !transactionRef.isBlank() ? transactionRef.trim() : null);
        payment.setReceivedBy(currentUser);
        payment.setPaidAt(OffsetDateTime.now());
        payment.setNote(note != null && !note.isBlank() ? note.trim() : null);

        BigDecimal newPaidAmount = (fine.getPaidAmount() != null ? fine.getPaidAmount() : BigDecimal.ZERO).add(amount);
        fine.setPaidAmount(newPaidAmount);

        // BR13: paid_amount = amount -> PAID, 0 < paid < amount -> PARTIAL
        if (newPaidAmount.compareTo(fine.getAmount()) >= 0) {
            fine.setStatus(FineStatus.PAID.getId());
        } else {
            fine.setStatus(FineStatus.PARTIAL.getId());
        }
        fine.setUpdatedAt(OffsetDateTime.now());

        dataManager.save(payment, fine);
        log.info("Thu tiền phạt thành công: Biên lai {}, Số tiền {}, Độc giả {}",
                receiptNo, amount, fine.getMember().getFullName());
        return payment;
    }

    /**
     * Thu toàn bộ các khoản nợ còn lại của một độc giả (Tiện ích quầy)
     */
    @Transactional
    public List<Payment> payAllFinesForMember(Long memberId, PaymentMethod method, String transactionRef, String note) {
        if (memberId == null) {
            throw new IllegalArgumentException("Vui lòng chọn độc giả!");
        }
        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán!");
        }

        List<Fine> unpaidFines = dataManager.load(Fine.class)
                .query("select f from Fine f where f.member.id = :memberId and f.status in ('UNPAID', 'PARTIAL') order by f.createdAt asc")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("loanItem", FetchPlan.BASE))
                .parameter("memberId", memberId)
                .list();

        if (unpaidFines.isEmpty()) {
            throw new IllegalStateException("Độc giả này hiện không có khoản nợ phạt nào cần thanh toán!");
        }

        List<Payment> payments = new ArrayList<>();
        for (Fine fine : unpaidFines) {
            BigDecimal remaining = fine.getRemainingAmount();
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                Payment p = payFine(fine.getId(), remaining, method, transactionRef, note);
                payments.add(p);
            }
        }
        return payments;
    }

    /**
     * Miễn / giảm khoản phạt (L10, BR14)
     */
    @Transactional
    public void waiveFine(Long fineId, String reason) {
        if (fineId == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoản phạt cần miễn giảm!");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do miễn giảm khoản phạt (bắt buộc)!");
        }

        Fine fine = dataManager.load(Fine.class)
                .id(fineId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản phạt ID: " + fineId));

        if (FineStatus.PAID.getId().equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalStateException("Khoản phạt này đã thanh toán đủ, không thể miễn giảm!");
        }
        if (FineStatus.WAIVED.getId().equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalStateException("Khoản phạt này đã được miễn giảm trước đó!");
        }

        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        fine.setStatus(FineStatus.WAIVED.getId());
        fine.setWaivedBy(currentUser);
        fine.setWaivedAt(OffsetDateTime.now());
        fine.setWaivedReason(reason.trim());
        fine.setUpdatedAt(OffsetDateTime.now());

        dataManager.save(fine);
        log.info("Miễn giảm khoản phạt ID {} cho độc giả '{}', Lý do: {}",
                fineId, fine.getMember().getFullName(), reason);
    }

    /**
     * L08: Xử lý báo mất sách (khi đang mượn)
     * BR09: Mất sách: tiền phạt = giá bìa x 1.5 + 20.000đ; bản sao chuyển LOST; loan_item chuyển LOST.
     */
    @Transactional
    public Fine reportLostBook(Long loanItemId, BigDecimal customFineAmount, String note) {
        if (loanItemId == null) {
            throw new IllegalArgumentException("Vui lòng chọn lượt mượn cần báo mất sách!");
        }

        LoanItem item = dataManager.load(LoanItem.class)
                .id(loanItemId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE)))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt mượn ID: " + loanItemId));

        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(item.getStatus())) {
            throw new IllegalStateException("Chỉ có thể báo mất sách cho cuốn sách đang trong trạng thái ĐANG MƯỢN!");
        }

        BookCopy copy = item.getBookCopy();
        Member member = item.getLoan().getMember();
        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        // Đổi trạng thái
        item.setStatus(LoanItemStatus.LOST.getId());
        item.setReturnedAt(OffsetDateTime.now());
        item.setReturnCondition(ReturnCondition.LOST.getId());
        item.setReceivedBy(currentUser);
        if (note != null && !note.isBlank()) {
            item.setNote(note.trim());
        }

        if (copy != null) {
            copy.setStatus(BookCopyStatus.LOST.getId());
        }

        // Tính tiền phạt BR09
        BigDecimal fineAmount;
        if (customFineAmount != null && customFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            fineAmount = customFineAmount;
        } else {
            BigDecimal price = (copy != null && copy.getBook() != null && copy.getBook().getPrice() != null)
                    ? copy.getBook().getPrice() : BigDecimal.valueOf(100000);
            fineAmount = price.multiply(BigDecimal.valueOf(1.5)).add(BigDecimal.valueOf(20000));
        }

        Fine fine = dataManager.create(Fine.class);
        fine.setMember(member);
        fine.setLoanItem(item);
        fine.setFineType(FineType.LOST.getId());
        fine.setAmount(fineAmount);
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID.getId());
        fine.setDescription(String.format("Bồi thường mất sách '%s' (Mã vạch: %s)",
                copy != null && copy.getBook() != null ? copy.getBook().getTitle() : "",
                copy != null ? copy.getBarcode() : ""));
        fine.setCreatedBy(currentUser);
        fine.setCreatedAt(OffsetDateTime.now());

        dataManager.save(item, copy, fine);
        log.info("Ghi nhận báo mất sách mã vạch '{}', độc giả '{}', Phạt bồi thường: {}",
                copy != null ? copy.getBarcode() : "", member.getFullName(), fineAmount);
        return fine;
    }

    /**
     * L08: Xử lý báo hỏng sách (khi đang mượn hoặc trả)
     */
    @Transactional
    public Fine reportDamagedBook(Long loanItemId, BigDecimal damageFineAmount, String damageDescription) {
        if (loanItemId == null) {
            throw new IllegalArgumentException("Vui lòng chọn lượt mượn cần báo hỏng sách!");
        }

        LoanItem item = dataManager.load(LoanItem.class)
                .id(loanItemId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE)))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt mượn ID: " + loanItemId));

        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(item.getStatus())) {
            throw new IllegalStateException("Chỉ có thể ghi nhận hỏng sách cho cuốn sách đang trong trạng thái ĐANG MƯỢN!");
        }

        BookCopy copy = item.getBookCopy();
        Member member = item.getLoan().getMember();
        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        item.setStatus(LoanItemStatus.RETURNED.getId());
        item.setReturnedAt(OffsetDateTime.now());
        item.setReturnCondition(ReturnCondition.DAMAGED.getId());
        item.setReceivedBy(currentUser);
        if (damageDescription != null && !damageDescription.isBlank()) {
            item.setNote(damageDescription.trim());
        }

        if (copy != null) {
            copy.setStatus(BookCopyStatus.DAMAGED.getId());
        }

        BigDecimal fineAmount;
        if (damageFineAmount != null && damageFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            fineAmount = damageFineAmount;
        } else {
            BigDecimal price = (copy != null && copy.getBook() != null && copy.getBook().getPrice() != null)
                    ? copy.getBook().getPrice() : BigDecimal.valueOf(100000);
            fineAmount = price.multiply(BigDecimal.valueOf(0.5));
        }

        Fine fine = dataManager.create(Fine.class);
        fine.setMember(member);
        fine.setLoanItem(item);
        fine.setFineType(FineType.DAMAGED.getId());
        fine.setAmount(fineAmount);
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID.getId());
        fine.setDescription(String.format("Bồi thường hỏng sách '%s' (Mã vạch: %s)%s",
                copy != null && copy.getBook() != null ? copy.getBook().getTitle() : "",
                copy != null ? copy.getBarcode() : "",
                damageDescription != null && !damageDescription.isBlank() ? " - " + damageDescription.trim() : ""));
        fine.setCreatedBy(currentUser);
        fine.setCreatedAt(OffsetDateTime.now());

        dataManager.save(item, copy, fine);
        log.info("Ghi nhận sách hỏng mã vạch '{}', độc giả '{}', Phạt: {}",
                copy != null ? copy.getBarcode() : "", member.getFullName(), fineAmount);
        return fine;
    }

    /**
     * Thống kê tiền phạt
     */
    public FineStats getFineStats() {
        BigDecimal totalOutstanding = dataManager.loadValue(
                "select coalesce(sum(f.amount - f.paidAmount), 0) from Fine f where f.status in ('UNPAID', 'PARTIAL')", BigDecimal.class)
                .one();

        Long unpaidCount = dataManager.loadValue(
                "select count(f) from Fine f where f.status in ('UNPAID', 'PARTIAL')", Long.class)
                .one();

        OffsetDateTime startOfToday = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        BigDecimal collectedToday = dataManager.loadValue(
                "select coalesce(sum(p.amount), 0) from Payment p where p.paidAt >= :today", BigDecimal.class)
                .parameter("today", startOfToday)
                .one();

        Long collectedTodayCount = dataManager.loadValue(
                "select count(p) from Payment p where p.paidAt >= :today", Long.class)
                .parameter("today", startOfToday)
                .one();

        BigDecimal totalWaived = dataManager.loadValue(
                "select coalesce(sum(f.amount), 0) from Fine f where f.status = 'WAIVED'", BigDecimal.class)
                .one();

        Long waivedCount = dataManager.loadValue(
                "select count(f) from Fine f where f.status = 'WAIVED'", Long.class)
                .one();

        Long indebtedMembers = dataManager.loadValue(
                "select count(distinct f.member.id) from Fine f where f.status in ('UNPAID', 'PARTIAL')", Long.class)
                .one();

        return new FineStats(
                totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO,
                unpaidCount != null ? unpaidCount : 0L,
                collectedToday != null ? collectedToday : BigDecimal.ZERO,
                collectedTodayCount != null ? collectedTodayCount : 0L,
                totalWaived != null ? totalWaived : BigDecimal.ZERO,
                waivedCount != null ? waivedCount : 0L,
                indebtedMembers != null ? indebtedMembers : 0L
        );
    }

    /**
     * Sinh số biên lai duy nhất dạng: REC-YYYYMMDD-XXXX
     */
    private synchronized String generateReceiptNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long countToday = dataManager.loadValue(
                "select count(p) from Payment p where p.receiptNo like :prefix", Long.class)
                .parameter("prefix", "REC-" + dateStr + "-%")
                .one();
        long nextSeq = (countToday != null ? countToday : 0) + 1;
        return String.format("REC-%s-%04d", dateStr, nextSeq);
    }
}
