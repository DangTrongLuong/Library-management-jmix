package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReaderLoanService {

    private final DataManager dataManager;
    private final CurrentAuthentication currentAuthentication;
    private final LoanService loanService;
    private final ReservationService reservationService;
    private final LibrarySettingService settingService;

    public ReaderLoanService(DataManager dataManager,
                             CurrentAuthentication currentAuthentication,
                             LoanService loanService,
                             ReservationService reservationService,
                             LibrarySettingService settingService) {
        this.dataManager = dataManager;
        this.currentAuthentication = currentAuthentication;
        this.loanService = loanService;
        this.reservationService = reservationService;
        this.settingService = settingService;
    }

    public record CurrentLoanItemDto(
            LoanItem loanItem,
            Book book,
            BookCopy bookCopy,
            LocalDate borrowDate,
            LocalDate dueDate,
            int renewCount,
            int maxRenewals,
            boolean isOverdue,
            long daysOverdue,
            long daysRemaining,
            boolean canRenew,
            String cantRenewReason
    ) {}

    public record LoanSummaryDto(
            int currentLoansCount,
            int maxBorrowLimit,
            int dueSoonCount,
            int overdueCount,
            BigDecimal unpaidFines,
            String memberCode,
            String fullName,
            String memberTypeName,
            LocalDate cardExpiryDate
    ) {}

    public record ReaderReservationDto(
            Reservation reservation,
            int queuePosition
    ) {}

    public record ReservationSummaryDto(
            int pendingCount,
            int readyCount,
            int historyCount,
            int maxReservations
    ) {}

    public record ReaderFineSummaryDto(
            BigDecimal totalOutstanding,
            BigDecimal totalAmount,
            BigDecimal totalPaid,
            BigDecimal totalWaived,
            int unpaidCount,
            int totalFinesCount
    ) {}

    /**
     * Lấy độc giả tương ứng với người dùng đang đăng nhập
     */
    public Optional<Member> getCurrentMember() {
        if (currentAuthentication == null || !(currentAuthentication.getUser() instanceof User u)) {
            return Optional.empty();
        }
        return dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user = :u")
                .parameter("u", u)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE))
                .optional();
    }

    /**
     * Thống kê tổng quan thẻ và tình trạng mượn của độc giả
     */
    public LoanSummaryDto getLoanSummary(Member member) {
        if (member == null) {
            return new LoanSummaryDto(0, 0, 0, 0, BigDecimal.ZERO, "-", "-", "-", null);
        }
        MemberType type = member.getMemberType();
        int maxBorrow = (type != null && type.getMaxLoanBooks() != null) ? type.getMaxLoanBooks() : 5;

        List<LoanItem> activeItems = getActiveLoanItems(member.getId());
        int currentCount = activeItems.size();

        LocalDate today = LocalDate.now();
        int dueSoon = 0;
        int overdue = 0;

        for (LoanItem item : activeItems) {
            if (item.getDueDate() != null) {
                if (today.isAfter(item.getDueDate())) {
                    overdue++;
                } else {
                    long days = ChronoUnit.DAYS.between(today, item.getDueDate());
                    int reminderDays = settingService != null ? settingService.getDueReminderDaysBefore() : 2;
                    if (days <= reminderDays) {
                        dueSoon++;
                    }
                }
            }
        }

        BigDecimal unpaidFines = loanService.calculateUnpaidFines(member.getId());

        return new LoanSummaryDto(
                currentCount,
                maxBorrow,
                dueSoon,
                overdue,
                unpaidFines,
                member.getMemberCode(),
                member.getFullName(),
                type != null ? type.getName() : "Mặc định",
                member.getCardExpiryDate()
        );
    }

    /**
     * Lấy danh sách các cuốn sách đang mượn kèm tình trạng gia hạn chi tiết
     */
    public List<CurrentLoanItemDto> getCurrentLoans(Long memberId) {
        if (memberId == null) return Collections.emptyList();

        List<LoanItem> items = getActiveLoanItems(memberId);
        List<CurrentLoanItemDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        BigDecimal unpaid = loanService.calculateUnpaidFines(memberId);
        BigDecimal maxFineThreshold = settingService != null ? settingService.getMaxUnpaidFineThreshold() : BigDecimal.valueOf(50000);
        boolean hasUnpaidFines = unpaid.compareTo(maxFineThreshold) > 0;

        for (LoanItem item : items) {
            BookCopy copy = item.getBookCopy();
            Book book = copy != null ? copy.getBook() : null;
            Member member = item.getLoan() != null ? item.getLoan().getMember() : null;
            MemberType type = member != null ? member.getMemberType() : null;

            int maxRenewals = (type != null && type.getMaxRenewals() != null) ? type.getMaxRenewals() : 1;
            int renewCount = item.getRenewCount() != null ? item.getRenewCount() : 0;

            LocalDate due = item.getDueDate();
            boolean isOverdue = due != null && today.isAfter(due);
            long daysOverdue = isOverdue ? ChronoUnit.DAYS.between(due, today) : 0;
            long daysRemaining = (due != null && !isOverdue) ? ChronoUnit.DAYS.between(today, due) : 0;

            boolean canRenew = true;
            String reason = null;

            if (isOverdue) {
                canRenew = false;
                reason = "Sách đã quá hạn (" + daysOverdue + " ngày trễ). Cần trả tại quầy!";
            } else if (renewCount >= maxRenewals) {
                canRenew = false;
                reason = String.format("Đã đạt số lần gia hạn tối đa (%d/%d lần)", renewCount, maxRenewals);
            } else if (hasUnpaidFines) {
                canRenew = false;
                reason = String.format("Đang có nợ phạt (%,.0f VNĐ) vượt ngưỡng cho phép (%,.0f VNĐ)", unpaid, maxFineThreshold);
            } else if (book != null && reservationService.hasPendingReservations(book)) {
                canRenew = false;
                reason = "Có độc giả khác đang đặt trước đầu sách này";
            }

            LocalDate borrowDate = (item.getLoan() != null && item.getLoan().getLoanDate() != null)
                    ? item.getLoan().getLoanDate().toLocalDate() : null;

            result.add(new CurrentLoanItemDto(
                    item, book, copy, borrowDate, due, renewCount, maxRenewals,
                    isOverdue, daysOverdue, daysRemaining, canRenew, reason
            ));
        }

        return result;
    }

    public List<LoanItem> getActiveLoanItems(Long memberId) {
        return dataManager.load(LoanItem.class)
                .query("select li from LoanItem li where li.loan.member.id = :mId and li.status = 'BORROWED' order by li.dueDate asc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE)
                                .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE)
                                        .add("authors", FetchPlan.BASE)
                                        .add("categories", FetchPlan.BASE))
                                .add("location", FetchPlan.BASE))
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE)
                                .add("member", memberPlan -> memberPlan.addFetchPlan(FetchPlan.BASE).add("memberType", FetchPlan.BASE))))
                .list();
    }

    public List<LoanItem> getLoanHistory(Long memberId) {
        if (memberId == null) return Collections.emptyList();
        return dataManager.load(LoanItem.class)
                .query("select li from LoanItem li where li.loan.member.id = :mId and li.status in ('RETURNED', 'LOST') order by li.returnedAt desc, li.dueDate desc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE)
                                .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE)
                                        .add("authors", FetchPlan.BASE)
                                        .add("categories", FetchPlan.BASE)))
                        .add("loan", loanPlan -> loanPlan.addFetchPlan(FetchPlan.BASE))
                        .add("receivedBy", FetchPlan.BASE))
                .list();
    }

    public Renewal renewOnline(Long loanItemId) {
        User u = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User user) {
            u = user;
        }
        return loanService.renewLoanItem(loanItemId, u, RenewalChannel.ONLINE, "Độc giả tự gia hạn trực tuyến");
    }

    /**
     * Lấy các lượt đặt trước đang hiệu lực (PENDING + READY) kèm thứ tự trong hàng chờ
     */
    public List<ReaderReservationDto> getActiveReservations(Long memberId) {
        if (memberId == null) return Collections.emptyList();

        List<Reservation> list = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.member.id = :mId and r.status in ('PENDING', 'READY') order by r.reservedAt desc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE)
                                .add("authors", FetchPlan.BASE)
                                .add("categories", FetchPlan.BASE))
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE)
                                .add("location", FetchPlan.BASE)))
                .list();

        List<ReaderReservationDto> result = new ArrayList<>();
        for (Reservation r : list) {
            int queuePos = 0;
            if (ReservationStatus.PENDING.getId().equalsIgnoreCase(r.getStatus()) && r.getBook() != null) {
                Long pos = dataManager.loadValue(
                        "select count(other) from Reservation other where other.book.id = :bookId and other.status = 'PENDING' and other.reservedAt <= :resAt",
                        Long.class)
                        .parameter("bookId", r.getBook().getId())
                        .parameter("resAt", r.getReservedAt())
                        .one();
                queuePos = pos != null ? pos.intValue() : 1;
            }
            result.add(new ReaderReservationDto(r, queuePos));
        }
        return result;
    }

    /**
     * Lấy lịch sử đặt trước (FULFILLED, CANCELLED, EXPIRED)
     */
    public List<Reservation> getReservationHistory(Long memberId) {
        if (memberId == null) return Collections.emptyList();

        return dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.member.id = :mId and r.status in ('FULFILLED', 'CANCELLED', 'EXPIRED') order by r.updatedAt desc, r.reservedAt desc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", bookPlan -> bookPlan.addFetchPlan(FetchPlan.BASE)
                                .add("authors", FetchPlan.BASE)
                                .add("categories", FetchPlan.BASE))
                        .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE)
                                .add("location", FetchPlan.BASE))
                        .add("cancelledBy", FetchPlan.BASE))
                .list();
    }

    /**
     * Thống kê tóm tắt đặt trước cho độc giả
     */
    public ReservationSummaryDto getReservationSummary(Member member) {
        if (member == null) {
            return new ReservationSummaryDto(0, 0, 0, 3);
        }
        int maxReservations = 3;
        if (member.getMemberType() != null && member.getMemberType().getMaxReservations() != null) {
            maxReservations = member.getMemberType().getMaxReservations();
        }

        Long pending = dataManager.loadValue(
                "select count(r) from Reservation r where r.member.id = :mId and r.status = 'PENDING'", Long.class)
                .parameter("mId", member.getId())
                .one();

        Long ready = dataManager.loadValue(
                "select count(r) from Reservation r where r.member.id = :mId and r.status = 'READY'", Long.class)
                .parameter("mId", member.getId())
                .one();

        Long history = dataManager.loadValue(
                "select count(r) from Reservation r where r.member.id = :mId and r.status in ('FULFILLED', 'CANCELLED', 'EXPIRED')", Long.class)
                .parameter("mId", member.getId())
                .one();

        return new ReservationSummaryDto(
                pending != null ? pending.intValue() : 0,
                ready != null ? ready.intValue() : 0,
                history != null ? history.intValue() : 0,
                maxReservations
        );
    }

    /**
     * Hủy yêu cầu đặt trước
     */
    public void cancelReservation(Long reservationId) {
        reservationService.cancelReservation(reservationId, "Độc giả tự hủy trên cổng bạn đọc");
    }

    /**
     * Lấy danh sách các khoản phạt của độc giả
     */
    public List<Fine> getReaderFines(Long memberId) {
        if (memberId == null) return Collections.emptyList();

        return dataManager.load(Fine.class)
                .query("select f from Fine f where f.member.id = :mId order by f.createdAt desc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("loanItem", itemPlan -> itemPlan.addFetchPlan(FetchPlan.BASE)
                                .add("bookCopy", copyPlan -> copyPlan.addFetchPlan(FetchPlan.BASE)
                                        .add("book", FetchPlan.BASE)))
                        .add("createdBy", FetchPlan.BASE)
                        .add("waivedBy", FetchPlan.BASE))
                .list();
    }

    /**
     * Lấy lịch sử biên lai thanh toán của độc giả
     */
    public List<Payment> getReaderPayments(Long memberId) {
        if (memberId == null) return Collections.emptyList();

        return dataManager.load(Payment.class)
                .query("select p from Payment p where p.member.id = :mId order by p.paidAt desc")
                .parameter("mId", memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("fine", FetchPlan.BASE)
                        .add("receivedBy", FetchPlan.BASE))
                .list();
    }

    /**
     * Thống kê tiền phạt của độc giả
     */
    public ReaderFineSummaryDto getFineSummary(Long memberId) {
        if (memberId == null) {
            return new ReaderFineSummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }
        List<Fine> fines = getReaderFines(memberId);
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalWaived = BigDecimal.ZERO;
        int unpaidCount = 0;

        for (Fine f : fines) {
            BigDecimal amt = f.getAmount() != null ? f.getAmount() : BigDecimal.ZERO;
            BigDecimal paid = f.getPaidAmount() != null ? f.getPaidAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(amt);
            totalPaid = totalPaid.add(paid);

            if (FineStatus.UNPAID.getId().equalsIgnoreCase(f.getStatus()) || FineStatus.PARTIAL.getId().equalsIgnoreCase(f.getStatus())) {
                totalOutstanding = totalOutstanding.add(amt.subtract(paid));
                unpaidCount++;
            } else if (FineStatus.WAIVED.getId().equalsIgnoreCase(f.getStatus())) {
                totalWaived = totalWaived.add(amt);
            }
        }

        return new ReaderFineSummaryDto(totalOutstanding, totalAmount, totalPaid, totalWaived, unpaidCount, fines.size());
    }
}

