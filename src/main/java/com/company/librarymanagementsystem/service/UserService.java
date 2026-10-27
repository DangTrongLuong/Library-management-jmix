package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final DataManager dataManager;
    private final CurrentAuthentication currentAuthentication;

    public UserService(DataManager dataManager, CurrentAuthentication currentAuthentication) {
        this.dataManager = dataManager;
        this.currentAuthentication = currentAuthentication;
    }

    public record UserRelatedSummary(
            User user,
            Member member,
            int borrowedBooksCount,
            int totalLoansCount,
            int pendingReservationsCount,
            int totalFinesCount,
            BigDecimal unpaidFinesTotal,
            int staffIssuedLoansCount,
            int staffReceivedItemsCount,
            int staffFinesCount,
            int staffPaymentsCount
    ) {
        public boolean hasReaderData() {
            return member != null;
        }

        public boolean hasStaffData() {
            return (staffIssuedLoansCount + staffReceivedItemsCount + staffFinesCount + staffPaymentsCount) > 0;
        }
    }

    /**
     * Kiểm tra và thống kê các dữ liệu liên đới của tài khoản trước khi xóa
     */
    public UserRelatedSummary inspectUserRelatedData(UUID userId) {
        if (userId == null) {
            return null;
        }

        User user = dataManager.load(User.class)
                .id(userId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("role", FetchPlan.BASE))
                .optional()
                .orElse(null);

        if (user == null) {
            return null;
        }

        // Tìm Member liên kết với user
        Optional<Member> optMember = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user.id = :userId")
                .parameter("userId", userId)
                .optional();

        Member member = optMember.orElse(null);
        int borrowedBooks = 0;
        int totalLoans = 0;
        int pendingReservations = 0;
        int totalFines = 0;
        BigDecimal unpaidFines = BigDecimal.ZERO;

        if (member != null) {
            Long borrowed = dataManager.loadValue(
                    "select count(li) from LoanItem li where li.loan.member.id = :memberId and li.status = 'BORROWED'",
                    Long.class)
                    .parameter("memberId", member.getId())
                    .one();
            borrowedBooks = borrowed != null ? borrowed.intValue() : 0;

            Long loans = dataManager.loadValue(
                    "select count(l) from Loan l where l.member.id = :memberId",
                    Long.class)
                    .parameter("memberId", member.getId())
                    .one();
            totalLoans = loans != null ? loans.intValue() : 0;

            Long res = dataManager.loadValue(
                    "select count(r) from Reservation r where r.member.id = :memberId and r.status in ('PENDING', 'READY')",
                    Long.class)
                    .parameter("memberId", member.getId())
                    .one();
            pendingReservations = res != null ? res.intValue() : 0;

            Long fines = dataManager.loadValue(
                    "select count(f) from Fine f where f.member.id = :memberId",
                    Long.class)
                    .parameter("memberId", member.getId())
                    .one();
            totalFines = fines != null ? fines.intValue() : 0;

            BigDecimal sumUnpaid = dataManager.loadValue(
                    "select coalesce(sum(f.amount - f.paidAmount), 0) from Fine f where f.member.id = :memberId and f.status in ('UNPAID', 'PARTIAL')",
                    BigDecimal.class)
                    .parameter("memberId", member.getId())
                    .one();
            unpaidFines = sumUnpaid != null ? sumUnpaid : BigDecimal.ZERO;
        }

        // Kiểm tra lịch sử nghiệp vụ thủ thư/nhân viên
        Long issuedLoans = dataManager.loadValue(
                "select count(l) from Loan l where l.issuedBy.id = :userId", Long.class)
                .parameter("userId", userId)
                .one();
        int staffIssuedLoans = issuedLoans != null ? issuedLoans.intValue() : 0;

        Long receivedItems = dataManager.loadValue(
                "select count(li) from LoanItem li where li.receivedBy.id = :userId", Long.class)
                .parameter("userId", userId)
                .one();
        int staffReceivedItems = receivedItems != null ? receivedItems.intValue() : 0;

        Long staffFines = dataManager.loadValue(
                "select count(f) from Fine f where f.createdBy.id = :userId or f.waivedBy.id = :userId", Long.class)
                .parameter("userId", userId)
                .one();
        int staffFinesCount = staffFines != null ? staffFines.intValue() : 0;

        Long staffPayments = dataManager.loadValue(
                "select count(p) from Payment p where p.receivedBy.id = :userId", Long.class)
                .parameter("userId", userId)
                .one();
        int staffPaymentsCount = staffPayments != null ? staffPayments.intValue() : 0;

        return new UserRelatedSummary(
                user, member,
                borrowedBooks, totalLoans, pendingReservations, totalFines, unpaidFines,
                staffIssuedLoans, staffReceivedItems, staffFinesCount, staffPaymentsCount
        );
    }

    /**
     * Xóa sạch người dùng và toàn bộ dữ liệu liên quan một cách an toàn và toàn vẹn
     */
    @Transactional
    public void deleteUserAndAllRelatedData(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Mã người dùng không hợp lệ!");
        }

        User user = dataManager.load(User.class)
                .id(userId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("role", FetchPlan.BASE))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng cần xóa!"));

        // 1. Bảo vệ tài khoản admin
        if ("admin".equalsIgnoreCase(user.getUsername()) || (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getCode()))) {
            throw new IllegalStateException("Không được phép xóa tài khoản Quản trị viên tối cao (Admin)!");
        }

        // 2. Bảo vệ tài khoản đang đăng nhập
        Object currentUserObj = currentAuthentication.getUser();
        if (currentUserObj instanceof User currentUser && currentUser.getId().equals(userId)) {
            throw new IllegalStateException("Bạn không thể tự xóa tài khoản của chính mình khi đang đăng nhập!");
        }

        // 3. Tìm tài khoản admin dự phòng để bàn giao các phiếu mượn/thu tiền do nhân viên này từng xử lý
        User systemAdmin = dataManager.load(User.class)
                .query("select u from User u where lower(u.username) = 'admin'")
                .optional()
                .orElse(null);

        // 4. Xử lý hồ sơ Độc giả (Member) nếu có
        Optional<Member> optMember = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user.id = :userId")
                .parameter("userId", userId)
                .optional();

        if (optMember.isPresent()) {
            Member member = optMember.get();
            Long memberId = member.getId();

            // 4.1 Xử lý các đặt trước (Reservations)
            List<Reservation> reservations = dataManager.load(Reservation.class)
                    .query("select r from Reservation r where r.member.id = :memberId")
                    .parameter("memberId", memberId)
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("bookCopy", FetchPlan.BASE))
                    .list();

            for (Reservation r : reservations) {
                // Nếu sách đang giữ chỗ cho người này (READY), trả lại trạng thái AVAILABLE trong kho
                if (r.getBookCopy() != null && "READY".equalsIgnoreCase(r.getStatus())) {
                    BookCopy copy = dataManager.load(BookCopy.class).id(r.getBookCopy().getId()).one();
                    copy.setStatus(BookCopyStatus.AVAILABLE.getId());
                    dataManager.save(copy);
                }
                dataManager.remove(r);
            }

            // 4.2 Xử lý các khoản thanh toán (Payments) của member
            List<Payment> payments = dataManager.load(Payment.class)
                    .query("select p from Payment p where p.member.id = :memberId")
                    .parameter("memberId", memberId)
                    .list();
            for (Payment p : payments) {
                dataManager.remove(p);
            }

            // 4.3 Xử lý các khoản tiền phạt (Fines) của member
            List<Fine> fines = dataManager.load(Fine.class)
                    .query("select f from Fine f where f.member.id = :memberId")
                    .parameter("memberId", memberId)
                    .list();
            for (Fine f : fines) {
                dataManager.remove(f);
            }

            // 4.4 Xử lý các phiếu mượn (Loans) và dòng mượn (LoanItems)
            List<Loan> loans = dataManager.load(Loan.class)
                    .query("select l from Loan l where l.member.id = :memberId")
                    .parameter("memberId", memberId)
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("loanItems", itemPlan -> itemPlan.addFetchPlan(FetchPlan.BASE).add("bookCopy", FetchPlan.BASE)))
                    .list();

            for (Loan l : loans) {
                if (l.getLoanItems() != null) {
                    for (LoanItem item : l.getLoanItems()) {
                        // Nếu cuốn sách đang mượn (BORROWED), giải phóng sách về trạng thái AVAILABLE trong kho
                        if ("BORROWED".equalsIgnoreCase(item.getStatus()) && item.getBookCopy() != null) {
                            BookCopy copy = dataManager.load(BookCopy.class).id(item.getBookCopy().getId()).one();
                            copy.setStatus(BookCopyStatus.AVAILABLE.getId());
                            dataManager.save(copy);
                        }
                    }
                }
                // Loan có cascade = CascadeType.ALL nên việc xóa Loan sẽ tự động xóa sạch các LoanItem thuộc về nó
                dataManager.remove(l);
            }

            // 4.5 Xóa hồ sơ Member
            dataManager.remove(member);
            log.info("Đã xóa hồ sơ độc giả mã {} liên kết với user {}", member.getMemberCode(), user.getUsername());
        }

        // 5. Xử lý các nghiệp vụ thủ thư/nhân viên (bàn giao sang tài khoản admin để bảo toàn dữ liệu lịch sử)
        if (systemAdmin != null) {
            List<Loan> staffIssuedLoans = dataManager.load(Loan.class)
                    .query("select l from Loan l where l.issuedBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Loan l : staffIssuedLoans) {
                l.setIssuedBy(systemAdmin);
                dataManager.save(l);
            }

            List<LoanItem> staffReceivedItems = dataManager.load(LoanItem.class)
                    .query("select li from LoanItem li where li.receivedBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (LoanItem li : staffReceivedItems) {
                li.setReceivedBy(systemAdmin);
                dataManager.save(li);
            }

            List<Fine> staffCreatedFines = dataManager.load(Fine.class)
                    .query("select f from Fine f where f.createdBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Fine f : staffCreatedFines) {
                f.setCreatedBy(systemAdmin);
                dataManager.save(f);
            }

            List<Fine> staffWaivedFines = dataManager.load(Fine.class)
                    .query("select f from Fine f where f.waivedBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Fine f : staffWaivedFines) {
                f.setWaivedBy(systemAdmin);
                dataManager.save(f);
            }

            List<Payment> staffPayments = dataManager.load(Payment.class)
                    .query("select p from Payment p where p.receivedBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Payment p : staffPayments) {
                p.setReceivedBy(systemAdmin);
                dataManager.save(p);
            }

            List<Renewal> staffRenewals = dataManager.load(Renewal.class)
                    .query("select r from Renewal r where r.renewedBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Renewal r : staffRenewals) {
                r.setRenewedBy(systemAdmin);
                dataManager.save(r);
            }

            List<Reservation> staffCancelledRes = dataManager.load(Reservation.class)
                    .query("select r from Reservation r where r.cancelledBy.id = :userId")
                    .parameter("userId", userId)
                    .list();
            for (Reservation r : staffCancelledRes) {
                r.setCancelledBy(systemAdmin);
                dataManager.save(r);
            }
        }

        // 6. Xóa User (JPA tự động dọn dẹp bảng quan hệ USER_PERMISSIONS)
        dataManager.remove(user);
        log.info("Đã xóa hoàn toàn người dùng {} (ID: {}) cùng toàn bộ dữ liệu liên quan thành công.", user.getUsername(), user.getId());
    }
}
