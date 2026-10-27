package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.security.CurrentAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final DataManager dataManager;
    private final CurrentAuthentication currentAuthentication;
    private final ReservationService reservationService;
    private final LibrarySettingService settingService;

    public LoanService(DataManager dataManager,
                       CurrentAuthentication currentAuthentication,
                       @org.springframework.context.annotation.Lazy ReservationService reservationService,
                       LibrarySettingService settingService) {
        this.dataManager = dataManager;
        this.currentAuthentication = currentAuthentication;
        this.reservationService = reservationService;
        this.settingService = settingService;
    }

    /**
     * Lấy số sách đang mượn của độc giả
     */
    public int countCurrentLoans(Long memberId) {
        if (memberId == null) return 0;
        Long count = dataManager.loadValue(
                "select count(li) from LoanItem li where li.loan.member.id = :memberId and li.status = 'BORROWED'",
                Long.class)
                .parameter("memberId", memberId)
                .one();
        return count != null ? count.intValue() : 0;
    }

    /**
     * Lấy số sách quá hạn của độc giả
     */
    public int countOverdueLoans(Long memberId) {
        if (memberId == null) return 0;
        Long count = dataManager.loadValue(
                "select count(li) from LoanItem li where li.loan.member.id = :memberId and li.status = 'BORROWED' and li.dueDate < :today",
                Long.class)
                .parameter("memberId", memberId)
                .parameter("today", LocalDate.now())
                .one();
        return count != null ? count.intValue() : 0;
    }

    /**
     * Lấy tổng tiền phạt chưa thanh toán của độc giả
     */
    public BigDecimal calculateUnpaidFines(Long memberId) {
        if (memberId == null) return BigDecimal.ZERO;
        List<Fine> fines = dataManager.load(Fine.class)
                .query("select f from Fine f where f.member.id = :memberId and f.status in ('UNPAID', 'PARTIAL')")
                .parameter("memberId", memberId)
                .list();

        BigDecimal total = BigDecimal.ZERO;
        for (Fine f : fines) {
            BigDecimal remaining = f.getRemainingAmount();
            if (remaining != null) {
                total = total.add(remaining);
            }
        }
        return total;
    }

    /**
     * L04: Lập phiếu mượn sách tại quầy
     */
    @Transactional
    public Loan createLoan(Member member, List<BookCopy> copies, User issuedBy, String note) {
        if (member == null) {
            throw new IllegalArgumentException("Vui lòng chọn độc giả!");
        }
        if (copies == null || copies.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sách mượn không được để trống!");
        }

        // Tải lại member kèm memberType đầy đủ
        Member reloadedMember = dataManager.load(Member.class)
                .id(member.getId())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE))
                .one();

        MemberType type = reloadedMember.getMemberType();
        int maxLoans = (type != null && type.getMaxLoanBooks() != null) ? type.getMaxLoanBooks() : 5;
        int currentLoans = countCurrentLoans(reloadedMember.getId());

        if (currentLoans + copies.size() > maxLoans) {
            throw new IllegalStateException(String.format(
                    "Độc giả đang mượn %d cuốn, chỉ được mượn thêm tối đa %d cuốn (Hạn mức: %d cuốn)!",
                    currentLoans, Math.max(0, maxLoans - currentLoans), maxLoans));
        }

        // Kiểm tra tiền phạt nợ theo ngưỡng cấu hình hệ thống (MAX_UNPAID_FINE_THRESHOLD)
        BigDecimal unpaidFines = calculateUnpaidFines(reloadedMember.getId());
        BigDecimal maxFineThreshold = settingService.getMaxUnpaidFineThreshold();
        if (unpaidFines.compareTo(maxFineThreshold) > 0) {
            throw new IllegalStateException(String.format(
                    "Độc giả đang có tiền phạt chưa thanh toán (%,.0f VNĐ) vượt quá ngưỡng cho phép (%,.0f VNĐ). Cần nộp phạt trước khi mượn tiếp!",
                    unpaidFines, maxFineThreshold));
        }

        // Kiểm tra sách quá hạn
        int overdueLoans = countOverdueLoans(reloadedMember.getId());
        if (overdueLoans > 0) {
            throw new IllegalStateException(String.format(
                    "Độc giả đang có %d cuốn sách quá hạn chưa trả. Cần trả sách trước khi mượn tiếp!",
                    overdueLoans));
        }

        // Tạo mã phiếu mượn tự động
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PM" + dateStr + "-";
        Long nextNum = dataManager.loadValue(
                "select count(l) from Loan l where l.loanCode like :pattern", Long.class)
                .parameter("pattern", prefix + "%")
                .one();
        long seq = (nextNum != null ? nextNum : 0) + 1;
        String loanCode = String.format("%s%04d", prefix, seq);

        Loan loan = dataManager.create(Loan.class);
        loan.setLoanCode(loanCode);
        loan.setMember(reloadedMember);
        loan.setIssuedBy(issuedBy != null ? issuedBy : (User) currentAuthentication.getUser());
        loan.setLoanDate(OffsetDateTime.now());
        loan.setNote(note);

        int loanDays = (type != null && type.getLoanDays() != null) ? type.getLoanDays() : 14;
        LocalDate dueDate = LocalDate.now().plusDays(loanDays);

        List<Object> entitiesToSave = new ArrayList<>();
        entitiesToSave.add(loan);

        List<LoanItem> loanItems = new ArrayList<>();
        for (BookCopy copy : copies) {
            // Tải lại bản sao kiểm tra trạng thái
            BookCopy reloadedCopy = dataManager.load(BookCopy.class)
                    .id(copy.getId())
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))
                    .one();

            String reservedOther = reservationService.checkReservedForOtherMember(reloadedCopy, reloadedMember);
            if (reservedOther != null) {
                throw new IllegalStateException(String.format(
                        "Bản sao sách mã '%s' (%s) hiện đang được giữ chỗ cho %s!",
                        reloadedCopy.getBarcode(),
                        reloadedCopy.getBook() != null ? reloadedCopy.getBook().getTitle() : "",
                        reservedOther));
            }

            if (!BookCopyStatus.AVAILABLE.getId().equalsIgnoreCase(reloadedCopy.getStatus())
                    && !BookCopyStatus.RESERVED.getId().equalsIgnoreCase(reloadedCopy.getStatus())) {
                throw new IllegalStateException(String.format(
                        "Bản sao sách mã '%s' (%s) không ở trạng thái sẵn sàng để mượn (Hiện tại: %s)!",
                        reloadedCopy.getBarcode(),
                        reloadedCopy.getBook() != null ? reloadedCopy.getBook().getTitle() : "",
                        reloadedCopy.getStatus()));
            }

            LoanItem item = dataManager.create(LoanItem.class);
            item.setLoan(loan);
            item.setBookCopy(reloadedCopy);
            item.setDueDate(dueDate);
            item.setRenewCount(0);
            item.setStatus(LoanItemStatus.BORROWED.getId());
            item.setCreatedAt(OffsetDateTime.now());

            // Đổi trạng thái bản sao sang BORROWED
            reloadedCopy.setStatus(BookCopyStatus.BORROWED.getId());

            loanItems.add(item);
            entitiesToSave.add(item);
            entitiesToSave.add(reloadedCopy);
        }

        loan.setLoanItems(loanItems);
        dataManager.save(entitiesToSave.toArray());

        // Hoàn tất các lượt đặt trước nếu bản sao vừa mượn đang được giữ chỗ cho độc giả này
        for (LoanItem item : loanItems) {
            reservationService.fulfillReservationForLoanItem(item.getBookCopy(), reloadedMember, item);
        }

        log.info("Lập phiếu mượn thành công: {} cho độc giả {}", loanCode, reloadedMember.getMemberCode());
        return loan;
    }

    /**
     * L05: Nhận trả sách tại quầy
     */
    @Transactional
    public ReturnResult returnBookCopy(String barcode, ReturnCondition condition, User receivedBy, String note) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập hoặc quét mã vạch sách!");
        }

        // Tìm bản sao
        Optional<BookCopy> optCopy = dataManager.load(BookCopy.class)
                .query("select bc from BookCopy bc where bc.barcode = :barcode")
                .parameter("barcode", barcode.trim())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))
                .optional();

        if (optCopy.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy bản sao sách có mã vạch: " + barcode);
        }

        BookCopy copy = optCopy.get();

        // Tìm LoanItem đang BORROWED
        Optional<LoanItem> optItem = dataManager.load(LoanItem.class)
                .query("select li from LoanItem li where li.bookCopy.id = :copyId and li.status = 'BORROWED'")
                .parameter("copyId", copy.getId())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE)
                                .add("member", memberPlan -> memberPlan.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE)))
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE)))
                .optional();

        if (optItem.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Cuốn sách '%s' (Mã vạch: %s) hiện không ở trong lượt mượn nào!",
                    copy.getBook() != null ? copy.getBook().getTitle() : "", copy.getBarcode()));
        }

        LoanItem loanItem = optItem.get();
        Member member = loanItem.getLoan().getMember();
        User cashier = receivedBy != null ? receivedBy : (User) currentAuthentication.getUser();

        loanItem.setReturnedAt(OffsetDateTime.now());
        loanItem.setReceivedBy(cashier);
        loanItem.setReturnCondition(condition.getId());
        if (note != null && !note.trim().isEmpty()) {
            loanItem.setNote(note.trim());
        }

        List<Object> toSave = new ArrayList<>();
        ReturnResult result = new ReturnResult();
        result.setBarcode(barcode);
        result.setBookTitle(copy.getBook() != null ? copy.getBook().getTitle() : "");
        result.setMemberCode(member != null ? member.getMemberCode() : "");
        result.setMemberName(member != null ? member.getFullName() : "");
        result.setCondition(condition);

        BigDecimal fineTotal = BigDecimal.ZERO;

        // Xử lý theo tình trạng
        if (condition == ReturnCondition.LOST) {
            loanItem.setStatus(LoanItemStatus.LOST.getId());
            copy.setStatus(BookCopyStatus.LOST.getId());

            // Phạt mất sách: Giá bìa * hệ số đền bù + phí xử lý (Cấu hình hệ thống: LOST_FEE_MULTIPLIER, LOST_PROCESSING_FEE)
            BigDecimal bookPrice = (copy.getBook() != null && copy.getBook().getPrice() != null)
                    ? copy.getBook().getPrice() : BigDecimal.valueOf(100000);
            BigDecimal multiplier = settingService.getLostFeeMultiplier();
            BigDecimal processingFee = settingService.getLostProcessingFee();
            BigDecimal lostFine = bookPrice.multiply(multiplier).add(processingFee);

            Fine fine = dataManager.create(Fine.class);
            fine.setMember(member);
            fine.setLoanItem(loanItem);
            fine.setFineType(FineType.LOST.getId());
            fine.setAmount(lostFine);
            fine.setPaidAmount(BigDecimal.ZERO);
            fine.setStatus(FineStatus.UNPAID.getId());
            fine.setDescription(String.format("Bồi thường mất sách '%s' (Mã vạch %s)",
                    result.getBookTitle(), copy.getBarcode()));
            fine.setCreatedBy(cashier);
            fine.setCreatedAt(OffsetDateTime.now());

            toSave.add(fine);
            fineTotal = fineTotal.add(lostFine);
            result.setFineCreated(true);
            result.setLostFineAmount(lostFine);
        } else if (condition == ReturnCondition.DAMAGED) {
            loanItem.setStatus(LoanItemStatus.RETURNED.getId());
            copy.setStatus(BookCopyStatus.DAMAGED.getId());

            // Phạt hư hỏng: Tạm tính 50% giá bìa
            BigDecimal bookPrice = (copy.getBook() != null && copy.getBook().getPrice() != null)
                    ? copy.getBook().getPrice() : BigDecimal.valueOf(100000);
            BigDecimal damageFine = bookPrice.multiply(BigDecimal.valueOf(0.5));

            Fine fine = dataManager.create(Fine.class);
            fine.setMember(member);
            fine.setLoanItem(loanItem);
            fine.setFineType(FineType.DAMAGED.getId());
            fine.setAmount(damageFine);
            fine.setPaidAmount(BigDecimal.ZERO);
            fine.setStatus(FineStatus.UNPAID.getId());
            fine.setDescription(String.format("Bồi thường hư hỏng sách '%s' (Mã vạch %s)",
                    result.getBookTitle(), copy.getBarcode()));
            fine.setCreatedBy(cashier);
            fine.setCreatedAt(OffsetDateTime.now());

            toSave.add(fine);
            fineTotal = fineTotal.add(damageFine);
            result.setFineCreated(true);
            result.setDamageFineAmount(damageFine);
        } else {
            loanItem.setStatus(LoanItemStatus.RETURNED.getId());
            Reservation nextRes = reservationService.assignReturnedCopy(copy);
            if (nextRes != null && nextRes.getMember() != null) {
                result.setReservedFor(nextRes.getMember().getFullName());
            }
        }

        // Kiểm tra phạt quá hạn (BR08)
        LocalDate today = LocalDate.now();
        if (today.isAfter(loanItem.getDueDate())) {
            long days = ChronoUnit.DAYS.between(loanItem.getDueDate(), today);
            result.setDaysOverdue(days);

            int graceDays = settingService.getFineGraceDays();
            long chargeableDays = Math.max(0, days - graceDays);

            if (chargeableDays > 0) {
                MemberType mType = member != null ? member.getMemberType() : null;
                BigDecimal finePerDay = (mType != null && mType.getFinePerDay() != null)
                        ? mType.getFinePerDay() : BigDecimal.valueOf(5000);

                BigDecimal overdueFine = finePerDay.multiply(BigDecimal.valueOf(chargeableDays));

                // Trần phạt quá hạn tối đa (% giá bìa) theo cấu hình FINE_CAP_RATIO
                BigDecimal bookPrice = (copy.getBook() != null && copy.getBook().getPrice() != null)
                        ? copy.getBook().getPrice() : null;
                if (bookPrice != null) {
                    BigDecimal capRatio = settingService.getFineCapRatio();
                    BigDecimal maxCap = bookPrice.multiply(capRatio)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    if (overdueFine.compareTo(maxCap) > 0) {
                        overdueFine = maxCap;
                    }
                }

                Fine overdueFineEntity = dataManager.create(Fine.class);
                overdueFineEntity.setMember(member);
                overdueFineEntity.setLoanItem(loanItem);
                overdueFineEntity.setFineType(FineType.OVERDUE.getId());
                overdueFineEntity.setDaysOverdue((int) days);
                overdueFineEntity.setAmount(overdueFine);
                overdueFineEntity.setPaidAmount(BigDecimal.ZERO);
                overdueFineEntity.setStatus(FineStatus.UNPAID.getId());
                overdueFineEntity.setDescription(String.format("Phạt quá hạn %d ngày cuốn '%s' (Số ngày tính phạt: %d)",
                        days, result.getBookTitle(), chargeableDays));
                overdueFineEntity.setCreatedBy(cashier);
                overdueFineEntity.setCreatedAt(OffsetDateTime.now());

                toSave.add(overdueFineEntity);
                fineTotal = fineTotal.add(overdueFine);
                result.setFineCreated(true);
                result.setOverdueFineAmount(overdueFine);
            }
        }

        result.setTotalFineAmount(fineTotal);
        toSave.add(loanItem);
        toSave.add(copy);

        dataManager.save(toSave.toArray());

        log.info("Trả sách thành công: Mã vạch {}, Độc giả {}, Phạt phát sinh: {}",
                barcode, result.getMemberCode(), fineTotal);
        return result;
    }

    /**
     * L06: Gia hạn mượn sách tại quầy
     */
    @Transactional
    public Renewal renewLoanItem(Long loanItemId, User renewedBy, RenewalChannel channel, String note) {
        if (loanItemId == null) {
            throw new IllegalArgumentException("Vui lòng chọn cuốn sách cần gia hạn!");
        }

        LoanItem loanItem = dataManager.load(LoanItem.class)
                .id(loanItemId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE)
                                .add("member", memberPlan -> memberPlan.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE)))
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE)))
                .one();

        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(loanItem.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gia hạn cuốn sách đang mượn!");
        }

        // BR07: Không gia hạn khi đã quá hạn
        LocalDate today = LocalDate.now();
        if (today.isAfter(loanItem.getDueDate())) {
            throw new IllegalStateException(String.format(
                    "Sách đã quá hạn từ ngày %s (%d ngày trễ). Không được phép gia hạn!",
                    loanItem.getDueDate(), ChronoUnit.DAYS.between(loanItem.getDueDate(), today)));
        }

        Member member = loanItem.getLoan().getMember();
        MemberType type = member != null ? member.getMemberType() : null;

        // BR07: Kiểm tra số lần gia hạn tối đa
        int maxRenewals = (type != null && type.getMaxRenewals() != null) ? type.getMaxRenewals() : 1;
        int currentRenewCount = loanItem.getRenewCount() != null ? loanItem.getRenewCount() : 0;
        if (currentRenewCount >= maxRenewals) {
            throw new IllegalStateException(String.format(
                    "Cuốn sách này đã gia hạn %d/%d lần. Đã đạt giới hạn tối đa cho loại độc giả này!",
                    currentRenewCount, maxRenewals));
        }

        // BR07: Kiểm tra nợ phạt theo ngưỡng cấu hình MAX_UNPAID_FINE_THRESHOLD
        if (member != null) {
            BigDecimal unpaid = calculateUnpaidFines(member.getId());
            BigDecimal maxFineThreshold = settingService.getMaxUnpaidFineThreshold();
            if (unpaid.compareTo(maxFineThreshold) > 0) {
                throw new IllegalStateException(String.format(
                        "Độc giả đang có tiền phạt chưa thanh toán (%,.0f VNĐ) vượt quá ngưỡng cho phép (%,.0f VNĐ). Cần nộp phạt trước khi gia hạn!",
                        unpaid, maxFineThreshold));
            }
        }

        // BR07: Không gia hạn nếu có độc giả khác đang đặt trước đầu sách
        if (loanItem.getBookCopy() != null && loanItem.getBookCopy().getBook() != null) {
            if (reservationService.hasPendingReservations(loanItem.getBookCopy().getBook())) {
                throw new IllegalStateException(
                        "Không thể gia hạn! Đầu sách này đang có độc giả khác đặt trước trong hàng đợi.");
            }
        }

        // Tính ngày hạn mới = hạn cũ + renewDays
        int renewDays = (type != null && type.getRenewDays() != null) ? type.getRenewDays() : 7;
        LocalDate oldDueDate = loanItem.getDueDate();
        LocalDate newDueDate = oldDueDate.plusDays(renewDays);

        User actor = renewedBy != null ? renewedBy : (User) currentAuthentication.getUser();

        Renewal renewal = dataManager.create(Renewal.class);
        renewal.setLoanItem(loanItem);
        renewal.setOldDueDate(oldDueDate);
        renewal.setNewDueDate(newDueDate);
        renewal.setRenewedBy(actor);
        renewal.setChannel(channel != null ? channel.getId() : RenewalChannel.COUNTER.getId());
        renewal.setRenewedAt(OffsetDateTime.now());
        renewal.setNote(note);

        loanItem.setDueDate(newDueDate);
        loanItem.setRenewCount(currentRenewCount + 1);

        dataManager.save(renewal, loanItem);
        log.info("Gia hạn thành công sách mã vạch {} đến ngày {}", loanItem.getBookCopy().getBarcode(), newDueDate);
        return renewal;
    }

    public static class ReturnResult {
        private String barcode;
        private String bookTitle;
        private String memberCode;
        private String memberName;
        private ReturnCondition condition;
        private long daysOverdue = 0;
        private boolean fineCreated = false;
        private BigDecimal overdueFineAmount = BigDecimal.ZERO;
        private BigDecimal lostFineAmount = BigDecimal.ZERO;
        private BigDecimal damageFineAmount = BigDecimal.ZERO;
        private BigDecimal totalFineAmount = BigDecimal.ZERO;
        private String reservedFor;

        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }

        public String getBookTitle() { return bookTitle; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

        public String getMemberCode() { return memberCode; }
        public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public ReturnCondition getCondition() { return condition; }
        public void setCondition(ReturnCondition condition) { this.condition = condition; }

        public long getDaysOverdue() { return daysOverdue; }
        public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }

        public boolean isFineCreated() { return fineCreated; }
        public void setFineCreated(boolean fineCreated) { this.fineCreated = fineCreated; }

        public BigDecimal getOverdueFineAmount() { return overdueFineAmount; }
        public void setOverdueFineAmount(BigDecimal overdueFineAmount) { this.overdueFineAmount = overdueFineAmount; }

        public BigDecimal getLostFineAmount() { return lostFineAmount; }
        public void setLostFineAmount(BigDecimal lostFineAmount) { this.lostFineAmount = lostFineAmount; }

        public BigDecimal getDamageFineAmount() { return damageFineAmount; }
        public void setDamageFineAmount(BigDecimal damageFineAmount) { this.damageFineAmount = damageFineAmount; }

        public BigDecimal getTotalFineAmount() { return totalFineAmount; }
        public void setTotalFineAmount(BigDecimal totalFineAmount) { this.totalFineAmount = totalFineAmount; }

        public String getReservedFor() { return reservedFor; }
        public void setReservedFor(String reservedFor) { this.reservedFor = reservedFor; }
    }
}
