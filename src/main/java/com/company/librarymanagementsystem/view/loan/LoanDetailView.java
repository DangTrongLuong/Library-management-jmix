package com.company.librarymanagementsystem.view.loan;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.LoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Route(value = "loans/:id", layout = MainView.class)
@ViewController("Loan.detail")
@ViewDescriptor("loan-detail-view.xml")
@EditedEntityContainer("loanDc")
public class LoanDetailView extends StandardDetailView<Loan> {

    @ViewComponent
    private VerticalLayout mainContainer;
    @ViewComponent
    private H2 headerTitle;
    @ViewComponent
    private Button saveCloseBtn;
    @ViewComponent
    private Button closeBtn;
    @ViewComponent
    private TextField memberNameField;
    @ViewComponent
    private TextField memberTypeField;
    @ViewComponent
    private TextField issuedByField;
    @ViewComponent
    private TextField loanDateField;
    @ViewComponent
    private TextField totalBooksField;

    @ViewComponent
    private DataGrid<LoanItem> loanItemsDataGrid;
    @ViewComponent
    private CollectionLoader<LoanItem> loanItemsDl;

    @ViewComponent
    private Button detailReturnBtn;
    @ViewComponent
    private Button detailRenewBtn;

    @Autowired
    private LoanService loanService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private ViewNavigators viewNavigators;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Subscribe
    public void onInit(InitEvent event) {
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        getContent().setWidthFull();
        if (mainContainer != null) {
            mainContainer.getStyle()
                    .set("margin-left", "auto")
                    .set("margin-right", "auto")
                    .set("align-self", "center");
        }
        initRenderers();
        initActions();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        Loan loan = getEditedEntity();
        if (loan != null) {
            headerTitle.setText("Chi tiết Phiếu Mượn: " + (loan.getLoanCode() != null ? loan.getLoanCode() : ""));

            Member m = loan.getMember();
            if (m != null) {
                memberNameField.setValue(String.format("%s (%s)",
                        m.getFullName() != null ? m.getFullName() : "",
                        m.getMemberCode() != null ? m.getMemberCode() : ""));
                if (m.getMemberType() != null) {
                    memberTypeField.setValue(m.getMemberType().getName() != null ? m.getMemberType().getName() : "");
                }
            }

            if (loan.getIssuedBy() != null) {
                issuedByField
                        .setValue(loan.getIssuedBy().getUsername() != null ? loan.getIssuedBy().getUsername() : "");
            }

            if (loan.getLoanDate() != null) {
                loanDateField.setValue(loan.getLoanDate().format(DTF));
            }

            int count = loan.getLoanItems() != null ? loan.getLoanItems().size() : 0;
            totalBooksField.setValue(count + " cuốn");
        }
    }

    private Div createCenteredCell(com.vaadin.flow.component.Component component) {
        Div div = new Div(component);
        div.setWidthFull();
        div.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("text-align", "center");
        return div;
    }

    private void setColumnAlignCenter(String key) {
        Grid.Column<LoanItem> col = loanItemsDataGrid.getColumnByKey(key);
        if (col != null) {
            col.setTextAlign(ColumnTextAlign.CENTER);
        }
    }

