
package com.company.librarymanagementsystem.view.permission;

import com.company.librarymanagementsystem.entity.Permission;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

import java.text.Normalizer;
import java.util.*;

@Route(value = "permissions/:id", layout = MainView.class)
@ViewController(id = "Permission.detail")
@ViewDescriptor(path = "permission-detail-view.xml")
@EditedEntityContainer("permissionDc")
public class PermissionDetailView extends StandardDetailView<Permission> {

    @ViewComponent
    private Select<String> moduleField;

    @ViewComponent
    private ComboBox<String> nameField;

    @ViewComponent
    private TextField codeField;

    @ViewComponent
    private TextField descriptionField;

    @org.springframework.beans.factory.annotation.Autowired
    private io.jmix.core.EntityStates entityStates;

    @org.springframework.beans.factory.annotation.Autowired
    private com.company.librarymanagementsystem.service.ToastService toastService;

    private boolean newEntity;

    public record PermissionItem(String code, String name, String description) {
    }

    private static final Map<String, List<PermissionItem>> MODULE_ITEMS = new LinkedHashMap<>();

    static {
        MODULE_ITEMS.put("BOOK", List.of(
                new PermissionItem("BOOK_VIEW", "Xem danh mục sách", "Xem danh sách và chi tiết thông tin đầu sách"),
                new PermissionItem("BOOK_CREATE", "Thêm mới đầu sách", "Thêm mới đầu sách vào thư viện"),
                new PermissionItem("BOOK_EDIT", "Cập nhật đầu sách", "Cập nhật, chỉnh sửa thông tin đầu sách"),
                new PermissionItem("BOOK_DELETE", "Xóa / ẩn đầu sách", "Xóa hoặc ẩn đầu sách khỏi danh mục"),
                new PermissionItem("BOOK_IMPORT", "Nhập sách từ Excel/CSV",
                        "Nhập danh mục sách hàng loạt từ file Excel/CSV"),
                new PermissionItem("BOOK_EXPORT", "Xuất danh mục sách", "Xuất danh mục sách ra định dạng Excel/PDF"),
                new PermissionItem("AUTHOR_MANAGE", "Quản lý tác giả", "Quản lý danh sách tác giả và dịch giả"),
                new PermissionItem("CATEGORY_MANAGE", "Quản lý thể loại",
                        "Quản lý cây thể loại và danh mục phân loại sách"),
                new PermissionItem("PUBLISHER_MANAGE", "Quản lý nhà xuất bản", "Quản lý thông tin nhà xuất bản"),
                new PermissionItem("LOCATION_MANAGE", "Quản lý vị trí kệ sách",
                        "Quản lý vị trí giá kệ sách trong thư viện"),
                new PermissionItem("COPY_VIEW", "Xem bản sao sách", "Tra cứu và xem danh sách bản sao vật lý"),
                new PermissionItem("COPY_CREATE", "Thêm bản sao & mã vạch",
                        "Thêm bản sao vật lý và sinh mã vạch (Barcode/RFID)"),
                new PermissionItem("COPY_EDIT", "Cập nhật tình trạng bản sao",
                        "Cập nhật tình trạng bản sao (bảo trì, mất, hỏng)"),
                new PermissionItem("COPY_WITHDRAW", "Thanh lý bản sao sách",
                        "Đề xuất và phê duyệt thanh lý bản sao sách")));

        MODULE_ITEMS.put("MEMBER", List.of(
                new PermissionItem("MEMBER_VIEW", "Xem danh sách độc giả", "Xem danh sách và hồ sơ bạn đọc"),
                new PermissionItem("MEMBER_CREATE", "Thêm độc giả & cấp thẻ",
                        "Đăng ký độc giả mới và cấp thẻ thư viện"),
                new PermissionItem("MEMBER_EDIT", "Cập nhật hồ sơ độc giả", "Cập nhật thông tin độc giả và liên hệ"),
                new PermissionItem("MEMBER_LOCK", "Khóa / Mở khóa thẻ", "Khóa hoặc mở khóa thẻ thư viện độc giả"),
                new PermissionItem("MEMBER_CARD_RENEW", "Gia hạn / Cấp lại thẻ",
                        "Gia hạn thời hạn thẻ hoặc cấp lại thẻ bị mất"),
                new PermissionItem("MEMBER_TYPE_MANAGE", "Quản lý loại độc giả",
                        "Quản lý các loại độc giả và chính sách hạn mức mượn")));

        MODULE_ITEMS.put("LOAN", List.of(
                new PermissionItem("LOAN_VIEW", "Xem phiếu mượn trả", "Xem danh sách và chi tiết phiếu mượn trả"),
                new PermissionItem("LOAN_CREATE", "Lập phiếu mượn sách", "Lập phiếu mượn sách cho độc giả tại quầy"),
                new PermissionItem("LOAN_RETURN", "Tiếp nhận trả sách",
                        "Tiếp nhận trả sách và kiểm tra tình trạng sách trả"),
                new PermissionItem("LOAN_RENEW", "Gia hạn thời hạn mượn", "Gia hạn thời hạn mượn cho cuốn sách"),
                new PermissionItem("RESERVATION_VIEW", "Xem danh sách đặt trước",
                        "Xem danh sách đặt trước và hàng đợi mượn sách"),
                new PermissionItem("RESERVATION_MANAGE", "Xử lý đặt trước sách",
                        "Xử lý đặt trước và thông báo giữ sách tại quầy"),
                new PermissionItem("RESERVATION_CANCEL", "Hủy yêu cầu đặt trước", "Hủy yêu cầu đặt trước sách")));

        MODULE_ITEMS.put("FINE", List.of(
                new PermissionItem("FINE_VIEW", "Xem danh sách tiền phạt", "Xem danh sách các khoản phạt và công nợ"),
                new PermissionItem("FINE_CREATE", "Tạo khoản phạt", "Tạo khoản phạt quá hạn, làm hỏng hoặc mất sách"),
                new PermissionItem("FINE_PAYMENT", "Thu tiền phạt & in biên lai",
                        "Thu tiền phạt và xuất biên lai thanh toán"),
                new PermissionItem("FINE_WAIVE", "Miễn giảm khoản phạt",
                        "Miễn giảm hoặc hủy bỏ khoản phạt (cần phê duyệt)")));

        MODULE_ITEMS.put("REPORT", List.of(
                new PermissionItem("REPORT_VIEW", "Xem Dashboard thống kê",
                        "Xem Dashboard thống kê và các chỉ số vận hành"),
                new PermissionItem("REPORT_OVERDUE", "Báo cáo sách quá hạn",
                        "Xem và xuất danh sách sách quá hạn chưa trả"),
                new PermissionItem("REPORT_REVENUE", "Báo cáo doanh thu tiền phạt",
                        "Báo cáo doanh thu thu tiền phạt và phí dịch vụ"),
                new PermissionItem("REPORT_INVENTORY", "Báo cáo kiểm kê kho",
                        "Báo cáo thống kê kiểm kê kho và lưu thông sách"),
                new PermissionItem("REPORT_EXPORT", "Xuất file báo cáo",
                        "Xuất các loại báo cáo thống kê ra Excel/PDF")));

        MODULE_ITEMS.put("SYSTEM", List.of(
                new PermissionItem("USER_VIEW", "Xem danh sách người dùng",
                        "Xem danh sách tài khoản người dùng hệ thống"),
                new PermissionItem("USER_MANAGE", "Quản lý người dùng",
                        "Thêm, sửa, khóa và đặt lại mật khẩu tài khoản"),
                new PermissionItem("ROLE_MANAGE", "Quản lý vai trò & quyền",
                        "Quản lý vai trò và phân quyền (Roles & Permissions)"),
                new PermissionItem("SETTING_MANAGE", "Cấu hình tham số hệ thống",
                        "Cấu hình tham số và chính sách thư viện"),
                new PermissionItem("HOLIDAY_MANAGE", "Quản lý lịch nghỉ lễ",
                        "Quản lý lịch nghỉ lễ và ngày đóng cửa thư viện"),
                new PermissionItem("NOTIFICATION_MANAGE", "Quản lý thông báo hệ thống",
                        "Quản lý và phát thông báo hệ thống"),
                new PermissionItem("AUDIT_LOG_VIEW", "Xem nhật ký thao tác",
                        "Tra cứu nhật ký thao tác và audit log hệ thống")));
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        setShowSaveNotification(false);
        initModuleField();
        initNameField();
        initCodeField();
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        newEntity = entityStates.isNew(getEditedEntity());
    }

