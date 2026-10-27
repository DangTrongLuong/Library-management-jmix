package com.company.librarymanagementsystem.view.member;

import com.company.librarymanagementsystem.entity.Member;
import com.company.librarymanagementsystem.entity.MemberStatus;
import com.company.librarymanagementsystem.security.AccessControlService;
import com.company.librarymanagementsystem.security.Permissions;
import com.company.librarymanagementsystem.service.MemberService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.grid.DataGrid;
import com.vaadin.flow.component.textfield.TextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@Route(value = "members", layout = MainView.class)
@ViewController(id = "Member.list")
@ViewDescriptor(path = "member-list-view.xml")
@LookupComponent("membersDataGrid")
@DialogMode(width = "80em")
public class MemberListView extends StandardListView<Member> {

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Select<String> statusFilterSelect;

    @ViewComponent
    private CollectionLoader<Member> membersDl;

    @ViewComponent
    private DataGrid<Member> membersDataGrid;

    @ViewComponent
    private Button createButton;

    @ViewComponent
    private Button editButton;

    @ViewComponent
    private Button removeButton;

    @ViewComponent
    private Button renewCardBtn;

    @ViewComponent
    private Button suspendBtn;

    @ViewComponent
    private Button activateBtn;

    @ViewComponent
    private Button checkEligibilityBtn;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ToastService toastService;

    @Autowired
    private Dialogs dialogs;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private AccessControlService accessControlService;

    @Subscribe
    public void onInit(final InitEvent event) {
        statusFilterSelect.setItems(Arrays.asList("", "ACTIVE", "SUSPENDED", "BLOCKED", "EXPIRED", "PENDING"));
        statusFilterSelect.setItemLabelGenerator(s -> switch (s) {
            case "ACTIVE" -> "Đang hoạt động";
            case "SUSPENDED" -> "Tạm khóa";
            case "BLOCKED" -> "Bị khóa vĩnh viễn";
            case "EXPIRED" -> "Hết hạn";
            case "PENDING" -> "Chờ duyệt";
            default -> "Tất cả trạng thái";
        });
        statusFilterSelect.setValue("");
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // Áp dụng phân quyền chi tiết (Fine-grained Permissions)
        accessControlService.applyPermission(createButton, Permissions.MEMBER_CREATE);
        accessControlService.applyPermission(membersDataGrid.getAction("createAction"), Permissions.MEMBER_CREATE);

        accessControlService.applyPermission(editButton, Permissions.MEMBER_EDIT);
        accessControlService.applyPermission(membersDataGrid.getAction("editAction"), Permissions.MEMBER_EDIT);

        // Nút xóa: chỉ Admin mới được phép hiển thị và thao tác
        boolean isAdmin = accessControlService.isAdmin();
        if (removeButton != null) {
            removeButton.setVisible(isAdmin);
        }
        Action removeAction = membersDataGrid.getAction("removeAction");
        if (removeAction != null) {
            removeAction.setVisible(isAdmin);
            removeAction.setEnabled(isAdmin);
        }

        accessControlService.applyPermission(renewCardBtn, Permissions.MEMBER_CARD_RENEW);
        accessControlService.applyPermission(suspendBtn, Permissions.MEMBER_LOCK);
        accessControlService.applyPermission(activateBtn, Permissions.MEMBER_LOCK);
        accessControlService.applyPermission(checkEligibilityBtn, Permissions.MEMBER_VIEW);

        loadData();
    }

    private void loadData() {
        String statusFilter = statusFilterSelect.getValue();
        String searchText = searchField.getValue();
        membersDl.setParameter("statusFilter",
                (statusFilter == null || statusFilter.isBlank()) ? null : statusFilter);
        membersDl.setParameter("searchText",
                (searchText == null || searchText.isBlank()) ? null : searchText.trim());
        membersDl.load();
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(final ClickEvent<Button> event) {
        membersDl.setFirstResult(0);
        loadData();
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(final ClickEvent<Button> event) {
        searchField.clear();
        statusFilterSelect.setValue("");
        membersDl.setFirstResult(0);
        loadData();
    }

    @Subscribe("membersDataGrid.refreshAction")
    public void onRefreshAction(final ActionPerformedEvent event) {
        loadData();
    }

    @Install(to = "membersDataGrid.removeAction", subject = "afterActionPerformedHandler")
    public void membersDataGridRemoveAfterActionPerformed(final RemoveOperation.AfterActionPerformedEvent<Member> event) {
        toastService.success("Xóa độc giả thành công!");
    }

    @Subscribe("renewCardBtn")
    public void onRenewCardBtnClick(final ClickEvent<Button> event) {
        Member selected = membersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn một độc giả trước khi gia hạn thẻ!");
            return;
        }
        String memberName = selected.getFullName() != null ? selected.getFullName() : "";
        dialogs.createOptionDialog()
                .withHeader("Xác nhận gia hạn thẻ")
                .withText(String.format("Gia hạn thẻ cho độc giả [%s] %s?",
                        selected.getMemberCode(), memberName))
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withText("Gia hạn")
                                .withHandler(e -> {
                                     try {
                                         memberService.renewCard(selected);
                                         toastService.success("Gia hạn thẻ thành công!");
                                         loadData();
                                     } catch (Exception ex) {
                                         toastService.error("Lỗi khi gia hạn thẻ: " + ex.getMessage());
                                     }
                                 }),
                        new DialogAction(DialogAction.Type.NO).withText("Hủy"))
                .open();
    }

