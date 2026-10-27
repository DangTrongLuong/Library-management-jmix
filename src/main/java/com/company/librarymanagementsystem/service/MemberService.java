package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.MemberStatus;
import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    @Autowired
    private DataManager dataManager;

    @Autowired
    private LibrarySettingService settingService;

    /**
     * Tìm kiếm thẻ thư viện theo mã thẻ hoặc số CCCD để liên kết tài khoản
     */
    public Optional<Member> findMemberForLinking(String memberCode, String idNumber) {
        if ((memberCode == null || memberCode.isBlank()) && (idNumber == null || idNumber.isBlank())) {
            return Optional.empty();
        }

        String codeParam = memberCode != null ? memberCode.trim().toUpperCase() : "";
        String idParam = idNumber != null ? idNumber.trim() : "";

        return dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.deletedAt is null " +
                        "and (:code = '' or upper(m.memberCode) = :code) " +
                        "and (:idNum = '' or m.idNumber = :idNum)")
                .parameter("code", codeParam)
                .parameter("idNum", idParam)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("memberType", FetchPlan.BASE)
                        .add("user", FetchPlan.BASE))
                .optional();
    }

    /**
     * Thực hiện liên kết tài khoản User vào hồ sơ Thẻ thư viện Member
     */
    @Transactional
    public Member linkUserToMember(User user, Long memberId) {
        if (user == null) {
            throw new IllegalArgumentException("Không xác định được người dùng cần liên kết!");
        }

        // 1. Kiểm tra tài khoản này đã liên kết với thẻ nào chưa
        Optional<Member> existing = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user = :u and m.deletedAt is null")
                .parameter("u", user)
                .optional();
        if (existing.isPresent() && !existing.get().getId().equals(memberId)) {
            throw new IllegalStateException(String.format(
                    "Tài khoản của bạn đã được liên kết với thẻ %s. Không thể liên kết thêm thẻ khác!",
                    existing.get().getMemberCode()));
        }

        // 2. Tải thẻ cần liên kết
        Member target = dataManager.load(Member.class)
                .id(memberId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("memberType", FetchPlan.BASE)
                        .add("user", FetchPlan.BASE))
                .one();

        // 3. Kiểm tra thẻ này đã liên kết với tài khoản nào khác chưa
        if (target.getUser() != null && !target.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException(String.format(
                    "Thẻ thư viện %s đã được liên kết với tài khoản '%s'. Vui lòng liên hệ thủ thư tại quầy nếu cần hỗ trợ!",
                    target.getMemberCode(), target.getUser().getUsername()));
        }

        // 4. Gán User cho Member và đồng bộ họ tên
        target.setUser(user);
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            target.setFullName(user.getFullName().trim());
        }
        return dataManager.save(target);
    }

    /**
     * Hủy liên kết tài khoản User khỏi thẻ thư viện Member
     */
    @Transactional
    public void unlinkUserFromMember(User user) {
        if (user == null) return;
        Optional<Member> memberOpt = dataManager.load(Member.class)
                .query("select m from LibraryMember m where m.user = :u and m.deletedAt is null")
                .parameter("u", user)
                .optional();
        if (memberOpt.isPresent()) {
            Member m = memberOpt.get();
            m.setUser(null);
            dataManager.save(m);
        }
    }

    /**
     * Tự động sinh mã thẻ thư viện dạng LIB{YYYY}-{000001}
     */
    public synchronized String generateNextMemberCode() {
        int currentYear = Year.now().getValue();
        String prefix = String.format("LIB%d-", currentYear);

        List<String> codes = dataManager.loadValue(
                        "select m.memberCode from LibraryMember m " +
                                "where m.memberCode like concat(:prefix, '%') " +
                                "order by m.memberCode desc", String.class)
                .parameter("prefix", prefix)
                .maxResults(1)
                .list();

        long nextSeq = 1L;
        if (!codes.isEmpty() && codes.get(0) != null) {
            String lastCode = codes.get(0);
            try {
                String numStr = lastCode.substring(lastCode.lastIndexOf('-') + 1);
                nextSeq = Long.parseLong(numStr) + 1;
            } catch (Exception ignored) {
            }
        }
        return String.format("%s%06d", prefix, nextSeq);
    }

    /**
     * Gia hạn thẻ thư viện thêm số tháng theo chính sách của loại độc giả
     */
    @Transactional
    public Member renewCard(Member member) {
        Member reloaded = dataManager.load(Member.class).id(member.getId()).one();
        int monthsToAdd = 12;
        if (reloaded.getMemberType() != null && reloaded.getMemberType().getCardValidMonths() != null) {
            monthsToAdd = reloaded.getMemberType().getCardValidMonths();
        }

        LocalDate baseDate = LocalDate.now();
        if (reloaded.getCardExpiryDate() != null && reloaded.getCardExpiryDate().isAfter(LocalDate.now())) {
            baseDate = reloaded.getCardExpiryDate();
        }
        reloaded.setCardExpiryDate(baseDate.plusMonths(monthsToAdd));

        if (MemberStatus.EXPIRED.getId().equalsIgnoreCase(reloaded.getStatus())) {
            reloaded.setStatus(MemberStatus.ACTIVE.getId());
            reloaded.setStatusReason(null);
        }

        return dataManager.save(reloaded);
    }

    /**
     * Tạm khóa thẻ độc giả
     */
    @Transactional
    public Member suspendMember(Member member, String reason) {
        Member reloaded = dataManager.load(Member.class).id(member.getId()).one();
        reloaded.setStatus(MemberStatus.SUSPENDED.getId());
        reloaded.setStatusReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Tạm khóa theo yêu cầu thủ thư");
        return dataManager.save(reloaded);
    }

    /**
     * Mở lại trạng thái hoạt động hoặc duyệt cấp thẻ chính thức cho độc giả
     */
    @Transactional
    public Member activateMember(Member member) {
        Member reloaded = dataManager.load(Member.class)
                .id(member.getId())
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("memberType", FetchPlan.BASE))
                .one();

        // Nếu thẻ đang ở trạng thái PENDING (đăng ký trực tuyến chờ cấp thẻ tại quầy)
        // Khi thủ thư duyệt cấp thẻ, cập nhật ngày cấp chính thức là hôm nay và tính hạn thẻ theo quy định đối tượng
        if (MemberStatus.PENDING.getId().equalsIgnoreCase(reloaded.getStatus())) {
            reloaded.setCardIssuedDate(LocalDate.now());
            int validMonths = 12;
            if (reloaded.getMemberType() != null && reloaded.getMemberType().getCardValidMonths() != null) {
                validMonths = reloaded.getMemberType().getCardValidMonths();
            }
            reloaded.setCardExpiryDate(LocalDate.now().plusMonths(validMonths));
        }

        reloaded.setStatus(MemberStatus.ACTIVE.getId());
        reloaded.setStatusReason(null);
        return dataManager.save(reloaded);
    }

    @Autowired
    @org.springframework.context.annotation.Lazy
    private LoanService loanService;

    /**
     * Kiểm tra điều kiện mượn sách (Eligibility Check - L03)
     */
    public EligibilityResult checkEligibility(Member member) {
        EligibilityResult result = new EligibilityResult();
        result.setMemberCode(member.getMemberCode());
        result.setMemberName(member.getFullName() != null ? member.getFullName() : "");
        result.setStatus(member.getStatus());

        MemberType type = member.getMemberType();
        int maxLoans = (type != null && type.getMaxLoanBooks() != null) ? type.getMaxLoanBooks() : 5;
        result.setMaxLoanBooks(maxLoans);

        List<String> reasons = new ArrayList<>();

        // 1. Kiểm tra trạng thái thẻ (BR01)
        if (member.isExpired() || MemberStatus.EXPIRED.getId().equalsIgnoreCase(member.getStatus())) {
            reasons.add(String.format("Thẻ thư viện đã hết hạn từ ngày %s. Cần gia hạn thẻ trước khi mượn sách!",
                    member.getCardExpiryDate()));
        } else if (MemberStatus.SUSPENDED.getId().equalsIgnoreCase(member.getStatus())) {
            reasons.add(String.format("Thẻ đang bị TẠM KHÓA! Lý do: %s",
                    member.getStatusReason() != null ? member.getStatusReason() : "Vi phạm quy định"));
        } else if (MemberStatus.BLOCKED.getId().equalsIgnoreCase(member.getStatus())) {
            reasons.add("Thẻ đang bị KHÓA VĨNH VIỄN! Cần Quản lý thư viện (Manager) xem xét mở khóa.");
        } else if (MemberStatus.PENDING.getId().equalsIgnoreCase(member.getStatus())) {
            reasons.add("Hồ sơ thẻ đang ở trạng thái CHỜ DUYỆT. Thủ thư cần kích hoạt thẻ trước.");
        }

        // 2. Kiểm tra số sách đang mượn, quá hạn và tiền phạt thực tế từ DB
        int currentLoans = loanService != null ? loanService.countCurrentLoans(member.getId()) : 0;
        int overdueLoans = loanService != null ? loanService.countOverdueLoans(member.getId()) : 0;
        BigDecimal unpaidFines = loanService != null ? loanService.calculateUnpaidFines(member.getId()) : BigDecimal.ZERO;

        result.setCurrentLoans(currentLoans);
        result.setOverdueLoans(overdueLoans);
        result.setUnpaidFines(unpaidFines);

        // BR02: Chặn khi nợ phạt vượt ngưỡng cấu hình (MAX_UNPAID_FINE_THRESHOLD)
        BigDecimal maxFineThreshold = settingService != null ? settingService.getMaxUnpaidFineThreshold() : BigDecimal.valueOf(50000);
        if (unpaidFines.compareTo(maxFineThreshold) > 0) {
            reasons.add(String.format("Độc giả đang có khoản phạt chưa thanh toán (%,.0f VNĐ) vượt ngưỡng cho phép (%,.0f VNĐ). Cần nộp phạt trước khi mượn!", unpaidFines, maxFineThreshold));
        }

        // BR03: Chặn khi có sách quá hạn
        if (overdueLoans > 0) {
            reasons.add(String.format("Độc giả đang có %d cuốn sách quá hạn chưa trả. Cần trả sách trước khi mượn tiếp!", overdueLoans));
        }

        // BR04: Chặn khi đã đạt hạn mức mượn tối đa
        if (currentLoans >= maxLoans) {
            reasons.add(String.format("Độc giả đã mượn đủ số sách tối đa (%d / %d cuốn). Không thể mượn thêm!", currentLoans, maxLoans));
        }

        result.setEligible(reasons.isEmpty());
        result.setBlockingReasons(reasons);
        return result;
    }

    /**
     * Đồng bộ họ tên từ User sang Member khi Admin cập nhật thông tin Người dùng
     */
    @Transactional
    public void syncMemberFromUser(User user) {
        if (user == null || user.getId() == null) return;
        try {
            Optional<Member> memberOpt = dataManager.load(Member.class)
                    .query("select m from LibraryMember m where m.user.id = :uid and m.deletedAt is null")
                    .parameter("uid", user.getId())
                    .optional();

            if (memberOpt.isPresent()) {
                Member m = memberOpt.get();
                String userFullName = user.getFullName();
                if (userFullName == null || userFullName.isBlank()) {
                    String last = user.getLastName() != null ? user.getLastName().trim() : "";
                    String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
                    userFullName = (last + " " + first).trim();
                }
                if (userFullName.isBlank()) {
                    userFullName = user.getUsername();
                }

                if (!userFullName.equals(m.getFullName())) {
                    m.setFullName(userFullName);
                    dataManager.save(m);
                    log.info("Đã đồng bộ họ tên độc giả Member [{}] theo User: {}", m.getMemberCode(), userFullName);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ họ tên từ User sang Member: {}", e.getMessage(), e);
        }
    }

    /**
     * Đồng bộ họ tên từ Member sang User khi Thủ thư/Admin cập nhật thông tin Độc giả
     */
    @Transactional
    public void syncUserFromMember(Member member) {
        if (member == null || member.getUser() == null || member.getUser().getId() == null) return;
        try {
            User user = dataManager.load(User.class)
                    .id(member.getUser().getId())
                    .optional()
                    .orElse(null);

            if (user != null) {
                String memberFullName = member.getFullName() != null ? member.getFullName().trim() : "";
                if (!memberFullName.isEmpty() && !memberFullName.equals(user.getFullName())) {
                    user.setFullName(memberFullName);
                    int lastSpace = memberFullName.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        user.setLastName(memberFullName.substring(0, lastSpace).trim());
                        user.setFirstName(memberFullName.substring(lastSpace + 1).trim());
                    } else {
                        user.setLastName("");
                        user.setFirstName(memberFullName);
                    }
                    dataManager.save(user);
                    log.info("Đã đồng bộ họ tên User [{}] theo Member: {}", user.getUsername(), memberFullName);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ họ tên từ Member sang User: {}", e.getMessage(), e);
        }
    }

    /**
     * Tự động quét và đồng bộ các tài khoản Độc giả / Người dùng đang bị lệch tên trong toàn hệ thống
     */
    @Transactional
    public int syncAllExistingMembersAndUsers() {
        try {
            List<Member> members = dataManager.load(Member.class)
                    .query("select m from LibraryMember m where m.user is not null and m.deletedAt is null")
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("user", FetchPlan.BASE))
                    .list();

            int count = 0;
            List<Member> toSave = new ArrayList<>();
            for (Member m : members) {
                User u = m.getUser();
                if (u != null) {
                    String userFullName = u.getFullName();
                    if (userFullName == null || userFullName.isBlank()) {
                        String last = u.getLastName() != null ? u.getLastName().trim() : "";
                        String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
                        userFullName = (last + " " + first).trim();
                    }
                    if (userFullName != null && !userFullName.isBlank() && !userFullName.equals(m.getFullName())) {
                        m.setFullName(userFullName);
                        toSave.add(m);
                        count++;
                    }
                }
            }
            if (!toSave.isEmpty()) {
                dataManager.save(toSave.toArray());
                log.info("Đã tự động rà soát & đồng bộ {} hồ sơ độc giả bị lệch tên theo tài khoản người dùng.", count);
            }
            return count;
        } catch (Exception e) {
            log.error("Lỗi khi quét đồng bộ tên độc giả: {}", e.getMessage(), e);
            return 0;
        }
    }

    public static class EligibilityResult {
        private String memberCode;
        private String memberName;
        private String status;
        private boolean eligible;
        private int currentLoans;
        private int maxLoanBooks;
        private int overdueLoans;
        private BigDecimal unpaidFines;
        private List<String> blockingReasons = new ArrayList<>();

        public String getMemberCode() { return memberCode; }
        public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }

        public int getCurrentLoans() { return currentLoans; }
        public void setCurrentLoans(int currentLoans) { this.currentLoans = currentLoans; }

        public int getMaxLoanBooks() { return maxLoanBooks; }
        public void setMaxLoanBooks(int maxLoanBooks) { this.maxLoanBooks = maxLoanBooks; }

        public int getOverdueLoans() { return overdueLoans; }
        public void setOverdueLoans(int overdueLoans) { this.overdueLoans = overdueLoans; }

        public BigDecimal getUnpaidFines() { return unpaidFines; }
        public void setUnpaidFines(BigDecimal unpaidFines) { this.unpaidFines = unpaidFines; }

        public List<String> getBlockingReasons() { return blockingReasons; }
        public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    }
}
