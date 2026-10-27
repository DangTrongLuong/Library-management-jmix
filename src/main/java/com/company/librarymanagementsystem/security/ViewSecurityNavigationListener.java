package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.view.login.LoginView;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.jmix.flowui.sys.UiAccessChecker;
import org.springframework.stereotype.Component;

/**
 * Listener kiểm soát toàn bộ điều hướng qua URL trong ứng dụng.
 * Nếu tài khoản hiện tại không có quyền xem View tương ứng (thông qua Jmix ViewPolicy / Security),
 * hệ thống sẽ chặn điều hướng, lập tức chuyển về MainView và hiển thị thông báo lỗi.
 */
@Component
public class ViewSecurityNavigationListener implements VaadinServiceInitListener {

    private final UiAccessChecker uiAccessChecker;

    public ViewSecurityNavigationListener(UiAccessChecker uiAccessChecker) {
        this.uiAccessChecker = uiAccessChecker;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(this::checkViewAccess);
        });
    }

    private void checkViewAccess(BeforeEnterEvent event) {
        Class<?> target = event.getNavigationTarget();
        if (target == null || target.equals(MainView.class) || target.equals(LoginView.class)
                || target.isAnnotationPresent(com.vaadin.flow.server.auth.AnonymousAllowed.class)) {
            return;
        }

        // Kiểm tra quyền truy cập theo cơ chế Security của Jmix
        if (!uiAccessChecker.isViewPermitted(target)) {
            event.forwardTo(MainView.class);

            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.access(() -> {
                    Notification notification = Notification.show(
                            "Bạn không có quyền truy cập vào chức năng này!",
                            4000,
                            Notification.Position.TOP_END
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        }
    }
}
