package com.company.librarymanagementsystem.view.login;

import com.company.librarymanagementsystem.entity.User;
import com.company.librarymanagementsystem.service.ToastService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.DataManager;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.loginform.JmixLoginI18n;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginViewSupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Route(value = "login")
@ViewController(id = "LoginView")
@ViewDescriptor(path = "login-view.xml")
public class LoginView extends StandardView implements LocaleChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("vi");

    @Autowired
    private CoreProperties coreProperties;

    @Autowired
    private LoginViewSupport loginViewSupport;

    @Autowired
    private MessageTools messageTools;

    @Autowired
    private Messages messages;

    @Autowired
    private ToastService toastService;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @ViewComponent
    private JmixLoginForm login;

    @ViewComponent
    private MessageBundle messageBundle;

    @Value("${ui.login.defaultUsername:}")
    private String defaultUsername;

    @Value("${ui.login.defaultPassword:}")
    private String defaultPassword;

    @ViewComponent
    private Button registerBtn;

    @Subscribe
    public void onInit(final InitEvent event) {
        VaadinSession.getCurrent().setLocale(DEFAULT_LOCALE);
        initLocales();
        initDefaultCredentials();
    }

    @Subscribe("registerBtn")
    public void onRegisterBtnClick(final ClickEvent<Button> event) {
        UI.getCurrent().navigate("register");
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        installLoginProgressFeedback();
    }

    private void installLoginProgressFeedback() {
        login.getElement().executeJs("""
                const loginForm = $0;
                if (loginForm.dataset.loginProgressFeedback === 'installed') return;
                loginForm.dataset.loginProgressFeedback = 'installed';

                let activeButton = null;
                let originalLabel = '';
                let loadingLabel = '';
                let submitting = false;
                let wasDisabled = false;
                let buttonObserver = null;

                const isDisabled = button => button.disabled || button.hasAttribute('disabled');

                const restoreLabel = () => {
                  const btn = activeButton || loginForm.shadowRoot?.querySelector('vaadin-button[slot="submit"]');
                  if (btn) {
                    btn.disabled = false;
                    btn.removeAttribute('disabled');
                    const locale = (loginForm.shadowRoot?.querySelector('#localesSelect')?.value || 'vi').toLowerCase();
                    btn.textContent = originalLabel || (locale.startsWith('en') ? 'Log in' : 'Đăng nhập');
                  }
                  buttonObserver?.disconnect();
                  buttonObserver = null;
                  activeButton = null;
                  submitting = false;
                  wasDisabled = false;
                };

                loginForm.resetSubmitState = restoreLabel;

                const syncLabel = () => {
                  if (!activeButton || !submitting) return;

                  if (isDisabled(activeButton)) {
                    wasDisabled = true;
                  } else if (wasDisabled) {
                    restoreLabel();
                    return;
                  }

                  if (activeButton.textContent.trim() !== loadingLabel) {
                    activeButton.textContent = loadingLabel;
                  }
                };

                loginForm.addEventListener('click', event => {
                  const path = event.composedPath();
                  if (!path.includes(loginForm) || submitting) return;

                  const button = path.find(node =>
                    node instanceof HTMLElement && node.matches('vaadin-button[slot="submit"]'));
                  if (!button || isDisabled(button)) return;

                  activeButton = button;
                  originalLabel = button.textContent.trim();
                  const locale = (loginForm.shadowRoot?.querySelector('#localesSelect')?.value || 'vi').toLowerCase();
                  loadingLabel = locale.startsWith('en') ? 'Logging in...' : 'Đang đăng nhập...';
                  submitting = true;
                  wasDisabled = false;

                  buttonObserver?.disconnect();
                  buttonObserver = new MutationObserver(syncLabel);
                  buttonObserver.observe(button, {
                    attributes: true,
                    attributeFilter: ['disabled'],
                    childList: true,
                    characterData: true,
                    subtree: true
                  });
                  syncLabel();
                }, true);
                """, login.getElement());
    }

    private void initLocales() {
        LinkedHashMap<Locale, String> locales = new LinkedHashMap<>();
        coreProperties.getAvailableLocales().forEach(locale -> {
            String displayName = switch (locale.getLanguage()) {
                case "vi" -> "Tiếng Việt";
                case "en" -> "English";
                default -> messageTools.getLocaleDisplayName(locale);
            };
            locales.put(locale, displayName);
        });

        ComponentUtils.setItemsMap(login, locales);

        login.setSelectedLocale(DEFAULT_LOCALE);
    }

    private void initDefaultCredentials() {
        if (StringUtils.isNotBlank(defaultUsername)) {
            login.setUsername(defaultUsername);
        }

        if (StringUtils.isNotBlank(defaultPassword)) {
            login.setPassword(defaultPassword);
        }
    }

    private void resetLoginSubmitButton() {
        login.getElement().executeJs("""
            if (typeof this.resetSubmitState === 'function') {
                this.resetSubmitState();
            } else {
                const btn = this.shadowRoot?.querySelector('vaadin-button[slot="submit"]');
                if (btn) {
                    btn.disabled = false;
                    btn.removeAttribute('disabled');
                    const locale = (this.shadowRoot?.querySelector('#localesSelect')?.value || 'vi').toLowerCase();
                    btn.textContent = locale.startsWith('en') ? 'Log in' : 'Đăng nhập';
                }
            }
        """);
        login.setEnabled(true);
        login.setError(false);
    }

    @Subscribe("login")
    public void onLogin(final LoginEvent event) {
        Locale selectedLocale = login.getSelectedLocale() != null
                ? login.getSelectedLocale()
                : DEFAULT_LOCALE;
        String username = event.getUsername();
        String password = event.getPassword();

        // 1. Kiểm tra tài khoản có cờ mustChangePassword không
        User user = null;
        try {
            user = systemAuthenticator.withSystem(() ->
                    dataManager.load(User.class)
                            .query("select u from User u where u.username = :username")
                            .parameter("username", username)
                            .optional()
                            .orElse(null)
            );
        } catch (Exception ex) {
            log.warn("Không thể tìm thông tin user '{}': {}", username, ex.getMessage());
        }

        if (user != null && user.isEnabled()
                && passwordEncoder.matches(password, user.getPassword())
                && Boolean.TRUE.equals(user.getMustChangePassword())) {
            // Khôi phục trạng thái nút submit trên login form
            resetLoginSubmitButton();
            openFirstTimeChangePasswordDialog(user, selectedLocale, login.isRememberMe());
            return;
        }

        // 2. Đăng nhập thông thường
        try {
            loginViewSupport.authenticate(
                    AuthDetails.of(username, password)
                            .withLocale(selectedLocale)
                            .withRememberMe(login.isRememberMe())
            );
            toastService.success(messages.getMessage(LoginView.class, "loginSuccessToast", selectedLocale));
        } catch (final BadCredentialsException | DisabledException | LockedException | AccessDeniedException e) {
            log.warn("Login failed for user '{}': {}", event.getUsername(), e.toString());
            event.getSource().setError(true);
            event.getSource().setEnabled(true);
            toastService.error(messages.getMessage(LoginView.class, "loginErrorToast", selectedLocale));
        }
    }

    private record PasswordValidationResult(boolean isValid, String errorMessage) {}

    private PasswordValidationResult validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            return new PasswordValidationResult(false, "Vui lòng nhập mật khẩu mới.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            return new PasswordValidationResult(false, "Vui lòng nhập xác nhận mật khẩu mới.");
        }
        if (!Objects.equals(newPassword, confirmPassword)) {
            return new PasswordValidationResult(false, "Mật khẩu xác nhận không khớp.");
        }
        if (newPassword.length() < 8) {
            return new PasswordValidationResult(false, "Mật khẩu phải có tối thiểu 8 ký tự.");
        }
        if (!Pattern.compile("[A-Z]").matcher(newPassword).find()) {
            return new PasswordValidationResult(false, "Mật khẩu phải chứa ít nhất 1 chữ in hoa (A-Z).");
        }
        if (!Pattern.compile("[a-z]").matcher(newPassword).find()) {
            return new PasswordValidationResult(false, "Mật khẩu phải chứa ít nhất 1 chữ in thường (a-z).");
        }
        if (!Pattern.compile("[0-9]").matcher(newPassword).find()) {
            return new PasswordValidationResult(false, "Mật khẩu phải chứa ít nhất 1 chữ số (0-9).");
        }
        if (!Pattern.compile("[^a-zA-Z0-9]").matcher(newPassword).find()) {
            return new PasswordValidationResult(false, "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (!, @, #, $, %,...).");
        }
        if (Objects.equals(newPassword, "123456a@")) {
            return new PasswordValidationResult(false, "Mật khẩu mới không được trùng với mật khẩu mặc định.");
        }
        return new PasswordValidationResult(true, null);
    }

    private void openFirstTimeChangePasswordDialog(User user, Locale locale, boolean rememberMe) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Đổi mật khẩu lần đầu");
        dialog.setWidth("480px");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        Div intro = new Div();
        intro.getStyle()
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("border-left", "4px solid var(--lumo-primary-color)")
                .set("padding", "10px 14px")
                .set("border-radius", "6px")
                .set("font-size", "0.875rem")
                .set("color", "var(--lumo-body-text-color)")
                .set("line-height", "1.4");
        intro.setText("Tài khoản '" + user.getUsername() + "' đang sử dụng mật khẩu mặc định. Vui lòng thiết lập mật khẩu mới trước khi tiếp tục.");

        PasswordField newPasswordField = new PasswordField("Mật khẩu mới *");
        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setPlaceholder("Nhập mật khẩu mới...");
        newPasswordField.setValueChangeMode(ValueChangeMode.EAGER);

        // Danh sách quy tắc mật khẩu đặt ngay dưới ô Mật khẩu mới với radio box tự tích xanh
        Div rulesContainer = new Div();
        rulesContainer.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("padding", "10px 14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "7px")
                .set("margin-top", "-4px")
                .set("margin-bottom", "4px");

        HorizontalLayout ruleLength = createRuleItem("Tối thiểu 8 ký tự", "rule-length");
        HorizontalLayout ruleUpper = createRuleItem("Ít nhất một chữ in hoa (A-Z)", "rule-upper");
        HorizontalLayout ruleLower = createRuleItem("Ít nhất một chữ in thường (a-z)", "rule-lower");
        HorizontalLayout ruleDigit = createRuleItem("Ít nhất một chữ số (0-9)", "rule-digit");
        HorizontalLayout ruleSpecial = createRuleItem("Ít nhất một ký tự đặc biệt (!, @, #, $, %,...)", "rule-special");

        rulesContainer.add(ruleLength, ruleUpper, ruleLower, ruleDigit, ruleSpecial);

        newPasswordField.addValueChangeListener(e -> {
            String val = e.getValue() != null ? e.getValue() : "";
            updateRuleStatus(ruleLength, val.length() >= 8);
            updateRuleStatus(ruleUpper, Pattern.compile("[A-Z]").matcher(val).find());
            updateRuleStatus(ruleLower, Pattern.compile("[a-z]").matcher(val).find());
            updateRuleStatus(ruleDigit, Pattern.compile("[0-9]").matcher(val).find());
            updateRuleStatus(ruleSpecial, Pattern.compile("[^a-zA-Z0-9]").matcher(val).find());
        });

        PasswordField confirmPasswordField = new PasswordField("Xác nhận mật khẩu mới *");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setPlaceholder("Nhập lại mật khẩu mới...");

        Span errorSpan = new Span();
        errorSpan.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("font-size", "0.85rem")
                .set("font-weight", "500");
        errorSpan.setVisible(false);

        layout.add(intro, newPasswordField, rulesContainer, confirmPasswordField, errorSpan);
        dialog.add(layout);

        Button saveAndLoginBtn = new Button("Đổi mật khẩu & Vào hệ thống");
        saveAndLoginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Hủy bỏ");
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        cancelBtn.addClickListener(e -> {
            resetLoginSubmitButton();
            dialog.close();
        });

        newPasswordField.addKeyDownListener(Key.ENTER, e -> confirmPasswordField.focus());
        confirmPasswordField.addKeyDownListener(Key.ENTER, e -> saveAndLoginBtn.click());

        // Lắng nghe phím gõ phía client để tự động tích xanh ngay lập tức 0ms
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                resetLoginSubmitButton();
            } else {
                newPasswordField.focus();
                newPasswordField.getElement().executeJs("""
                    const input = this.shadowRoot ? this.shadowRoot.querySelector('input') : this.querySelector('input');
                    if (input && !input.dataset.ruleListener) {
                        input.dataset.ruleListener = 'true';
                        const update = (id, ok) => {
                            const el = document.getElementById(id);
                            if (!el) return;
                            const radio = el.children[0];
                            const label = el.children[1];
                            if (ok) {
                                radio.textContent = '✓';
                                radio.style.borderColor = '#16a34a';
                                radio.style.backgroundColor = '#16a34a';
                                el.style.color = '#15803d';
                                if (label) label.style.fontWeight = '500';
                            } else {
                                radio.textContent = '';
                                radio.style.borderColor = '#cbd5e1';
                                radio.style.backgroundColor = 'transparent';
                                el.style.color = 'var(--lumo-secondary-text-color)';
                                if (label) label.style.fontWeight = 'normal';
                            }
                        };
                        input.addEventListener('input', () => {
                            const v = input.value || '';
                            update('rule-length', v.length >= 8);
                            update('rule-upper', /[A-Z]/.test(v));
                            update('rule-lower', /[a-z]/.test(v));
                            update('rule-digit', /[0-9]/.test(v));
                            update('rule-special', /[^a-zA-Z0-9]/.test(v));
                        });
                    }
                """);
            }
        });

        saveAndLoginBtn.addClickListener(e -> {
            String newPass = newPasswordField.getValue();
            String confirmPass = confirmPasswordField.getValue();

            PasswordValidationResult result = validateNewPassword(newPass, confirmPass);
            if (!result.isValid()) {
                errorSpan.setText(result.errorMessage());
                errorSpan.setVisible(true);
                return;
            }

            try {
                // Cập nhật mật khẩu mới và hủy cờ mustChangePassword
                systemAuthenticator.runWithSystem(() -> {
                    User u = dataManager.load(User.class).id(user.getId()).one();
                    u.setPassword(passwordEncoder.encode(newPass));
                    u.setMustChangePassword(false);
                    u.setLastLoginAt(OffsetDateTime.now());
                    dataManager.save(u);
                });

                dialog.close();
                toastService.success("Đổi mật khẩu thành công! Đang đăng nhập...");

                // Đăng nhập tự động ngay với mật khẩu mới, không cần nhập lại
                loginViewSupport.authenticate(
                        AuthDetails.of(user.getUsername(), newPass)
                                .withLocale(locale)
                                .withRememberMe(rememberMe)
                );
            } catch (Exception ex) {
                log.error("Lỗi khi cập nhật mật khẩu lần đầu cho user '{}': {}", user.getUsername(), ex.getMessage(), ex);
                errorSpan.setText("Đã xảy ra lỗi khi đăng nhập: " + ex.getMessage());
                errorSpan.setVisible(true);
                resetLoginSubmitButton();
            }
        });

        dialog.getFooter().add(cancelBtn, saveAndLoginBtn);
        dialog.open();
    }

    private HorizontalLayout createRuleItem(String text, String elementId) {
        HorizontalLayout item = new HorizontalLayout();
        item.setId(elementId);
        item.setAlignItems(FlexComponent.Alignment.CENTER);
        item.setSpacing(true);
        item.getStyle()
                .set("font-size", "0.8125rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("transition", "all 0.15s ease")
                .set("gap", "8px");

        Span radio = new Span();
        radio.getStyle()
                .set("width", "16px")
                .set("height", "16px")
                .set("border-radius", "50%")
                .set("border", "2px solid #cbd5e1")
                .set("box-sizing", "border-box")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "10px")
                .set("line-height", "1")
                .set("font-weight", "bold")
                .set("color", "white")
                .set("background-color", "transparent")
                .set("transition", "all 0.15s ease")
                .set("flex-shrink", "0");

        Span label = new Span(text);
        label.getStyle().set("transition", "all 0.15s ease");

        item.add(radio, label);
        return item;
    }

    private void updateRuleStatus(HorizontalLayout item, boolean passed) {
        Span radio = (Span) item.getComponentAt(0);
        Span label = (Span) item.getComponentAt(1);

        if (passed) {
            radio.setText("✓");
            radio.getStyle()
                    .set("border-color", "#16a34a")
                    .set("background-color", "#16a34a");
            item.getStyle().set("color", "#15803d");
            label.getStyle().set("font-weight", "500");
        } else {
            radio.setText("");
            radio.getStyle()
                    .set("border-color", "#cbd5e1")
                    .set("background-color", "transparent");
            item.getStyle().set("color", "var(--lumo-secondary-text-color)");
            label.getStyle().set("font-weight", "normal");
        }
    }

    @Override
    public void localeChange(final LocaleChangeEvent event) {
        UI.getCurrent().getPage().setTitle(messageBundle.getMessage("LoginView.title"));

        final JmixLoginI18n loginI18n = JmixLoginI18n.createDefault();

        final JmixLoginI18n.JmixForm form = new JmixLoginI18n.JmixForm();
        form.setTitle(messageBundle.getMessage("loginForm.headerTitle"));
        form.setUsername(messageBundle.getMessage("loginForm.username"));
        form.setPassword(messageBundle.getMessage("loginForm.password"));
        form.setSubmit(messageBundle.getMessage("loginForm.submit"));
        form.setForgotPassword(messageBundle.getMessage("loginForm.forgotPassword"));
        form.setRememberMe(messageBundle.getMessage("loginForm.rememberMe"));
        loginI18n.setForm(form);

        final LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle(messageBundle.getMessage("loginForm.errorTitle"));
        errorMessage.setMessage(messageBundle.getMessage("loginForm.badCredentials"));
        errorMessage.setUsername(messageBundle.getMessage("loginForm.errorUsername"));
        errorMessage.setPassword(messageBundle.getMessage("loginForm.errorPassword"));
        loginI18n.setErrorMessage(errorMessage);

        login.setI18n(loginI18n);
    }
}