    @Subscribe
    public void onAfterSave(final AfterSaveEvent event) {
        String permName = getEditedEntity().getName() != null ? getEditedEntity().getName() : getEditedEntity().getCode();
        if (newEntity) {
            toastService.success(String.format("Tạo mới quyền '%s' thành công!", permName));
            newEntity = false;
        } else {
            toastService.success(String.format("Cập nhật thông tin quyền '%s' thành công!", permName));
        }
    }

    private void initModuleField() {
        moduleField.setItems(List.of("BOOK", "MEMBER", "LOAN", "FINE", "REPORT", "SYSTEM"));
        moduleField.setItemLabelGenerator(this::getModuleLabel);
        moduleField.setRenderer(new ComponentRenderer<>(mod -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle().set("gap", "8px");

            Span dot = new Span();
            dot.addClassName("perm-badge-dot");
            dot.addClassName("perm-dot-" + (mod != null ? mod.toLowerCase() : "other"));
            row.add(dot);

            Span text = new Span(getModuleLabel(mod));
            row.add(text);
            return row;
        }));
    }

    private String getModuleLabel(String mod) {
        return switch (mod != null ? mod : "") {
            case "BOOK" -> "Sách & Danh mục";
            case "MEMBER" -> "Độc giả & Thẻ";
            case "LOAN" -> "Mượn / Trả";
            case "FINE" -> "Tiền phạt";
            case "REPORT" -> "Báo cáo & Thống kê";
            case "SYSTEM" -> "Hệ thống & Phân quyền";
            default -> mod != null ? mod : "";
        };
    }

    private void initNameField() {
        nameField.addCustomValueSetListener(e -> nameField.setValue(e.getDetail()));
        nameField.setEnabled(false);
        nameField.setPlaceholder("Vui lòng chọn phân hệ trước...");
        nameField.setRenderer(new ComponentRenderer<>(name -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle().set("gap", "8px");

            Span dot = new Span();
            dot.addClassName("perm-badge-dot");
            String mod = moduleField.getValue();
            dot.addClassName("perm-dot-" + (mod != null ? mod.toLowerCase() : "other"));
            row.add(dot);

            Span text = new Span(name != null ? name : "");
            row.add(text);
            return row;
        }));
    }

    private void initCodeField() {
        codeField.setPlaceholder("Tự động sinh theo tên quyền...");
    }

    @Subscribe("moduleField")
    public void onModuleFieldValueChange(final ComponentValueChangeEvent<Select<String>, String> event) {
        String selectedModule = event.getValue();
        updateNameFieldForModule(selectedModule);
    }

    private void updateNameFieldForModule(String module) {
        if (module != null && MODULE_ITEMS.containsKey(module)) {
            nameField.setEnabled(true);
            nameField.setPlaceholder("Chọn hoặc nhập tên quyền...");
            List<String> names = MODULE_ITEMS.get(module).stream()
                    .map(PermissionItem::name)
                    .toList();
            nameField.setItems(names);
        } else {
            nameField.setEnabled(false);
            nameField.setPlaceholder("Vui lòng chọn phân hệ trước...");
            nameField.setItems(Collections.emptyList());
        }
    }

    @Subscribe("nameField")
    public void onNameFieldValueChange(final ComponentValueChangeEvent<ComboBox<String>, String> event) {
        String selectedName = event.getValue();
        if (selectedName != null && !selectedName.isBlank()) {
            String module = moduleField.getValue();
            if (module != null && MODULE_ITEMS.containsKey(module)) {
                Optional<PermissionItem> match = MODULE_ITEMS.get(module).stream()
                        .filter(item -> item.name().equalsIgnoreCase(selectedName))
                        .findFirst();

                if (match.isPresent()) {
                    PermissionItem item = match.get();
                    codeField.setValue(item.code());
                    String currentDesc = descriptionField.getValue();
                    if (currentDesc == null || currentDesc.isBlank()) {
                        descriptionField.setValue(item.description());
                    }
                } else {
                    // Tên quyền tự nhập (custom): nếu mã quyền đang trống thì sinh mã tự động
                    String currentCode = codeField.getValue();
                    if (currentCode == null || currentCode.isBlank()) {
                        codeField.setValue(generateCodeFromName(module, selectedName));
                    }
                }
            }
        }
    }

    private String generateCodeFromName(String module, String name) {
        if (name == null || name.isBlank())
            return "";
        String unaccented = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .toUpperCase()
                .replaceAll("^_+|_+$", "");
        return (module != null ? module + "_" : "") + unaccented;
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        Permission permission = getEditedEntity();
        if (permission.getModule() != null) {
            updateNameFieldForModule(permission.getModule());
        } else {
            nameField.setEnabled(false);
            nameField.setPlaceholder("Vui lòng chọn phân hệ trước...");
        }

        // Đảm bảo không hiển thị lỗi đỏ khi người dùng vừa mở view
        moduleField.setInvalid(false);
        moduleField.setErrorMessage(null);
        nameField.setInvalid(false);
        nameField.setErrorMessage(null);
        codeField.setInvalid(false);
        codeField.setErrorMessage(null);
    }
}
