package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.Permission;
import com.company.librarymanagementsystem.entity.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class PermissionDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(PermissionDataInitializer.class);

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    public PermissionDataInitializer(DataManager dataManager, ObjectMapper objectMapper, MemberService memberService) {
        this.dataManager = dataManager;
        this.objectMapper = objectMapper;
        this.memberService = memberService;
    }

    /**
     * Tự động kiểm tra và import quyền cùng gán quyền mặc định cho các vai trò khi
     * ứng dụng khởi động.
     */
    @EventListener(ApplicationStartedEvent.class)
    @Authenticated
    public void onApplicationStarted() {
        try {
            int importedCount = importPermissionsFromJson();
            log.info("Khởi tạo danh sách quyền hoàn tất. Đã nạp mới/kiểm tra {} quyền vào database.", importedCount);
            assignDefaultPermissionsToRoles();
            int synced = memberService.syncAllExistingMembersAndUsers();
            if (synced > 0) {
                log.info("Tự động rà soát khởi động: Đã đồng bộ {} hồ sơ độc giả khớp với tên tài khoản người dùng.", synced);
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo danh sách quyền và vai trò mặc định: {}", e.getMessage(), e);
        }
    }

    /**
     * Trả về chuỗi JSON định nghĩa toàn bộ 43 quyền chuẩn trích xuất từ tài liệu
     * phân tích hệ thống.
     */
    public String getDefaultPermissionsJson() {
        return """
                [
                  {"module": "BOOK", "code": "BOOK_VIEW", "name": "Xem danh mục sách", "description": "Xem danh sách và chi tiết thông tin đầu sách"},
                  {"module": "BOOK", "code": "BOOK_CREATE", "name": "Thêm mới đầu sách", "description": "Thêm mới đầu sách vào thư viện"},
                  {"module": "BOOK", "code": "BOOK_EDIT", "name": "Cập nhật đầu sách", "description": "Cập nhật, chỉnh sửa thông tin đầu sách"},
                  {"module": "BOOK", "code": "BOOK_DELETE", "name": "Xóa / ẩn đầu sách", "description": "Xóa hoặc ẩn đầu sách khỏi danh mục"},
                  {"module": "BOOK", "code": "BOOK_IMPORT", "name": "Nhập sách từ Excel/CSV", "description": "Nhập danh mục sách hàng loạt từ file Excel/CSV"},
                  {"module": "BOOK", "code": "BOOK_EXPORT", "name": "Xuất danh mục sách", "description": "Xuất danh mục sách ra định dạng Excel/PDF"},
                  {"module": "BOOK", "code": "AUTHOR_MANAGE", "name": "Quản lý tác giả", "description": "Quản lý danh sách tác giả và dịch giả"},
                  {"module": "BOOK", "code": "CATEGORY_MANAGE", "name": "Quản lý thể loại", "description": "Quản lý cây thể loại và danh mục phân loại sách"},
                  {"module": "BOOK", "code": "PUBLISHER_MANAGE", "name": "Quản lý nhà xuất bản", "description": "Quản lý thông tin nhà xuất bản"},
                  {"module": "BOOK", "code": "LOCATION_MANAGE", "name": "Quản lý vị trí kệ sách", "description": "Quản lý vị trí giá kệ sách trong thư viện"},
                  {"module": "BOOK", "code": "COPY_VIEW", "name": "Xem bản sao sách", "description": "Tra cứu và xem danh sách bản sao vật lý"},
                  {"module": "BOOK", "code": "COPY_CREATE", "name": "Thêm bản sao & mã vạch", "description": "Thêm bản sao vật lý và sinh mã vạch (Barcode/RFID)"},
                  {"module": "BOOK", "code": "COPY_EDIT", "name": "Cập nhật tình trạng bản sao", "description": "Cập nhật tình trạng bản sao (bảo trì, mất, hỏng)"},
                  {"module": "BOOK", "code": "COPY_WITHDRAW", "name": "Thanh lý bản sao sách", "description": "Đề xuất và phê duyệt thanh lý bản sao sách"},

                  {"module": "MEMBER", "code": "MEMBER_VIEW", "name": "Xem danh sách độc giả", "description": "Xem danh sách và hồ sơ bạn đọc"},
                  {"module": "MEMBER", "code": "MEMBER_CREATE", "name": "Thêm độc giả & cấp thẻ", "description": "Đăng ký độc giả mới và cấp thẻ thư viện"},
                  {"module": "MEMBER", "code": "MEMBER_EDIT", "name": "Cập nhật hồ sơ độc giả", "description": "Cập nhật thông tin độc giả và liên hệ"},
                  {"module": "MEMBER", "code": "MEMBER_LOCK", "name": "Khóa / Mở khóa thẻ", "description": "Khóa hoặc mở khóa thẻ thư viện độc giả"},
                  {"module": "MEMBER", "code": "MEMBER_CARD_RENEW", "name": "Gia hạn / Cấp lại thẻ", "description": "Gia hạn thời hạn thẻ hoặc cấp lại thẻ bị mất"},
                  {"module": "MEMBER", "code": "MEMBER_TYPE_MANAGE", "name": "Quản lý loại độc giả", "description": "Quản lý các loại độc giả và chính sách hạn mức mượn"},

                  {"module": "LOAN", "code": "LOAN_VIEW", "name": "Xem phiếu mượn trả", "description": "Xem danh sách và chi tiết phiếu mượn trả"},
                  {"module": "LOAN", "code": "LOAN_CREATE", "name": "Lập phiếu mượn sách", "description": "Lập phiếu mượn sách cho độc giả tại quầy"},
                  {"module": "LOAN", "code": "LOAN_RETURN", "name": "Tiếp nhận trả sách", "description": "Tiếp nhận trả sách và kiểm tra tình trạng sách trả"},
                  {"module": "LOAN", "code": "LOAN_RENEW", "name": "Gia hạn thời hạn mượn", "description": "Gia hạn thời hạn mượn cho cuốn sách"},
                  {"module": "LOAN", "code": "RESERVATION_VIEW", "name": "Xem danh sách đặt trước", "description": "Xem danh sách đặt trước và hàng đợi mượn sách"},
                  {"module": "LOAN", "code": "RESERVATION_MANAGE", "name": "Xử lý đặt trước sách", "description": "Xử lý đặt trước và thông báo giữ sách tại quầy"},
                  {"module": "LOAN", "code": "RESERVATION_CANCEL", "name": "Hủy yêu cầu đặt trước", "description": "Hủy yêu cầu đặt trước sách"},

                  {"module": "FINE", "code": "FINE_VIEW", "name": "Xem danh sách tiền phạt", "description": "Xem danh sách các khoản phạt và công nợ"},
                  {"module": "FINE", "code": "FINE_CREATE", "name": "Tạo khoản phạt", "description": "Tạo khoản phạt quá hạn, làm hỏng hoặc mất sách"},
                  {"module": "FINE", "code": "FINE_PAYMENT", "name": "Thu tiền phạt & in biên lai", "description": "Thu tiền phạt và xuất biên lai thanh toán"},
                  {"module": "FINE", "code": "FINE_WAIVE", "name": "Miễn giảm khoản phạt", "description": "Miễn giảm hoặc hủy bỏ khoản phạt (cần phê duyệt)"},

                  {"module": "REPORT", "code": "REPORT_VIEW", "name": "Xem Dashboard thống kê", "description": "Xem Dashboard thống kê và các chỉ số vận hành"},
                  {"module": "REPORT", "code": "REPORT_OVERDUE", "name": "Báo cáo sách quá hạn", "description": "Xem và xuất danh sách sách quá hạn chưa trả"},
                  {"module": "REPORT", "code": "REPORT_REVENUE", "name": "Báo cáo doanh thu tiền phạt", "description": "Báo cáo doanh thu thu tiền phạt và phí dịch vụ"},
                  {"module": "REPORT", "code": "REPORT_INVENTORY", "name": "Báo cáo kiểm kê kho", "description": "Báo cáo thống kê kiểm kê kho và lưu thông sách"},
                  {"module": "REPORT", "code": "REPORT_EXPORT", "name": "Xuất file báo cáo", "description": "Xuất các loại báo cáo thống kê ra Excel/PDF"},

                  {"module": "SYSTEM", "code": "USER_VIEW", "name": "Xem danh sách người dùng", "description": "Xem danh sách tài khoản người dùng hệ thống"},
                  {"module": "SYSTEM", "code": "USER_MANAGE", "name": "Quản lý người dùng", "description": "Thêm, sửa, khóa và đặt lại mật khẩu tài khoản"},
                  {"module": "SYSTEM", "code": "ROLE_MANAGE", "name": "Quản lý vai trò & quyền", "description": "Quản lý vai trò và phân quyền (Roles & Permissions)"},
                  {"module": "SYSTEM", "code": "SETTING_MANAGE", "name": "Cấu hình tham số hệ thống", "description": "Cấu hình tham số và chính sách thư viện"},
                  {"module": "SYSTEM", "code": "HOLIDAY_MANAGE", "name": "Quản lý lịch nghỉ lễ", "description": "Quản lý lịch nghỉ lễ và ngày đóng cửa thư viện"},
                  {"module": "SYSTEM", "code": "NOTIFICATION_MANAGE", "name": "Quản lý thông báo hệ thống", "description": "Quản lý và phát thông báo hệ thống"},
                  {"module": "SYSTEM", "code": "AUDIT_LOG_VIEW", "name": "Xem nhật ký thao tác", "description": "Tra cứu nhật ký thao tác và audit log hệ thống"}
                ]
                """;
    }

    @Authenticated
    public int importPermissionsFromJson() {
        try {
            List<Map<String, String>> items = objectMapper.readValue(
                    getDefaultPermissionsJson(),
                    new TypeReference<>() {
                    });

            int count = 0;
            for (Map<String, String> item : items) {
                String code = item.get("code");
                String name = item.get("name");
                String module = item.get("module");
                String description = item.get("description");

                Optional<Permission> existing = dataManager.load(Permission.class)
                        .query("select p from Permission p where p.code = :code")
                        .parameter("code", code)
                        .optional();

                if (existing.isEmpty()) {
                    Permission permission = dataManager.create(Permission.class);
                    permission.setCode(code);
                    permission.setName(name);
                    permission.setModule(module);
                    permission.setDescription(description);
                    dataManager.save(permission);
                    count++;
                } else {
                    Permission permission = existing.get();
                    if (!Objects.equals(permission.getName(), name)
                            || !Objects.equals(permission.getDescription(), description)
                            || !Objects.equals(permission.getModule(), module)) {
                        permission.setName(name);
                        permission.setDescription(description);
                        permission.setModule(module);
                        dataManager.save(permission);
                    }
                }
            }
            return count;
        } catch (Exception e) {
            log.error("Lỗi khi import quyền từ JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Import quyền thất bại", e);
        }
    }

    /**
     * Gán quyền mặc định cho các vai trò chuẩn của hệ thống: ADMIN, MANAGER,
     * LIBRARIAN, CATALOGER, READER.
     */
    @Authenticated
    public void assignDefaultPermissionsToRoles() {
        List<Permission> allPermissions = dataManager.load(Permission.class).all().list();
        Map<String, Permission> permMap = new HashMap<>();
        for (Permission p : allPermissions) {
            permMap.put(p.getCode(), p);
        }

        List<Role> roles = dataManager.load(Role.class)
                .query("select distinct r from Role r left join fetch r.permissions")
                .list();

        for (Role role : roles) {
            if (role.getPermissions() == null) {
                role.setPermissions(new ArrayList<>());
            }

            Set<String> targetCodes = switch (role.getCode()) {
                case "ADMIN" -> permMap.keySet();
                case "MANAGER" -> Set.of(
                        "BOOK_VIEW", "BOOK_EXPORT", "COPY_VIEW", "COPY_WITHDRAW",
                        "MEMBER_VIEW", "MEMBER_LOCK", "MEMBER_TYPE_MANAGE",
                        "LOAN_VIEW", "RESERVATION_VIEW",
                        "FINE_VIEW", "FINE_WAIVE",
                        "REPORT_VIEW", "REPORT_OVERDUE", "REPORT_REVENUE", "REPORT_INVENTORY", "REPORT_EXPORT",
                        "HOLIDAY_MANAGE", "AUDIT_LOG_VIEW");
                case "LIBRARIAN" -> Set.of(
                        "BOOK_VIEW", "COPY_VIEW",
                        "MEMBER_VIEW", "MEMBER_CREATE", "MEMBER_EDIT", "MEMBER_LOCK", "MEMBER_CARD_RENEW",
                        "LOAN_VIEW", "LOAN_CREATE", "LOAN_RETURN", "LOAN_RENEW", "RESERVATION_VIEW",
                        "RESERVATION_MANAGE", "RESERVATION_CANCEL",
                        "FINE_VIEW", "FINE_CREATE", "FINE_PAYMENT", "FINE_WAIVE",
                        "REPORT_VIEW", "REPORT_OVERDUE",
                        "NOTIFICATION_MANAGE");
                case "CATALOGER" -> Set.of(
                        "BOOK_VIEW", "BOOK_CREATE", "BOOK_EDIT", "BOOK_DELETE", "BOOK_IMPORT", "BOOK_EXPORT",
                        "AUTHOR_MANAGE", "CATEGORY_MANAGE", "PUBLISHER_MANAGE", "LOCATION_MANAGE",
                        "COPY_VIEW", "COPY_CREATE", "COPY_EDIT", "COPY_WITHDRAW",
                        "REPORT_INVENTORY");
                case "READER" -> Set.of(
                        "BOOK_VIEW", "COPY_VIEW",
                        "LOAN_VIEW", "LOAN_RENEW",
                        "RESERVATION_VIEW", "RESERVATION_MANAGE", "RESERVATION_CANCEL",
                        "FINE_VIEW");
                default -> Collections.emptySet();
            };

            boolean changed = false;

            // Loại bỏ COPY_EDIT nếu còn tồn tại trong vai trò LIBRARIAN
            if ("LIBRARIAN".equals(role.getCode())) {
                boolean removed = role.getPermissions().removeIf(p -> "COPY_EDIT".equals(p.getCode()));
                if (removed) {
                    changed = true;
                }
            }

            Set<String> currentCodes = new HashSet<>();
            for (Permission p : role.getPermissions()) {
                currentCodes.add(p.getCode());
            }

            for (String code : targetCodes) {
                if (!currentCodes.contains(code) && permMap.containsKey(code)) {
                    role.getPermissions().add(permMap.get(code));
                    changed = true;
                }
            }

            if (changed) {
                dataManager.save(role);
                log.info("Đã cập nhật danh sách quyền cho vai trò '{}'.", role.getCode());
            }
        }
    }
}
