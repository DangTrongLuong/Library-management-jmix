package com.company.librarymanagementsystem.security;

import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.RouteAccessDeniedError;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

@AnonymousAllowed
public class CustomAccessDeniedError extends RouteAccessDeniedError {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        event.forwardTo(MainView.class);

        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                Notification notification = Notification.show(
                        "Bạn không có quyền truy cập vào chức năng này!",
                        4000,
                        Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
        }

        return HttpServletResponse.SC_FORBIDDEN;
    }
}
