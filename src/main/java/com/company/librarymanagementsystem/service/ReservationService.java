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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    public static final int DEFAULT_HOLD_DAYS = 3;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private MemberService memberService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private LibrarySettingService settingService;

    public int getHoldExpiryDays() {
        return settingService != null ? settingService.getHoldExpiryDays() : DEFAULT_HOLD_DAYS;
    }

    public record ReservationValidationResult(boolean isValid, String errorMessage) {
    }

    public record ReservationStats(long totalPending, long totalReady, long totalFulfilled, long totalExpiredOrCancelled) {
    }

    /**
     * Kiểm tra tính hợp lệ trước khi đặt trước sách (BR10)
     */
    public ReservationValidationResult validateReservation(Member member, Book book) {
        if (member == null) {
            return new ReservationValidationResult(false, "Vui lòng chọn độc giả!");
        }
        if (book == null) {
            return new ReservationValidationResult(false, "Vui lòng chọn đầu sách!");
        }

        // 1. Kiểm tra tư cách độc giả (thẻ active, không nợ phạt, không quá hạn)
        MemberService.EligibilityResult eligibility = memberService.checkEligibility(member);
        if (!eligibility.isEligible()) {
            return new ReservationValidationResult(false,
                    "Độc giả không đủ điều kiện đặt trước sách: " + String.join("; ", eligibility.getBlockingReasons()));
        }

        // 2. Kiểm tra số lượt đặt trước đang hiệu lực (PENDING + READY)
        int maxReservations = 3;
        if (member.getMemberType() != null && member.getMemberType().getMaxReservations() != null) {
            maxReservations = member.getMemberType().getMaxReservations();
        }

        Long activeCount = dataManager.loadValue(
                "select count(r) from Reservation r where r.member.id = :memberId and r.status in ('PENDING', 'READY')",
                Long.class)
                .parameter("memberId", member.getId())
                .one();

        if (activeCount >= maxReservations) {
            return new ReservationValidationResult(false, String.format(
                    "Độc giả đã đạt số lượt đặt trước tối đa (%d / %d lượt đang chờ)!", activeCount, maxReservations));
        }

        // 3. Kiểm tra không đặt trùng cùng một đầu sách
        Long duplicateCount = dataManager.loadValue(
                "select count(r) from Reservation r where r.member.id = :memberId and r.book.id = :bookId and r.status in ('PENDING', 'READY')",
                Long.class)
                .parameter("memberId", member.getId())
                .parameter("bookId", book.getId())
                .one();

        if (duplicateCount > 0) {
            return new ReservationValidationResult(false, "Độc giả đang có yêu cầu đặt trước cho đầu sách này rồi!");
        }

        // 4. Kiểm tra độc giả có đang mượn cuốn sách thuộc đầu sách này không
        Long currentlyBorrowing = dataManager.loadValue(
                "select count(li) from LoanItem li where li.loan.member.id = :memberId and li.bookCopy.book.id = :bookId and li.status = 'BORROWED'",
                Long.class)
                .parameter("memberId", member.getId())
                .parameter("bookId", book.getId())
                .one();

        if (currentlyBorrowing > 0) {
            return new ReservationValidationResult(false, "Độc giả hiện đang mượn cuốn sách thuộc đầu sách này, không được đặt trước thêm!");
        }

        // 5. Kiểm tra tình trạng bản sao trên kệ: chỉ cho đặt khi không còn bản sao AVAILABLE
        Long availableCopies = dataManager.loadValue(
                "select count(bc) from BookCopy bc where bc.book.id = :bookId and bc.status = 'AVAILABLE'",
                Long.class)
                .parameter("bookId", book.getId())
                .one();

        if (availableCopies > 0) {
            return new ReservationValidationResult(false, String.format(
                    "Đầu sách này hiện còn %d bản sao sẵn sàng trên kệ (AVAILABLE), vui lòng đến quầy mượn trực tiếp!", availableCopies));
        }

        return new ReservationValidationResult(true, null);
    }

    /**
     * Tạo yêu cầu đặt trước sách mới (BR10)
     */
    @Transactional
    public Reservation createReservation(Member member, Book book) {
        ReservationValidationResult val = validateReservation(member, book);
        if (!val.isValid()) {
            throw new IllegalStateException(val.errorMessage());
        }

        Reservation res = dataManager.create(Reservation.class);
        res.setMember(member);
        res.setBook(book);
        res.setStatus(ReservationStatus.PENDING.getId());
        res.setReservedAt(OffsetDateTime.now());

        return dataManager.save(res);
    }

    /**
     * Hủy yêu cầu đặt trước sách
     */
    @Transactional
    public void cancelReservation(Long reservationId, String reason) {
        Reservation res = dataManager.load(Reservation.class)
                .id(reservationId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", FetchPlan.BASE)
                        .add("bookCopy", FetchPlan.BASE))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đặt trước ID: " + reservationId));

        if (ReservationStatus.FULFILLED.getId().equalsIgnoreCase(res.getStatus())
                || ReservationStatus.CANCELLED.getId().equalsIgnoreCase(res.getStatus())) {
            throw new IllegalStateException("Yêu cầu này đã ở trạng thái kết thúc, không thể hủy!");
        }

        User currentUser = null;
        if (currentAuthentication != null && currentAuthentication.getUser() instanceof User u) {
            currentUser = u;
        }

        boolean wasReady = ReservationStatus.READY.getId().equalsIgnoreCase(res.getStatus());
        BookCopy copy = res.getBookCopy();

        res.setStatus(ReservationStatus.CANCELLED.getId());
        res.setCancelReason(reason != null ? reason.trim() : "Thủ thư hủy tại quầy");
        res.setCancelledAt(OffsetDateTime.now());
        res.setCancelledBy(currentUser);
        res.setBookCopy(null);
        dataManager.save(res);

        // Nếu lượt bị hủy đang giữ bản sao, xử lý điều chuyển bản sao cho người kế tiếp hoặc trả về AVAILABLE
        if (wasReady && copy != null) {
            reassignOrReleaseCopy(copy);
        }
    }

    /**
     * Gán bản sao vừa trả cho người đặt trước đầu tiên trong hàng đợi (BR11)
     * Trả về Reservation nếu gán thành công, null nếu không có ai chờ.
     */
    @Transactional
    public Reservation assignReturnedCopy(BookCopy copy) {
        if (copy == null || copy.getBook() == null) {
            return null;
        }

        // Tìm lượt PENDING sớm nhất theo FIFO
        Optional<Reservation> nextResOpt = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.book.id = :bookId and r.status = 'PENDING' order by r.reservedAt asc")
                .parameter("bookId", copy.getBook().getId())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("member", FetchPlan.BASE)
                        .add("book", FetchPlan.BASE))
                .optional();

        if (nextResOpt.isPresent()) {
            Reservation nextRes = nextResOpt.get();
            OffsetDateTime now = OffsetDateTime.now();
            nextRes.setBookCopy(copy);
            nextRes.setStatus(ReservationStatus.READY.getId());
            nextRes.setNotifiedAt(now);
            nextRes.setHoldExpiresAt(now.plusDays(getHoldExpiryDays()));

            copy.setStatus(BookCopyStatus.RESERVED.getId());

            dataManager.save(nextRes, copy);
            log.info("Bản sao mã vạch '{}' đã được giữ chỗ cho độc giả '{}' (Hạn nhận: {})",
                    copy.getBarcode(), nextRes.getMember().getFullName(), nextRes.getHoldExpiresAt());
            return nextRes;
        } else {
            copy.setStatus(BookCopyStatus.AVAILABLE.getId());
            dataManager.save(copy);
            return null;
        }
    }

    /**
     * Gán thủ công một bản sao khả dụng (AVAILABLE) cho một lượt đặt trước đang chờ (PENDING).
     */
    @Transactional
    public BookCopy assignAvailableCopy(Long reservationId) {
        Reservation res = dataManager.load(Reservation.class)
                .id(reservationId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", FetchPlan.BASE)
                        .add("member", FetchPlan.BASE))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt đặt trước ID: " + reservationId));

        if (!ReservationStatus.PENDING.getId().equalsIgnoreCase(res.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gán bản sao cho lượt đặt trước đang ở trạng thái 'ĐANG CHỜ'!");
        }

        if (res.getBook() == null) {
            throw new IllegalStateException("Lượt đặt trước không có thông tin đầu sách!");
        }

        Optional<BookCopy> copyOpt = dataManager.load(BookCopy.class)
                .query("select c from BookCopy c where c.book.id = :bookId and c.status = 'AVAILABLE' order by c.id asc")
                .parameter("bookId", res.getBook().getId())
                .optional();

        if (copyOpt.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Hiện tại đầu sách '%s' không có bản sao nào sẵn sàng trên kệ (AVAILABLE) để gán!",
                    res.getBook().getTitle()));
        }

        BookCopy copy = copyOpt.get();
        OffsetDateTime now = OffsetDateTime.now();
        res.setBookCopy(copy);
        res.setStatus(ReservationStatus.READY.getId());
        res.setNotifiedAt(now);
        res.setHoldExpiresAt(now.plusDays(getHoldExpiryDays()));

        copy.setStatus(BookCopyStatus.RESERVED.getId());
        dataManager.save(res, copy);

        log.info("Đã gán bản sao '{}' cho lượt đặt trước của độc giả '{}'",
                copy.getBarcode(), res.getMember() != null ? res.getMember().getFullName() : "");
        return copy;
    }

    /**
     * Tự động quét và điều phối bản sao khả dụng (AVAILABLE) cho các lượt đặt trước đang chờ (PENDING).
     * Áp dụng khi thư viện nhập thêm sách mới, cập nhật bản sao hoặc thủ thư bấm làm mới.
     */
    @Transactional
    public int autoMatchPendingReservations() {
        List<Reservation> pendingList = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.status = 'PENDING' order by r.reservedAt asc")
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", FetchPlan.BASE)
                        .add("member", FetchPlan.BASE))
                .list();

        int matchedCount = 0;
        for (Reservation res : pendingList) {
            if (res.getBook() == null) {
                continue;
            }

            Optional<BookCopy> copyOpt = dataManager.load(BookCopy.class)
                    .query("select c from BookCopy c where c.book.id = :bookId and c.status = 'AVAILABLE' order by c.id asc")
                    .parameter("bookId", res.getBook().getId())
                    .optional();

            if (copyOpt.isPresent()) {
                BookCopy copy = copyOpt.get();
                OffsetDateTime now = OffsetDateTime.now();
                res.setBookCopy(copy);
                res.setStatus(ReservationStatus.READY.getId());
                res.setNotifiedAt(now);
                res.setHoldExpiresAt(now.plusDays(getHoldExpiryDays()));

                copy.setStatus(BookCopyStatus.RESERVED.getId());
                dataManager.save(res, copy);
                matchedCount++;

                log.info("Tự động gán bản sao '{}' cho lượt đặt trước ID {} của độc giả '{}'",
                        copy.getBarcode(), res.getId(), res.getMember() != null ? res.getMember().getFullName() : "");
            }
        }
        return matchedCount;
    }

    /**
     * Điều chuyển bản sao cho lượt đặt trước tiếp theo hoặc trả về kệ AVAILABLE
     */
    @Transactional
    public void reassignOrReleaseCopy(BookCopy copy) {
        assignReturnedCopy(copy);
    }

    /**
     * Hoàn tất lượt đặt trước khi độc giả thực hiện mượn sách (L04)
     */
    @Transactional
    public void fulfillReservationForLoanItem(BookCopy copy, Member member, LoanItem loanItem) {
        if (copy == null || member == null) {
            return;
        }

        Optional<Reservation> resOpt = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.bookCopy.id = :copyId and r.member.id = :memberId and r.status = 'READY'")
                .parameter("copyId", copy.getId())
                .parameter("memberId", member.getId())
                .optional();

        if (resOpt.isPresent()) {
            Reservation res = resOpt.get();
            res.setStatus(ReservationStatus.FULFILLED.getId());
            res.setFulfilledLoanItem(loanItem);
            dataManager.save(res);
            log.info("Lượt đặt trước ID {} của độc giả '{}' đã hoàn tất!", res.getId(), member.getFullName());
        }
    }

    /**
     * Kiểm tra xem bản sao có đang bị giữ chỗ cho một độc giả KHÁC không.
     * Trả về tên độc giả được giữ chỗ nếu bị giữ, null nếu hợp lệ cho mượn.
     */
    public String checkReservedForOtherMember(BookCopy copy, Member currentBorrower) {
        if (copy == null || !BookCopyStatus.RESERVED.getId().equalsIgnoreCase(copy.getStatus())) {
            return null;
        }

        Optional<Reservation> resOpt = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.bookCopy.id = :copyId and r.status = 'READY'")
                .parameter("copyId", copy.getId())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("member", FetchPlan.BASE))
                .optional();

        if (resOpt.isPresent()) {
            Reservation res = resOpt.get();
            if (currentBorrower != null && res.getMember() != null
                    && res.getMember().getId().equals(currentBorrower.getId())) {
                return null; // Đúng là người đặt -> được phép mượn
            }
            return res.getMember() != null ? String.format("%s (%s)", res.getMember().getFullName(), res.getMember().getMemberCode()) : "độc giả khác";
        }

        return null;
    }

    /**
     * Kiểm tra đầu sách có yêu cầu đặt trước đang PENDING không (chặn gia hạn L06)
     */
    public boolean hasPendingReservations(Book book) {
        if (book == null) {
            return false;
        }
        Long count = dataManager.loadValue(
                "select count(r) from Reservation r where r.book.id = :bookId and r.status = 'PENDING'",
                Long.class)
                .parameter("bookId", book.getId())
                .one();
        return count > 0;
    }

    /**
     * Quét và xử lý các lượt đặt trước READY đã quá thời hạn giữ chỗ (BR12)
     */
    @Transactional
    public int processExpiredReservations() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Reservation> expiredList = dataManager.load(Reservation.class)
                .query("select r from Reservation r where r.status = 'READY' and r.holdExpiresAt < :now")
                .parameter("now", now)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("book", FetchPlan.BASE)
                        .add("bookCopy", FetchPlan.BASE))
                .list();

        int processed = 0;
        for (Reservation res : expiredList) {
            BookCopy copy = res.getBookCopy();
            res.setStatus(ReservationStatus.EXPIRED.getId());
            res.setBookCopy(null);
            dataManager.save(res);
            processed++;

            if (copy != null) {
                reassignOrReleaseCopy(copy);
            }
        }
        return processed;
    }

    /**
     * Lấy các chỉ số thống kê đặt trước phục vụ dashboard/ribbon
     */
    public ReservationStats getStats() {
        Long pending = dataManager.loadValue(
                "select count(r) from Reservation r where r.status = 'PENDING'", Long.class).one();
        Long ready = dataManager.loadValue(
                "select count(r) from Reservation r where r.status = 'READY'", Long.class).one();
        Long fulfilled = dataManager.loadValue(
                "select count(r) from Reservation r where r.status = 'FULFILLED'", Long.class).one();
        Long expiredOrCancelled = dataManager.loadValue(
                "select count(r) from Reservation r where r.status in ('EXPIRED', 'CANCELLED')", Long.class).one();

        return new ReservationStats(pending, ready, fulfilled, expiredOrCancelled);
    }
}