    private void initRenderers() {
        setColumnAlignCenter("bookCopy.barcode");
        setColumnAlignCenter("dueDate");
        setColumnAlignCenter("overdueStatus");
        setColumnAlignCenter("renewCount");
        setColumnAlignCenter("itemStatus");
        setColumnAlignCenter("returnCondition");
        setColumnAlignCenter("returnedAt");
        setColumnAlignCenter("receivedBy.username");

        // Mã vạch căn giữa
        loanItemsDataGrid.getColumnByKey("bookCopy.barcode").setRenderer(new ComponentRenderer<>(item -> {
            String bc = (item.getBookCopy() != null && item.getBookCopy().getBarcode() != null)
                    ? item.getBookCopy().getBarcode()
                    : "-";
            Span span = new Span(bc);
            span.getStyle().set("font-family", "monospace").set("font-weight", "500");
            return createCenteredCell(span);
        }));

        // Hạn trả căn giữa
        loanItemsDataGrid.getColumnByKey("dueDate").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getDueDate() != null) {
                return createCenteredCell(new Span(item.getDueDate().format(DATE_FMT)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Tình trạng hạn (Quá hạn / Còn lại / Đã trả)
        loanItemsDataGrid.getColumnByKey("overdueStatus").setRenderer(new ComponentRenderer<>(item -> {
            String status = item.getStatus();
            LocalDate due = item.getDueDate();
            LocalDate today = LocalDate.now();

            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "12px")
                    .set("font-size", "12px")
                    .set("font-weight", "600");

            if (LoanItemStatus.BORROWED.getId().equalsIgnoreCase(status)) {
                if (due != null && today.isAfter(due)) {
                    long days = ChronoUnit.DAYS.between(due, today);
                    badge.setText(" Quá hạn " + days + " ngày");
                    badge.getStyle().set("background-color", "#fee2e2").set("color", "#dc2626");
                } else if (due != null) {
                    long remaining = ChronoUnit.DAYS.between(today, due);
                    if (remaining == 0) {
                        badge.setText(" Hết hạn hôm nay");
                        badge.getStyle().set("background-color", "#fef3c7").set("color", "#d97706");
                    } else {
                        badge.setText(" Còn " + remaining + " ngày");
                        badge.getStyle().set("background-color", "#e0f2fe").set("color", "#0284c7");
                    }
                }
            } else if (LoanItemStatus.RETURNED.getId().equalsIgnoreCase(status)) {
                LocalDate returnDate = item.getReturnedAt() != null
                        ? item.getReturnedAt().toLocalDate()
                        : null;
                if (due != null && returnDate != null && returnDate.isAfter(due)) {
                    long overdueDays = ChronoUnit.DAYS.between(due, returnDate);
                    badge.setText("Trả trễ " + overdueDays + " ngày");
                    badge.getStyle().set("background-color", "#fef3c7").set("color", "#b45309");
                } else {
                    badge.setText("Đúng hạn");
                    badge.getStyle().set("background-color", "#dcfce7").set("color", "#15803d");
                }
            } else if (LoanItemStatus.LOST.getId().equalsIgnoreCase(status)) {
                badge.setText("Mất sách");
                badge.getStyle().set("background-color", "#f3f4f6").set("color", "#6b7280");
            }
            return createCenteredCell(badge);
        }));

        // Lần gia hạn căn giữa
        loanItemsDataGrid.getColumnByKey("renewCount").setRenderer(new ComponentRenderer<>(item -> {
            return createCenteredCell(
                    new Span(String.valueOf(item.getRenewCount() != null ? item.getRenewCount() : 0)));
        }));

        // Trạng thái mượn trả căn giữa
        loanItemsDataGrid.getColumnByKey("itemStatus").setRenderer(new ComponentRenderer<>(item -> {
            String st = item.getStatus();
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");

            if (LoanItemStatus.BORROWED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đang mượn");
                badge.getStyle().set("background-color", "#eff6ff").set("color", "#1d4ed8");
            } else if (LoanItemStatus.RETURNED.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã trả");
                badge.getStyle().set("background-color", "#f0fdf4").set("color", "#16a34a");
            } else if (LoanItemStatus.LOST.getId().equalsIgnoreCase(st)) {
                badge.setText("Đã mất");
                badge.getStyle().set("background-color", "#fef2f2").set("color", "#b91c1c");
            }
            return createCenteredCell(badge);
        }));

        // Ngày trả sạch sẽ (bỏ hoàn toàn +0700) căn giữa
        loanItemsDataGrid.getColumnByKey("returnedAt").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getReturnedAt() != null) {
                return createCenteredCell(new Span(item.getReturnedAt().format(DTF)));
            }
            return createCenteredCell(new Span("-"));
        }));

        // Tình trạng khi trả bằng tiếng Việt căn giữa
        loanItemsDataGrid.getColumnByKey("returnCondition").setRenderer(new ComponentRenderer<>(item -> {
            String cond = item.getReturnCondition();
            if (cond == null || cond.trim().isEmpty()) {
                return createCenteredCell(new Span("-"));
            }
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");

            if (ReturnCondition.GOOD.getId().equalsIgnoreCase(cond)) {
                badge.setText("Tốt / Bình thường");
                badge.getStyle().set("background-color", "#f0fdf4").set("color", "#15803d");
            } else if (ReturnCondition.DAMAGED.getId().equalsIgnoreCase(cond)) {
                badge.setText("Hư hỏng");
                badge.getStyle().set("background-color", "#fef3c7").set("color", "#b45309");
            } else if (ReturnCondition.LOST.getId().equalsIgnoreCase(cond)) {
                badge.setText("Bị mất");
                badge.getStyle().set("background-color", "#fee2e2").set("color", "#b91c1c");
            } else {
                badge.setText(cond);
            }
            return createCenteredCell(badge);
        }));

        // Thủ thư nhận trả căn giữa
        loanItemsDataGrid.getColumnByKey("receivedBy.username").setRenderer(new ComponentRenderer<>(item -> {
            if (item.getReceivedBy() != null && item.getReceivedBy().getUsername() != null) {
                return createCenteredCell(new Span(item.getReceivedBy().getUsername()));
            }
            return createCenteredCell(new Span("-"));
        }));
    }

    private void initActions() {
        detailReturnBtn.addClickListener(this::onDetailReturnClick);
        detailRenewBtn.addClickListener(this::onDetailRenewClick);
        closeBtn.addClickListener(e -> viewNavigators.view(this, LoanListView.class).navigate());
        saveCloseBtn.addClickListener(e -> {
            save().then(() -> viewNavigators.view(this, LoanListView.class).navigate());
        });
    }

    private void onDetailReturnClick(ClickEvent<Button> event) {
        LoanItem selected = loanItemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 cuốn sách đang mượn trong danh sách để trả!");
            return;
        }
        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Cuốn sách này đã được trả hoặc báo mất trước đó!");
            return;
        }

        String barcode = selected.getBookCopy() != null ? selected.getBookCopy().getBarcode() : "";
        openReturnDialog(barcode);
    }

    private void onDetailRenewClick(ClickEvent<Button> event) {
        LoanItem selected = loanItemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            toastService.warning("Vui lòng chọn 1 cuốn sách đang mượn trong danh sách để gia hạn!");
            return;
        }
        if (!LoanItemStatus.BORROWED.getId().equalsIgnoreCase(selected.getStatus())) {
            toastService.warning("Chỉ có thể gia hạn cuốn sách đang mượn!");
            return;
        }

        openRenewDialog(selected);
    }

    private void openReturnDialog(String barcode) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tiếp Nhận Trả Sách ");
        dialog.setWidth("38em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        RadioButtonGroup<ReturnCondition> conditionGroup = new RadioButtonGroup<>();
        conditionGroup.setLabel("Tình trạng sách khi trả");
        conditionGroup.setItems(ReturnCondition.GOOD, ReturnCondition.DAMAGED, ReturnCondition.LOST);
        conditionGroup.setItemLabelGenerator(ReturnCondition::getCaption);
        conditionGroup.setValue(ReturnCondition.GOOD);

        TextField noteField = new TextField("Ghi chú nhận trả");
        noteField.setWidthFull();
        noteField.setPlaceholder("Nhập ghi chú thêm nếu có...");

        layout.add(new Span("Mã vạch cuốn sách: " + barcode), conditionGroup, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Trả", e -> {
            try {
                User user = (User) currentAuthentication.getUser();
                LoanService.ReturnResult res = loanService.returnBookCopy(
                        barcode, conditionGroup.getValue(), user, noteField.getValue());

                dialog.close();
                loanItemsDl.load();

                if (res.isFineCreated() && res.getTotalFineAmount().compareTo(BigDecimal.ZERO) > 0) {
                    toastService.warning(String.format("Trả sách thành công! Độc giả phát sinh tiền phạt: %,.0f VNĐ",
                            res.getTotalFineAmount()));
                } else {
                    toastService.success("Nhận trả sách thành công!");
                }
            } catch (Exception ex) {
                toastService.error("Lỗi khi trả sách: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Đóng", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void openRenewDialog(LoanItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Gia Hạn Mượn Sách");
        dialog.setWidth("36em");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Member member = getEditedEntity().getMember();
        MemberType type = member != null ? member.getMemberType() : null;
        int maxRenewals = type != null && type.getMaxRenewals() != null ? type.getMaxRenewals() : 1;
        int renewDays = type != null && type.getRenewDays() != null ? type.getRenewDays() : 7;
        int currentRenewCount = item.getRenewCount() != null ? item.getRenewCount() : 0;
        LocalDate oldDue = item.getDueDate();
        LocalDate newDue = oldDue != null ? oldDue.plusDays(renewDays) : LocalDate.now().plusDays(renewDays);

        Div card = new Div();
        card.getStyle()
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "8px")
                .set("padding", "14px");

        FormLayout fl = new FormLayout();
        fl.addFormItem(new Span(item.getBookCopy() != null ? item.getBookCopy().getBarcode() : ""), "Mã vạch");
        fl.addFormItem(new Span(oldDue != null ? oldDue.toString() : ""), "Hạn trả hiện tại");
        fl.addFormItem(new Span(currentRenewCount + " / " + maxRenewals + " lần"), "Số lần đã gia hạn");

        Span newDueSpan = new Span(newDue.toString() + String.format(" (+%d ngày)", renewDays));
        newDueSpan.getStyle().set("font-weight", "700").set("color", "#059669");
        fl.addFormItem(newDueSpan, "Hạn trả mới dự kiến");

        card.add(fl);

        TextField noteField = new TextField("Ghi chú gia hạn");
        noteField.setWidthFull();

        layout.add(card, noteField);
        dialog.add(layout);

        Button confirmBtn = new Button("Xác nhận Gia hạn", e -> {
            try {
                User user = (User) currentAuthentication.getUser();
                loanService.renewLoanItem(item.getId(), user, RenewalChannel.COUNTER, noteField.getValue());
                dialog.close();
                loanItemsDl.load();
                toastService.success("Gia hạn thành công! Hạn mới: " + newDue);
            } catch (Exception ex) {
                toastService.error("Không thể gia hạn: " + ex.getMessage());
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Hủy", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