    @Subscribe("suspendBtn")
    public void onSuspendBtnClick(final ClickEvent<Button> event) {
        Member selected = membersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn một độc giả!");
            return;
        }
        if (!MemberStatus.ACTIVE.getId().equalsIgnoreCase(selected.getStatus())
                && !MemberStatus.PENDING.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Chỉ có thể tạm khóa thẻ đang HOẠT ĐỘNG hoặc CHỜ DUYỆT!");
            return;
        }
        dialogs.createInputDialog(this)
                .withHeader("Tạm khóa thẻ độc giả")
                .withParameters(
                        InputParameter.stringParameter("reason")
                                .withLabel("Lý do tạm khóa")
                                .withRequired(true))
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        String reason = closeEvent.getValue("reason");
                        try {
                            memberService.suspendMember(selected, reason);
                            toastService.success("Đã tạm khóa thẻ độc giả!");
                            loadData();
                        } catch (Exception ex) {
                            toastService.error("Lỗi: " + ex.getMessage());
                        }
                    }
                })
                .open();
    }

    @Subscribe("activateBtn")
    public void onActivateBtnClick(final ClickEvent<Button> event) {
        Member selected = membersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn một độc giả!");
            return;
        }
        if (MemberStatus.ACTIVE.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Thẻ này đang ở trạng thái HOẠT ĐỘNG rồi!");
            return;
        }
        String memberName = selected.getFullName() != null ? selected.getFullName() : "";
        boolean isPending = MemberStatus.PENDING.getId().equalsIgnoreCase(selected.getStatus());
        String header = isPending ? "Xác nhận duyệt & cấp thẻ thư viện" : "Xác nhận mở khóa thẻ";
        String typeName = selected.getMemberType() != null ? selected.getMemberType().getName() : "Độc giả";
        String prompt = isPending
                ? String.format("Xác nhận đã đối chiếu giấy tờ và đồng ý duyệt cấp thẻ chính thức cho độc giả [%s] %s (Đối tượng: %s)?",
                selected.getMemberCode(), memberName, typeName)
                : String.format("Mở khóa thẻ cho độc giả [%s] %s?",
                selected.getMemberCode(), memberName);
        String actionText = isPending ? "Duyệt & Cấp thẻ" : "Mở khóa";

        dialogs.createOptionDialog()
                .withHeader(header)
                .withText(prompt)
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withText(actionText)
                                .withHandler(e -> {
                                     try {
                                         memberService.activateMember(selected);
                                         toastService.success(isPending
                                                 ? "Đã duyệt và cấp thẻ chính thức thành công cho độc giả!"
                                                 : "Đã mở khóa thẻ độc giả!");
                                         loadData();
                                     } catch (Exception ex) {
                                         toastService.error("Lỗi: " + ex.getMessage());
                                     }
                                 }),
                        new DialogAction(DialogAction.Type.NO).withText("Hủy"))
                .open();
    }

    @Subscribe("checkEligibilityBtn")
    public void onCheckEligibilityBtnClick(final ClickEvent<Button> event) {
        Member selected = membersDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn một độc giả để kiểm tra!");
            return;
        }

        Member fullMember = dataManager.load(Member.class)
                .id(selected.getId())
                .fetchPlan(fpBuilder -> fpBuilder.addAll(
                        "memberCode", "fullName", "status", "statusReason",
                        "cardIssuedDate", "cardExpiryDate",
                        "memberType.maxLoanBooks", "memberType.name"))
                .one();

        MemberService.EligibilityResult result = memberService.checkEligibility(fullMember);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kết quả kiểm tra tư cách mượn");
        dialog.setWidth("25em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.getStyle().set("box-sizing", "border-box").set("overflow-x", "hidden");

        // Dòng 1: Độc giả
        Div row1 = new Div();
        Span label1 = new Span("Độc giả: ");
        label1.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span val1 = new Span(result.getMemberName() != null ? result.getMemberName() : "-");
        val1.getStyle().set("font-weight", "600");
        row1.add(label1, val1);

        // Dòng 2: Mã thẻ
        Div row2 = new Div();
        Span label2 = new Span("Mã thẻ: ");
        label2.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span val2 = new Span(result.getMemberCode() != null ? result.getMemberCode() : "-");
        val2.getStyle().set("font-family", "monospace").set("font-weight", "600");
        row2.add(label2, val2);

        // Dòng 3: Sách đang mượn
        Div row3 = new Div();
        Span label3 = new Span("Sách đang mượn: ");
        label3.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span val3 = new Span(String.format("%d / %d cuốn", result.getCurrentLoans(), result.getMaxLoanBooks()));
        val3.getStyle().set("font-weight", "600");
        row3.add(label3, val3);

        // Dòng 4: Trạng thái (hộp màu tùy theo trạng thái)
        Div statusBox = new Div();
        statusBox.getStyle()
                .set("box-sizing", "border-box")
                .set("width", "100%")
                .set("padding", "8px 12px")
                .set("border-radius", "6px")
                .set("margin-top", "6px")
                .set("font-weight", "600")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px");

        if (result.isEligible()) {
            statusBox.getStyle()
                    .set("background-color", "#e8f5e9") // Xanh lá nhẹ
                    .set("border", "1px solid #a5d6a7")
                    .set("color", "#2e7d32"); // Chữ xanh lá đậm
            Span title = new Span("ĐỦ ĐIỀU KIỆN MƯỢN SÁCH");
            title.getStyle().set("font-size", "var(--lumo-font-size-m)");
            statusBox.add(title);
            toastService.success("Độc giả đủ điều kiện mượn sách!");
        } else {
            String status = result.getStatus();
            boolean isWarning = "SUSPENDED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status);
            if (isWarning) {
                statusBox.getStyle()
                        .set("background-color", "#fff8e1") // Vàng cam nhẹ
                        .set("border", "1px solid #ffe082")
                        .set("color", "#b78103");
            } else {
                statusBox.getStyle()
                        .set("background-color", "#ffebee") // Đỏ hồng nhẹ
                        .set("border", "1px solid #ffcdd2")
                        .set("color", "#c62828");
            }

            Span title = new Span("KHÔNG ĐỦ ĐIỀU KIỆN MƯỢN SÁCH");
            title.getStyle().set("font-size", "var(--lumo-font-size-m)");
            statusBox.add(title);

            if (result.getBlockingReasons() != null && !result.getBlockingReasons().isEmpty()) {
                Div reasonsList = new Div();
                reasonsList.getStyle().set("font-weight", "normal").set("font-size", "var(--lumo-font-size-s)");
                for (String r : result.getBlockingReasons()) {
                    Div item = new Div();
                    item.setText("• " + r);
                    reasonsList.add(item);
                }
                statusBox.add(reasonsList);
            }
            toastService.warning("Độc giả không đủ điều kiện mượn sách!");
        }

        layout.add(row1, row2, row3, statusBox);
        dialog.add(layout);

        Button closeBtn = new Button("Đồng ý", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);

        dialog.open();
    }

    @Supply(to = "membersDataGrid.fullNameCol", subject = "renderer")
    public Renderer<Member> fullNameColRenderer() {
        return new ComponentRenderer<>(member -> {
            String name = member.getFullName();
            if (name == null || name.isBlank()) {
                if (member.getUser() != null) {
                    name = member.getUser().getUsername();
                }
            }
            return new Span(name != null && !name.isBlank() ? name : "-");
        });
    }

    @Supply(to = "membersDataGrid.usernameCol", subject = "renderer")
    public Renderer<Member> usernameColRenderer() {
        return new ComponentRenderer<>(member -> {
            if (member.getUser() == null) return new Span("-");
            Span sp = new Span(member.getUser().getUsername() != null ? member.getUser().getUsername() : "-");
            sp.getElement().getThemeList().add("badge contrast small");
            sp.getStyle().set("font-family", "monospace");
            return sp;
        });
    }

    @Supply(to = "membersDataGrid.memberTypeCol", subject = "renderer")
    public Renderer<Member> memberTypeColRenderer() {
        return new ComponentRenderer<>(member -> {
            if (member.getMemberType() == null) return new Span("-");
            Span sp = new Span(member.getMemberType().getName() != null ? member.getMemberType().getName() : "-");
            sp.getElement().getThemeList().add("badge small");
            return sp;
        });
    }

    @Supply(to = "membersDataGrid.statusCol", subject = "renderer")
    public Renderer<Member> statusColRenderer() {
        return new ComponentRenderer<>(member -> {
            MemberStatus st = member.getStatusEnum();
            boolean expired = member.isExpired();

            String label;
            String theme;
            if (expired && (st == MemberStatus.ACTIVE || st == MemberStatus.EXPIRED)) {
                label = "Hết hạn";
                theme = "badge error";
            } else if (st == null) {
                label = member.getStatus() != null ? member.getStatus() : "-";
                theme = "badge contrast";
            } else {
                label = switch (st) {
                    case ACTIVE -> "Hoạt động";
                    case SUSPENDED -> "Tạm khóa";
                    case BLOCKED -> "Bị khóa";
                    case EXPIRED -> "Hết hạn";
                    case PENDING -> "Chờ duyệt";
                };
                theme = switch (st) {
                    case ACTIVE -> "badge success";
                    case SUSPENDED -> "badge warning";
                    case BLOCKED, EXPIRED -> "badge error";
                    case PENDING -> "badge warning primary";
                };
            }
            Span sp = new Span(label);
            sp.getElement().getThemeList().add(theme);
            return sp;
        });
    }
}