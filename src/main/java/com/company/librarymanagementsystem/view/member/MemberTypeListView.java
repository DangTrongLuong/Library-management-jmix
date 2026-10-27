package com.company.librarymanagementsystem.view.member;

import com.company.librarymanagementsystem.entity.MemberType;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

import java.text.DecimalFormat;

@Route(value = "member-types", layout = MainView.class)
@ViewController(id = "MemberType.list")
@ViewDescriptor(path = "member-type-list-view.xml")
@LookupComponent("memberTypesDataGrid")
@DialogMode(width = "64em")
public class MemberTypeListView extends StandardListView<MemberType> {

    @ViewComponent
    private io.jmix.flowui.component.grid.DataGrid<MemberType> memberTypesDataGrid;

    @ViewComponent
    private com.vaadin.flow.component.button.Button createButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button editButton;

    @ViewComponent
    private com.vaadin.flow.component.button.Button removeButton;

    @org.springframework.beans.factory.annotation.Autowired
    private com.company.librarymanagementsystem.security.AccessControlService accessControlService;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    @Subscribe
    public void onReady(final ReadyEvent event) {
        accessControlService.applyPermission(createButton, com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);
        accessControlService.applyPermission(memberTypesDataGrid.getAction("createAction"), com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);

        accessControlService.applyPermission(editButton, com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);
        accessControlService.applyPermission(memberTypesDataGrid.getAction("editAction"), com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);

        accessControlService.applyPermission(removeButton, com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);
        accessControlService.applyPermission(memberTypesDataGrid.getAction("removeAction"), com.company.librarymanagementsystem.security.Permissions.MEMBER_TYPE_MANAGE);
    }

    @Supply(to = "memberTypesDataGrid.finePerDay", subject = "renderer")
    public Renderer<MemberType> finePerDayRenderer() {
        return new ComponentRenderer<>(type -> {
            if (type.getFinePerDay() != null) {
                return new Span(currencyFormat.format(type.getFinePerDay()) + " đ");
            }
            return new Span("0 đ");
        });
    }

    @Supply(to = "memberTypesDataGrid.code", subject = "renderer")
    public Renderer<MemberType> codeRenderer() {
        return new ComponentRenderer<>(type -> {
            Span span = new Span(type.getCode() != null ? type.getCode() : "-");
            span.getElement().getThemeList().add("badge");
            span.getStyle().set("font-family", "monospace").set("font-weight", "bold");
            return span;
        });
    }
}
