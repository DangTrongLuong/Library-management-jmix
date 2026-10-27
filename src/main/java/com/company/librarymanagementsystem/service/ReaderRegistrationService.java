package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ReaderRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ReaderRegistrationService.class);

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[0-9]{9,10}$");

    @Autowired
    private DataManager dataManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private MemberService memberService;

    @Autowired
    private AuditLogService auditLogService;

    public record RegistrationRequest(
            String username,
            String fullName,
            String email,
            String phone,
            String password,
            String confirmPassword,
            Long memberTypeId
    ) {}

    public record RegistrationResult(
            boolean success,
            String message,
            String username,
            String memberCode,
            String memberTypeName,
            String memberStatus
    ) {
        public static RegistrationResult error(String message) {
            return new RegistrationResult(false, message, null, null, null, null);
        }

        public static RegistrationResult success(String message, String username, String memberCode, String memberTypeName, String memberStatus) {
            return new RegistrationResult(true, message, username, memberCode, memberTypeName, memberStatus);
        }
    }

    /**
     * Xác thực thông tin đầu vào của form đăng ký
     */
    public void validate(RegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ!");
        }

        // 1. Tên đăng nhập
        if (request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập!");
        }
        String username = request.username().trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập phải từ 4-30 ký tự, chỉ gồm chữ cái, số, dấu chấm, gạch dưới hoặc gạch ngang!");
        }

        // 2. Họ và tên
        if (request.fullName() == null || request.fullName().trim().length() < 2) {
            throw new IllegalArgumentException("Vui lòng nhập họ và tên đầy đủ (tối thiểu 2 ký tự)!");
        }

        // 3. Email
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email!");
        }
        String email = request.email().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Địa chỉ email không đúng định dạng (Ví dụ: ban.doc@example.com)!");
        }

        // 4. Số điện thoại
        if (request.phone() == null || request.phone().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại liên hệ!");
        }
        String phone = request.phone().trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (Ví dụ: 0912345678 hoặc +84912345678)!");
        }

        // 5. Mật khẩu
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có tối thiểu 6 ký tự!");
        }

        // 6. Xác nhận mật khẩu
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không trùng khớp!");
        }

        // 7. Loại đối tượng độc giả (bắt buộc)
        if (request.memberTypeId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại đối tượng độc giả (Sinh viên, Giảng viên,...)!");
        }
    }

    /**
     * Thực hiện đăng ký tài khoản cho Bạn đọc (Reader) với quyền hệ thống
     */
    public RegistrationResult registerReader(RegistrationRequest request) {
        // Kiểm tra hợp lệ đầu vào
        validate(request);

        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();
        String fullName = request.fullName().trim();

        // Chạy với System privileges để bypass phân quyền đối với khách chưa đăng nhập
        return systemAuthenticator.withSystem(() -> {
            // 1. Kiểm tra loại độc giả được chọn
            MemberType chosenType = dataManager.load(MemberType.class)
                    .id(request.memberTypeId())
                    .optional()
                    .orElseThrow(() -> new IllegalArgumentException("Loại độc giả được chọn không tồn tại trong hệ thống!"));

            // 2. Kiểm tra trùng lặp Tên đăng nhập
            Optional<User> existingUsername = dataManager.load(User.class)
                    .query("select u from User u where lower(u.username) = :uname")
                    .parameter("uname", username)
                    .optional();
            if (existingUsername.isPresent()) {
                return RegistrationResult.error("Tên đăng nhập '" + username + "' đã được sử dụng. Vui lòng chọn tên khác!");
            }

            // 3. Kiểm tra trùng lặp Email
            Optional<User> existingEmail = dataManager.load(User.class)
                    .query("select u from User u where lower(u.email) = :email and u.deletedAt is null")
                    .parameter("email", email)
                    .optional();
            if (existingEmail.isPresent()) {
                return RegistrationResult.error("Email '" + email + "' đã được đăng ký cho một tài khoản khác!");
            }

            // 4. Kiểm tra trùng lặp Số điện thoại trong bảng User
            Optional<User> existingPhone = dataManager.load(User.class)
                    .query("select u from User u where u.phone = :phone and u.deletedAt is null")
                    .parameter("phone", phone)
                    .optional();
            if (existingPhone.isPresent()) {
                return RegistrationResult.error("Số điện thoại '" + phone + "' đã được liên kết với một tài khoản khác!");
            }

            // 5. Tải vai trò READER và quyền mặc định
            Role readerRole = dataManager.load(Role.class)
                    .query("select r from Role r where upper(r.code) = 'READER' and r.deletedAt is null")
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                            .add("permissions", FetchPlan.BASE))
                    .optional()
                    .orElse(null);

            if (readerRole == null) {
                log.warn("Không tìm thấy vai trò READER trong CSDL! Đang tự động khởi tạo vai trò READER...");
                readerRole = dataManager.create(Role.class);
                readerRole.setCode("READER");
                readerRole.setName("Độc giả");
                readerRole.setDescription("Bạn đọc tra cứu, mượn sách, gia hạn và đặt trước trực tuyến");
                readerRole = dataManager.save(readerRole);
            }

            // 6. Tạo tài khoản User mới
            User newUser = dataManager.create(User.class);
            newUser.setUsername(username);
            newUser.setPassword(passwordEncoder.encode(request.password()));
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setRole(readerRole);
            if (readerRole.getPermissions() != null) {
                newUser.setPermissions(new ArrayList<>(readerRole.getPermissions()));
            } else {
                newUser.setPermissions(new ArrayList<>());
            }
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setActive(true);
            newUser.setMustChangePassword(false);
            newUser.setFailedLoginCount(0);
            newUser.setTimeZoneId("Asia/Ho_Chi_Minh");

            User savedUser = dataManager.save(newUser);
            log.info("Đã tạo mới tài khoản Reader: {} (id: {})", savedUser.getUsername(), savedUser.getId());

            // 7. Khởi tạo hồ sơ Thẻ thư viện (Member) ở trạng thái CHỜ DUYỆT (PENDING)
            // Thẻ phải do Thủ thư hoặc người có thẩm quyền kiểm tra giấy tờ và kích hoạt / cấp thẻ tại quầy
            Member newMember = dataManager.create(Member.class);
            newMember.setUser(savedUser);
            newMember.setFullName(savedUser.getFullName());
            newMember.setMemberCode(memberService.generateNextMemberCode());
            newMember.setMemberType(chosenType);
            newMember.setCardIssuedDate(LocalDate.now());

            int validMonths = chosenType.getCardValidMonths() != null ? chosenType.getCardValidMonths() : 12;
            newMember.setCardExpiryDate(LocalDate.now().plusMonths(validMonths));
            newMember.setStatus(MemberStatus.PENDING.getId());
            newMember.setStatusReason("Đăng ký trực tuyến - Chờ đến quầy thư viện xuất trình giấy tờ để cấp thẻ chính thức");

            Member targetMember = dataManager.save(newMember);
            log.info("Đã tạo hồ sơ thẻ thư viện chờ duyệt {} cho độc giả {} (Loại: {})",
                    targetMember.getMemberCode(), savedUser.getUsername(), chosenType.getName());

            // 8. Ghi Audit Log
            String actionDesc = String.format("Độc giả '%s' tự đăng ký tài khoản trực tuyến (Đối tượng: %s, Mã hồ sơ: %s, Trạng thái: CHỜ CẤP THẺ)",
                    savedUser.getUsername(),
                    chosenType.getName(),
                    targetMember.getMemberCode());
            auditLogService.log("READER_REGISTER", "User", savedUser.getId().toString(), actionDesc);

            String successMsg = String.format(
                    "Đăng ký tài khoản thành công! Hồ sơ loại độc giả '%s' với mã thẻ %s đã được tạo. Trạng thái: CHỜ CẤP THẺ. Vui lòng đến quầy thư viện để Thủ thư đối chiếu giấy tờ và cấp thẻ chính thức.",
                    chosenType.getName(),
                    targetMember.getMemberCode());

            return RegistrationResult.success(successMsg, savedUser.getUsername(), targetMember.getMemberCode(), chosenType.getName(), targetMember.getStatus());
        });
    }

    /**
     * Lấy loại độc giả mặc định (ưu tiên STUDENT, nếu không có lấy bản ghi đầu tiên)
     */
    private MemberType getDefaultMemberType() {
        Optional<MemberType> studentType = dataManager.load(MemberType.class)
                .query("select mt from MemberType mt where mt.code = 'STUDENT' and mt.deletedAt is null")
                .optional();
        if (studentType.isPresent()) {
            return studentType.get();
        }

        List<MemberType> allTypes = dataManager.load(MemberType.class)
                .query("select mt from MemberType mt where mt.deletedAt is null order by mt.id asc")
                .maxResults(1)
                .list();

        if (!allTypes.isEmpty()) {
            return allTypes.get(0);
        }

        // Tạo loại độc giả mặc định nếu bảng đang rỗng
        MemberType defaultType = dataManager.create(MemberType.class);
        defaultType.setCode("STUDENT");
        defaultType.setName("Sinh viên");
        defaultType.setMaxLoanBooks(5);
        defaultType.setLoanDays(14);
        defaultType.setMaxRenewals(1);
        defaultType.setRenewDays(7);
        defaultType.setMaxReservations(3);
        defaultType.setFinePerDay(java.math.BigDecimal.valueOf(5000));
        defaultType.setCardValidMonths(12);
        defaultType.setDescription("Loại độc giả mặc định cho bạn đọc tự đăng ký trực tuyến");
        return dataManager.save(defaultType);
    }
}
