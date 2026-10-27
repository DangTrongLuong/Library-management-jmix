package com.company.librarymanagementsystem.service;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.flowui.Notifications;
import org.springframework.stereotype.Component;

@Component
public class ToastService {

    private static final int DURATION_MS = 4000;

    private final Notifications notifications;

    public ToastService(Notifications notifications) {
        this.notifications = notifications;
    }

    public void success(String message) {
        show(message, Notifications.Type.SUCCESS, VaadinIcon.CHECK_CIRCLE, "toast-icon-success");
    }

    public void error(String message) {
        show(message, Notifications.Type.ERROR, VaadinIcon.EXCLAMATION_CIRCLE, "toast-icon-error");
    }

    public void warning(String message) {
        show(message, Notifications.Type.WARNING, VaadinIcon.WARNING, "toast-icon-warning");
    }

    public void info(String message) {
        show(message, Notifications.Type.DEFAULT, VaadinIcon.INFO_CIRCLE, "toast-icon-info");
    }

    private void show(String message, Notifications.Type type, VaadinIcon vaadinIcon, String iconClass) {
        if (message == null || message.isBlank()) {
            return;
        }

        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);
        layout.addClassNames("toast-layout");

        Icon icon = vaadinIcon.create();
        icon.addClassNames("toast-icon", iconClass);

        Span textSpan = new Span(message);
        textSpan.addClassNames("toast-message");

        layout.add(icon, textSpan);

        notifications.create(layout)
                .withType(type)
                .withPosition(Notification.Position.TOP_END)
                .withDuration(DURATION_MS)
                .withCloseable(false)
                .show();
    }
}
