package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LibrarianShiftService {

    private final DataManager dataManager;
    private final ReservationService reservationService;

    public LibrarianShiftService(DataManager dataManager, ReservationService reservationService) {
        this.dataManager = dataManager;
        this.reservationService = reservationService;
    }

    public record ShiftStats(
            LocalDate date,
            long loansCount,
            long loanCopiesCount,
            long returnsCount,
            long returnsGoodCount,
            long returnsDamagedOrLostCount,
            long readyReservationsCount,
            long readyExpiringTodayCount,
            BigDecimal finesCollected,
            long paymentsCount,
            long renewalsCount,
            long activePatronsVisited
    ) {}

    public record ReadyReservationItem(
            Long reservationId,
            String bookTitle,
            String bookIsbn,
            String copyBarcode,
            Long memberId,
            String memberName,
            String memberCode,
            String memberPhone,
            OffsetDateTime reservedAt,
            OffsetDateTime readyAt,
            OffsetDateTime holdExpiresAt,
            long remainingHours,
            String statusBadge,
            String badgeTheme
    ) {}

    public record ShiftActivityItem(
            OffsetDateTime time,
            String type,
            String typeLabel,
            String title,
            String description,
            String memberName,
            String staffName,
            String badgeTheme,
            String badgeColor
    ) {}

    public record UrgentOverdueItem(
            String bookTitle,
            String copyBarcode,
            String memberName,
            String memberPhone,
            LocalDate dueDate,
            long daysOverdue,
            BigDecimal estimatedFine
    ) {}

    /**
     * Lấy thống kê tổng hợp ca trực theo ngày
     */
    public ShiftStats getShiftStats(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        ZoneOffset currentOffset = OffsetDateTime.now().getOffset();
        OffsetDateTime start = targetDate.atStartOfDay().atOffset(currentOffset);
        OffsetDateTime nextDayStart = targetDate.plusDays(1).atStartOfDay().atOffset(currentOffset);

        // 1. Số lượt mượn (số phiếu) và số cuốn sách mượn trong ngày
        long loansCount = dataManager.loadValue(
                "select count(l) from Loan l where l.loanDate >= :start and l.loanDate < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        long loanCopiesCount = dataManager.loadValue(
                "select count(li) from LoanItem li where li.loan.loanDate >= :start and li.loan.loanDate < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        // 2. Số cuốn sách độc giả trả tại quầy trong ngày
        long returnsCount = dataManager.loadValue(
                "select count(li) from LoanItem li where li.returnedAt >= :start and li.returnedAt < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        long returnsGoodCount = dataManager.loadValue(
                "select count(li) from LoanItem li where li.returnedAt >= :start and li.returnedAt < :nextDayStart " +
                "and (li.returnCondition is null or li.returnCondition = 'GOOD')", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        long returnsDamagedOrLostCount = dataManager.loadValue(
                "select count(li) from LoanItem li where li.returnedAt >= :start and li.returnedAt < :nextDayStart " +
                "and li.returnCondition in ('DAMAGED', 'LOST')", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        // 3. Sách đặt trước đang chờ độc giả ghé lấy tại quầy (READY)
        long readyReservationsCount = dataManager.loadValue(
                "select count(r) from Reservation r where r.status = 'READY'", Long.class)
                .one();

        long readyExpiringTodayCount = dataManager.loadValue(
                "select count(r) from Reservation r where r.status = 'READY' and r.holdExpiresAt < :nextDayStart", Long.class)
                .parameter("nextDayStart", nextDayStart)
                .one();

        // 4. Tổng tiền phạt thu được trong ca trực / ngày
        BigDecimal finesCollected = dataManager.loadValue(
                "select coalesce(sum(p.amount), 0) from Payment p where p.paidAt >= :start and p.paidAt < :nextDayStart", BigDecimal.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        long paymentsCount = dataManager.loadValue(
                "select count(p) from Payment p where p.paidAt >= :start and p.paidAt < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        // 5. Gia hạn sách trong ngày
        long renewalsCount = dataManager.loadValue(
                "select count(rn) from Renewal rn where rn.renewedAt >= :start and rn.renewedAt < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .one();

        // 6. Số độc giả độc nhất ghé quầy giao dịch trong ngày
        Set<Long> uniqueMemberIds = new HashSet<>();
        List<Long> loanMemberIds = dataManager.loadValue(
                "select distinct l.member.id from Loan l where l.loanDate >= :start and l.loanDate < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .list();
        uniqueMemberIds.addAll(loanMemberIds);

        List<Long> returnMemberIds = dataManager.loadValue(
                "select distinct li.loan.member.id from LoanItem li where li.returnedAt >= :start and li.returnedAt < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .list();
        uniqueMemberIds.addAll(returnMemberIds);

        List<Long> paymentMemberIds = dataManager.loadValue(
                "select distinct p.member.id from Payment p where p.paidAt >= :start and p.paidAt < :nextDayStart", Long.class)
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .list();
        uniqueMemberIds.addAll(paymentMemberIds);

        long activePatronsVisited = uniqueMemberIds.size();

        return new ShiftStats(
                targetDate,
                loansCount,
                loanCopiesCount,
                returnsCount,
                returnsGoodCount,
                returnsDamagedOrLostCount,
                readyReservationsCount,
                readyExpiringTodayCount,
                finesCollected != null ? finesCollected : BigDecimal.ZERO,
                paymentsCount,
                renewalsCount,
                activePatronsVisited
        );
    }

    /**
     * Lấy danh sách các cuốn sách đặt trước đang chờ độc giả ghé lấy tại quầy (READY)
     */
    public List<ReadyReservationItem> getReadyReservations() {
        List<Reservation> reservations = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.status = 'READY' order by r.holdExpiresAt asc")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", FetchPlan.BASE)
                        .add("bookCopy", FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("member.user", FetchPlan.BASE))
                .list();

        List<ReadyReservationItem> items = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (Reservation r : reservations) {
            String title = r.getBook() != null ? r.getBook().getTitle() : "Chưa xác định";
            String isbn = r.getBook() != null ? r.getBook().getIsbn() : "-";
            String barcode = r.getBookCopy() != null ? r.getBookCopy().getBarcode() : "-";
            Long memberId = r.getMember() != null ? r.getMember().getId() : null;
            String memberName = r.getMember() != null ? r.getMember().getFullName() : "Độc giả";
            String memberCode = r.getMember() != null ? r.getMember().getMemberCode() : "-";
            String phone = (r.getMember() != null && r.getMember().getUser() != null)
                    ? r.getMember().getUser().getPhone() : "-";

            OffsetDateTime holdExpires = r.getHoldExpiresAt();
            long remainingHours = 0;
            String statusBadge;
            String badgeTheme;

            if (holdExpires != null) {
                if (holdExpires.isBefore(now)) {
                    remainingHours = 0;
                    statusBadge = "ĐÃ QUÁ HẠN GIỮ";
                    badgeTheme = "error";
                } else {
                    remainingHours = Duration.between(now, holdExpires).toHours();
                    if (remainingHours <= 24) {
                        statusBadge = "SẮP HẾT HẠN (" + remainingHours + "h)";
                        badgeTheme = "warning";
                    } else {
                        long days = remainingHours / 24;
                        statusBadge = "CÒN " + days + " NGÀY (" + remainingHours + "h)";
                        badgeTheme = "success";
                    }
                }
            } else {
                statusBadge = "CHỜ NHẬN";
                badgeTheme = "primary";
            }

            items.add(new ReadyReservationItem(
                    r.getId(),
                    title,
                    isbn,
                    barcode,
                    memberId,
                    memberName,
                    memberCode,
                    phone != null ? phone : "-",
                    r.getReservedAt(),
                    r.getNotifiedAt(),
                    holdExpires,
                    remainingHours,
                    statusBadge,
                    badgeTheme
            ));
        }

        return items;
    }

    /**
     * Lấy dòng thời gian các hoạt động giao dịch diễn ra trong ca trực / ngày
     */
    public List<ShiftActivityItem> getRecentShiftActivities(LocalDate date, int limit) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        ZoneOffset currentOffset = OffsetDateTime.now().getOffset();
        OffsetDateTime start = targetDate.atStartOfDay().atOffset(currentOffset);
        OffsetDateTime nextDayStart = targetDate.plusDays(1).atStartOfDay().atOffset(currentOffset);

        List<ShiftActivityItem> activities = new ArrayList<>();

        // 1. Các lượt mượn sách
        List<Loan> loans = dataManager.load(Loan.class)
                .query("select l from Loan l where l.loanDate >= :start and l.loanDate < :nextDayStart order by l.loanDate desc")
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .maxResults(limit)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("issuedBy", FetchPlan.BASE)
                        .add("loanItems", builderItems -> builderItems.addFetchPlan(FetchPlan.BASE)
                                .add("bookCopy", builderCopy -> builderCopy.addFetchPlan(FetchPlan.BASE)
                                        .add("book", FetchPlan.BASE))))
                .list();

        for (Loan l : loans) {
            int count = l.getLoanItems() != null ? l.getLoanItems().size() : 0;
            String memberName = l.getMember() != null ? l.getMember().getFullName() : "Độc giả";
            String staff = l.getIssuedBy() != null ? (l.getIssuedBy().getFullName() != null ? l.getIssuedBy().getFullName() : l.getIssuedBy().getUsername()) : "Thủ thư";

            activities.add(new ShiftActivityItem(
                    l.getLoanDate(),
                    "LOAN",
                    "Mượn sách",
                    "Lập phiếu mượn " + l.getLoanCode(),
                    String.format("Độc giả %s mượn %d cuốn sách", memberName, count),
                    memberName,
                    staff,
                    "primary",
                    "#0284c7"
            ));
        }

        // 2. Các lượt trả sách
        List<LoanItem> returnedItems = dataManager.load(LoanItem.class)
                .query("select li from LoanItem li where li.returnedAt >= :start and li.returnedAt < :nextDayStart order by li.returnedAt desc")
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .maxResults(limit)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("loan", builderLoan -> builderLoan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE))
                        .add("receivedBy", FetchPlan.BASE)
                        .add("bookCopy", builderCopy -> builderCopy.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE)))
                .list();

        for (LoanItem li : returnedItems) {
            String title = (li.getBookCopy() != null && li.getBookCopy().getBook() != null)
                    ? li.getBookCopy().getBook().getTitle() : "Sách thư viện";
            String barcode = li.getBookCopy() != null ? li.getBookCopy().getBarcode() : "";
            String memberName = (li.getLoan() != null && li.getLoan().getMember() != null)
                    ? li.getLoan().getMember().getFullName() : "Độc giả";
            String staff = li.getReceivedBy() != null ? (li.getReceivedBy().getFullName() != null ? li.getReceivedBy().getFullName() : li.getReceivedBy().getUsername()) : "Thủ thư";
            String condition = "GOOD".equalsIgnoreCase(li.getReturnCondition()) ? "Nguyên vẹn" : "Có phát sinh hỏng/mất";

            activities.add(new ShiftActivityItem(
                    li.getReturnedAt(),
                    "RETURN",
                    "Trả sách",
                    "Nhận trả sách [" + barcode + "]",
                    String.format("'%s' - Độc giả: %s (%s)", title, memberName, condition),
                    memberName,
                    staff,
                    "success",
                    "#16a34a"
            ));
        }

        // 3. Các lượt thu tiền phạt
        List<Payment> payments = dataManager.load(Payment.class)
                .query("select p from Payment p where p.paidAt >= :start and p.paidAt < :nextDayStart order by p.paidAt desc")
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .maxResults(limit)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("receivedBy", FetchPlan.BASE))
                .list();

        for (Payment p : payments) {
            String memberName = p.getMember() != null ? p.getMember().getFullName() : "Độc giả";
            String staff = p.getReceivedBy() != null ? (p.getReceivedBy().getFullName() != null ? p.getReceivedBy().getFullName() : p.getReceivedBy().getUsername()) : "Thủ thư";

            activities.add(new ShiftActivityItem(
                    p.getPaidAt(),
                    "PAYMENT",
                    "Thu phạt",
                    "Biên lai " + p.getReceiptNo(),
                    String.format("Thu %,.0f VNĐ từ %s (%s)", p.getAmount(), memberName, p.getMethod()),
                    memberName,
                    staff,
                    "warning",
                    "#d97706"
            ));
        }

        // 4. Các lượt gia hạn sách
        List<Renewal> renewals = dataManager.load(Renewal.class)
                .query("select rn from Renewal rn where rn.renewedAt >= :start and rn.renewedAt < :nextDayStart order by rn.renewedAt desc")
                .parameter("start", start)
                .parameter("nextDayStart", nextDayStart)
                .maxResults(limit)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("renewedBy", FetchPlan.BASE)
                        .add("loanItem", builderItem -> builderItem.addFetchPlan(FetchPlan.BASE)
                                .add("loan", builderLoan -> builderLoan.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE))
                                .add("bookCopy", builderCopy -> builderCopy.addFetchPlan(FetchPlan.BASE).add("book", FetchPlan.BASE))))
                .list();

        for (Renewal rn : renewals) {
            String title = (rn.getLoanItem() != null && rn.getLoanItem().getBookCopy() != null && rn.getLoanItem().getBookCopy().getBook() != null)
                    ? rn.getLoanItem().getBookCopy().getBook().getTitle() : "Sách";
            String memberName = (rn.getLoanItem() != null && rn.getLoanItem().getLoan() != null && rn.getLoanItem().getLoan().getMember() != null)
                    ? rn.getLoanItem().getLoan().getMember().getFullName() : "Độc giả";
            String staff = rn.getRenewedBy() != null ? (rn.getRenewedBy().getFullName() != null ? rn.getRenewedBy().getFullName() : rn.getRenewedBy().getUsername()) : "Thủ thư";

            activities.add(new ShiftActivityItem(
                    rn.getRenewedAt(),
                    "RENEWAL",
                    "Gia hạn",
                    "Gia hạn đến " + rn.getNewDueDate(),
                    String.format("'%s' cho %s (%s)", title, memberName, rn.getChannel()),
                    memberName,
                    staff,
                    "contrast",
                    "#9333ea"
            ));
        }

        // Sắp xếp giảm dần theo thời gian
        activities.sort((a, b) -> b.time().compareTo(a.time()));

        if (activities.size() > limit) {
            return activities.subList(0, limit);
        }
        return activities;
    }

    /**
     * Lấy danh sách các sách quá hạn nghiêm trọng cần lưu ý khi độc giả đến quầy
     */
    public List<UrgentOverdueItem> getUrgentOverdueList(int limit) {
        LocalDate today = LocalDate.now();
        List<KeyValueEntity> rows = dataManager.loadValues(
                "select li.bookCopy.book.title, li.bookCopy.barcode, li.loan.member.fullName, " +
                "li.loan.member.user.phone, li.dueDate " +
                "from LoanItem li " +
                "where li.status = 'BORROWED' and li.dueDate < :today " +
                "order by li.dueDate asc")
                .parameter("today", today)
                .maxResults(limit)
                .properties("title", "barcode", "memberName", "phone", "dueDate")
                .list();

        List<UrgentOverdueItem> items = new ArrayList<>();
        for (KeyValueEntity r : rows) {
            String title = r.getValue("title");
            String barcode = r.getValue("barcode");
            String member = r.getValue("memberName");
            String phone = r.getValue("phone");
            LocalDate due = r.getValue("dueDate");
            long days = due != null ? ChronoUnit.DAYS.between(due, today) : 0;
            BigDecimal fine = BigDecimal.valueOf(days * 5000L);

            items.add(new UrgentOverdueItem(
                    title != null ? title : "Chưa xác định",
                    barcode != null ? barcode : "-",
                    member != null ? member : "Độc giả",
                    phone != null ? phone : "-",
                    due,
                    days,
                    fine
            ));
        }
        return items;
    }

    /**
     * Tự động quét điều phối bản sao cho các yêu cầu đặt trước PENDING
     */
    public int autoMatchPendingReservations() {
        return reservationService.autoMatchPendingReservations();
    }

    /**
     * Quét và hủy các lượt đặt trước READY đã quá thời hạn giữ chỗ
     */
    public int processExpiredReservations() {
        return reservationService.processExpiredReservations();
    }

    /**
     * Hủy yêu cầu đặt trước tại quầy
     */
    public void cancelReservation(Long reservationId, String reason) {
        reservationService.cancelReservation(reservationId, reason);
    }
}
