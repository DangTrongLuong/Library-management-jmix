package com.company.librarymanagementsystem.view.register;

import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.service.ReaderRegistrationService;
import com.company.librarymanagementsystem.service.ReaderRegistrationService.RegistrationRequest;
import com.company.librarymanagementsystem.service.ReaderRegistrationService.RegistrationResult;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.login.LoginView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "register")
@ViewController("RegisterView")
@ViewDescriptor("register-view.xml")
@AnonymousAllowed
public class RegisterView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(RegisterView.class);

    @ViewComponent
    private TextField usernameField;
    @ViewComponent
    private TextField fullNameField;
    @ViewComponent
    private TextField emailField;
    @ViewComponent
    private TextField phoneField;
    @ViewComponent
    private PasswordField passwordField;
    @ViewComponent
    private PasswordField confirmPasswordField;
    @ViewComponent
    private Select<MemberType> memberTypeField;

    @ViewComponent
    private Div messageBanner;
    @ViewComponent
    private Button submitRegisterBtn;
    @ViewComponent
    private Button loginLinkBtn;

    @Autowired
    private ReaderRegistrationService registrationService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private DataManager dataManager;

    @Subscribe
    public void onInit(final InitEvent event) {
        submitRegisterBtn.addClickListener(this::onSubmitRegister);
        loginLinkBtn.addClickListener(e -> navigateToLogin());

        // Hỗ trợ nhấn Enter trên các ô nhập để kích hoạt đăng ký
        usernameField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));
        fullNameField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));
        emailField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));
        phoneField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));
        passwordField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));
        confirmPasswordField.addKeyPressListener(Key.ENTER, e -> onSubmitRegister(null));

        // Tải danh sách loại đối tượng độc giả (Sinh viên, Giảng viên, Cán bộ...) với
        // System rights
        List<MemberType> types = systemAuthenticator.withSystem(() -> dataManager.load(MemberType.class)
                .query("select mt from MemberType mt where mt.deletedAt is null order by mt.id asc")
                .list());

        memberTypeField.setItems(types);
        memberTypeField.setItemLabelGenerator(t -> t != null ? t.getName() : "");

        // Mặc định chọn loại Sinh viên nếu có, hoặc đối tượng đầu tiên
        types.stream()
                .filter(t -> "STUDENT".equalsIgnoreCase(t.getCode()))
                .findFirst()
                .ifPresentOrElse(memberTypeField::setValue, () -> {
                    if (!types.isEmpty()) {
                        memberTypeField.setValue(types.get(0));
                    }
                });
    }

    private void onSubmitRegister(ClickEvent<Button> event) {
        clearMessage();

        String username = usernameField.getValue();
        String fullName = fullNameField.getValue();
        String email = emailField.getValue();
        String phone = phoneField.getValue();
        String password = passwordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();
        MemberType selectedType = memberTypeField.getValue();

        if (selectedType == null) {
            showErrorMessage("Vui lòng chọn loại đối tượng độc giả (Sinh viên, Giảng viên, Cán bộ...)!");
            return;
        }

        RegistrationRequest request = new RegistrationRequest(
                username, fullName, email, phone, password, confirmPassword, selectedType.getId());

        // Kiểm tra nhanh tính hợp lệ trước khi gửi
        try {
            registrationService.validate(request);
        } catch (IllegalArgumentException ex) {
            showErrorMessage(ex.getMessage());
            return;
        }

        submitRegisterBtn.setEnabled(false);
        submitRegisterBtn.setText("Đang đăng ký...");

        try {
            RegistrationResult result = registrationService.registerReader(request);

            if (result.success()) {
                showSuccessDialog(result);
            } else {
                showErrorMessage(result.message());
                submitRegisterBtn.setEnabled(true);
                submitRegisterBtn.setText("Đăng ký tài khoản");
            }
        } catch (Exception ex) {
            log.error("Lỗi khi đăng ký tài khoản bạn đọc: {}", ex.getMessage(), ex);
            showErrorMessage("Đã xảy ra lỗi hệ thống khi đăng ký: " + ex.getMessage());
            submitRegisterBtn.setEnabled(true);
            submitRegisterBtn.setText("Đăng ký tài khoản");
        }
    }

    private void showSuccessDialog(RegistrationResult result) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Đăng Ký Thành Công!");
        dialog.setWidth("480px");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.CENTER);

        Div iconWrapper = new Div();
        iconWrapper.addClassNames("p-m", "rounded-full", "bg-success-10", "text-success", "flex", "items-center",
                "justify-center");
        iconWrapper.getStyle().set("width", "64px").set("height", "64px").set("font-size", "32px");
        iconWrapper.add(VaadinIcon.CHECK_CIRCLE.create());

        H3 title = new H3("Chào mừng bạn đọc mới!");
        title.getStyle().set("margin", "0 0 4px 0").set("color", "var(--lumo-primary-text-color)");

        Span intro = new Span("Tài khoản của bạn đã được khởi tạo thành công với vai trò Độc giả.");
        intro.addClassNames("text-secondary", "text-center", "text-s");

        // Thẻ thông tin tài khoản vừa tạo
        Div infoCard = new Div();
        infoCard.addClassNames("p-m", "bg-contrast-5", "rounded-m", "border", "border-contrast-10", "flex", "flex-col",
                "gap-xs", "w-full");

        Div rowUser = createInfoRow("Tên đăng nhập:", result.username());
        Div rowType = createInfoRow("Đối tượng độc giả:",
                result.memberTypeName() != null ? result.memberTypeName() : "Sinh viên");
        Div rowCard = createInfoRow("Mã hồ sơ thẻ:", result.memberCode());

        // Dòng trạng thái nổi bật Chờ cấp thẻ
        Div rowStatus = new Div();
        rowStatus.addClassNames("flex", "justify-between", "items-center", "text-s");
        Span lblStatus = new Span("Trạng thái thẻ:");
        lblStatus.addClassNames("text-secondary");
        Span valStatus = new Span("CHỜ CẤP THẺ TẠI QUẦY");
        valStatus.addClassNames("font-bold", "text-warning");
        rowStatus.add(lblStatus, valStatus);

        infoCard.add(rowUser, rowType, rowCard, rowStatus);

        Div advisoryCard = new Div();
        advisoryCard.addClassNames("p-s", "rounded-m", "border", "border-warning-50", "bg-warning-10", "text-warning",
                "flex", "items-start", "gap-xs", "w-full", "text-xs");
        Span alertIcon = new Span(VaadinIcon.EXCLAMATION_CIRCLE.create());
        alertIcon.getStyle().set("flex-shrink", "0").set("margin-top", "2px");
        Span alertText = new Span(
                "Thẻ thư viện cần được Thủ thư kiểm tra đối chiếu giấy tờ (thẻ sinh viên/thẻ cán bộ/giấy tờ liên quan) tại quầy thư viện để chính thức cấp thẻ và kích hoạt quyền mượn sách.");
        advisoryCard.add(alertIcon, alertText);

        content.add(iconWrapper, title, intro, infoCard, advisoryCard);
        dialog.add(content);

        Button loginBtn = new Button("Đến trang Đăng nhập ngay", VaadinIcon.SIGN_IN.create());
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.setWidthFull();
        loginBtn.addClickListener(e -> {
            dialog.close();
            navigateToLogin();
        });

        dialog.getFooter().add(loginBtn);
        dialog.open();

        toastService.success("Chúc mừng! Đăng ký tài khoản Bạn đọc thành công.");
    }

    private Div createInfoRow(String label, String value) {
        Div row = new Div();
        row.addClassNames("flex", "justify-between", "items-center", "text-s");
        Span lbl = new Span(label);
        lbl.addClassNames("text-secondary");
        Span val = new Span(value);
        val.addClassNames("font-bold", "text-primary");
        row.add(lbl, val);
        return row;
    }

    private void showErrorMessage(String message) {
        messageBanner.removeAll();
        messageBanner.removeClassName("hidden");
        messageBanner.setClassName(
                "p-s rounded-m text-s mt-s border border-error-50 bg-error-10 text-error flex items-center gap-s");

        Span icon = new Span();
        icon.add(VaadinIcon.EXCLAMATION_CIRCLE.create());
        Span text = new Span(message);

        messageBanner.add(icon, text);
        toastService.error(message);
    }

    private void clearMessage() {
        messageBanner.removeAll();
        messageBanner.addClassName("hidden");
    }

    private void navigateToLogin() {
        UI.getCurrent().navigate(LoginView.class);
    }
}
