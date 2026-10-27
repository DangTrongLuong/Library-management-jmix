package com.company.librarymanagementsystem.view.reader;

import com.company.librarymanagementsystem.entity.*;
import com.company.librarymanagementsystem.service.ReaderLoanService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "reader/loan-history", layout = MainView.class)
@ViewController(id = "ReaderLoanHistoryView")
@ViewDescriptor(path = "reader-loan-history-view.xml")
public class ReaderLoanHistoryView extends StandardView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DEFAULT_COVER = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=300&auto=format&fit=crop&q=80";

    @ViewComponent
    private Div kpiContainer;
    @ViewComponent
    private TextField searchKeywordField;
    @ViewComponent
    private ComboBox<String> statusFilter;
    @ViewComponent
    private Span historyCountLabel;
    @ViewComponent
    private Div historyGridWrapper;

    @Autowired
    private ReaderLoanService readerLoanService;
    @Autowired
    private ToastService toastService;

    private Member currentMember;
    private List<LoanItem> allHistoryItems = new ArrayList<>();
    private Grid<LoanItem> historyGrid;

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        loadHistoryData();
    }

    private void initFilters() {
        statusFilter.setItems("ALL", "ON_TIME", "OVERDUE", "LOST");
        statusFilter.setItemLabelGenerator(val -> switch (val) {
            case "ON_TIME" -> "Trả đúng hạn";
            case "OVERDUE" -> "Trả quá hạn";
            case "LOST" -> "Báo mất sách";
            default -> "Tất cả trạng thái";
        });
        statusFilter.setValue("ALL");

        searchKeywordField.addValueChangeListener(e -> applyFilter());
        statusFilter.addValueChangeListener(e -> applyFilter());
    }

    @Subscribe("refreshBtn")
    public void onRefreshBtnClick(final com.vaadin.flow.component.ClickEvent<?> event) {
        loadHistoryData();
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(final com.vaadin.flow.component.ClickEvent<?> event) {
        searchKeywordField.clear();
        statusFilter.setValue("ALL");
        applyFilter();
    }

    private void loadHistoryData() {
        Optional<Member> memberOpt = readerLoanService.getCurrentMember();
        if (memberOpt.isEmpty()) {
            renderNoMemberCard();
            return;
        }

        this.currentMember = memberOpt.get();
        allHistoryItems = readerLoanService.getLoanHistory(currentMember.getId());

        renderStats();
        applyFilter();
    }

    private void renderNoMemberCard() {
        historyGridWrapper.removeAll();
        Div banner = new Div();
        banner.addClassNames("bg-warning-10", "border", "border-warning-30", "rounded-m", "p-m", "text-center", "box-border", "flex", "flex-col", "items-center", "gap-s");
        
        H4 title = new H4("Tài khoản chưa liên kết Thẻ Độc giả");
        title.addClassNames("text-warning", "font-bold");
        title.getStyle().set("margin", "0 0 4px 0");
        
        Span desc = new Span("Nếu bạn đã có thẻ thư viện cấp tại quầy, bạn có thể liên kết thẻ vào tài khoản này để xem toàn bộ lịch sử mượn trả.");
        desc.addClassNames("text-secondary", "text-s");

        Button linkBtn = new Button("Đi đến trang Liên kết Thẻ Thư Viện", VaadinIcon.LINK.create(), e -> UI.getCurrent().navigate("reader-profile"));
        linkBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        banner.add(title, desc, linkBtn);
        historyGridWrapper.add(banner);
    }

    private void renderStats() {
        if (kpiContainer == null) return;
        kpiContainer.removeAll();

        int total = allHistoryItems.size();
        int onTime = 0;
        int issue = 0;

        for (LoanItem item : allHistoryItems) {
            if (LoanItemStatus.LOST.getId().equalsIgnoreCase(item.getStatus())) {
                issue++;
            } else if (item.getReturnedAt() != null && item.getDueDate() != null) {
                LocalDate returnDate = item.getReturnedAt().toLocalDate();
                if (returnDate.isAfter(item.getDueDate())) {
                    issue++;
                } else {
                    onTime++;
                }
            } else {
                onTime++;
            }
        }

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        // 1. Tổng lượt mượn đã trả
        grid.add(createKpiCard(
                "Tổng Lượt Mượn",
                String.format("%,d cuốn", total),
                "Toàn bộ lịch sử từ trước đến nay",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        // 2. Trả đúng hạn
        grid.add(createKpiCard(
                "Trả Đúng Hạn",
                String.format("%,d cuốn", onTime),
                "Giữ gìn uy tín mượn sách tốt",
                onTime > 0 ? "bg-success-10 border-success-30 text-success" : "bg-base border-contrast-10",
                onTime > 0 ? "text-success font-bold" : "text-primary"
        ));

        // 3. Từng quá hạn / Báo mất
        grid.add(createKpiCard(
                "Quá Hạn / Báo Mất",
                String.format("%,d cuốn", issue),
                issue > 0 ? "Phát sinh trễ hạn hoặc đền bù" : "Không có vi phạm",
                issue > 0 ? "bg-error-10 border-error-50 text-error" : "bg-base border-contrast-10",
                issue > 0 ? "text-error font-bold" : "text-primary"
        ));

        // 4. Tỷ lệ đúng hạn
        String rateStr = total > 0 ? String.format("%.0f%%", ((double) onTime / total) * 100) : "100%";
        grid.add(createKpiCard(
                "Tỷ Lệ Đúng Hạn",
                rateStr,
                "Chỉ số tin cậy của bạn đọc",
                "bg-base border-contrast-10",
                "text-primary"
        ));

        kpiContainer.add(grid);
    }

    private Div createKpiCard(String label, String value, String subtext, String boxClass, String valueClass) {
        Div card = new Div();
        card.addClassNames("bg-base", "rounded-m", "border", "p-s", "shadow-xs", "flex", "flex-col", "justify-between", "box-border");
        card.getStyle().set("box-sizing", "border-box").set("min-width", "0");
        if (boxClass.contains("border-")) {
            for (String c : boxClass.split(" ")) {
                if (!c.isBlank()) card.addClassName(c);
            }
        } else {
            card.addClassNames("border-contrast-10");
        }

        Span lbl = new Span(label);
        lbl.addClassNames("text-secondary", "font-medium", "uppercase");
        lbl.getStyle().set("font-size", "10.5px");
        lbl.getStyle().set("letter-spacing", "0.5px");

        Span val = new Span(value);
        val.addClassNames("font-bold", "my-xs");
        val.getStyle().set("font-size", "18px");
        for (String c : valueClass.split(" ")) {
            if (!c.isBlank()) val.addClassName(c);
        }

        Span sub = new Span(subtext);
        sub.addClassNames("text-secondary");
        sub.getStyle().set("font-size", "11px");

        card.add(lbl, val, sub);
        return card;
    }

    private void applyFilter() {
        if (allHistoryItems == null) return;

        String kw = searchKeywordField.getValue() != null ? searchKeywordField.getValue().trim().toLowerCase() : "";
        String status = statusFilter.getValue() != null ? statusFilter.getValue() : "ALL";

        List<LoanItem> filtered = allHistoryItems.stream().filter(item -> {
            // Lọc từ khóa
            if (!kw.isEmpty()) {
                String title = (item.getBookCopy() != null && item.getBookCopy().getBook() != null)
                        ? item.getBookCopy().getBook().getTitle().toLowerCase() : "";
                String barcode = item.getBookCopy() != null && item.getBookCopy().getBarcode() != null
                        ? item.getBookCopy().getBarcode().toLowerCase() : "";
                String authors = (item.getBookCopy() != null && item.getBookCopy().getBook() != null
                        && item.getBookCopy().getBook().getAuthors() != null)
                        ? item.getBookCopy().getBook().getAuthors().stream().map(a -> a.getFullName().toLowerCase()).collect(Collectors.joining(" "))
                        : "";

                if (!title.contains(kw) && !barcode.contains(kw) && !authors.contains(kw)) {
                    return false;
                }
            }

            // Lọc trạng thái
            if ("LOST".equals(status)) {
                return LoanItemStatus.LOST.getId().equalsIgnoreCase(item.getStatus());
            } else if ("ON_TIME".equals(status)) {
                if (LoanItemStatus.LOST.getId().equalsIgnoreCase(item.getStatus())) return false;
                if (item.getReturnedAt() != null && item.getDueDate() != null) {
                    return !item.getReturnedAt().toLocalDate().isAfter(item.getDueDate());
                }
                return true;
            } else if ("OVERDUE".equals(status)) {
                if (LoanItemStatus.LOST.getId().equalsIgnoreCase(item.getStatus())) return false;
                if (item.getReturnedAt() != null && item.getDueDate() != null) {
                    return item.getReturnedAt().toLocalDate().isAfter(item.getDueDate());
                }
                return false;
            }

            return true;
        }).collect(Collectors.toList());

        historyCountLabel.setText(String.format("Hiển thị %d / %d lượt mượn", filtered.size(), allHistoryItems.size()));
        renderHistoryGrid(filtered);
    }

    private void renderHistoryGrid(List<LoanItem> items) {
        historyGridWrapper.removeAll();

        if (items.isEmpty()) {
            Div emptyDiv = new Div();
            emptyDiv.getStyle()
                    .set("text-align", "center")
                    .set("padding", "40px")
                    .set("color", "#64748b");
            emptyDiv.add(new H4("Không có bản ghi lịch sử mượn trả nào phù hợp"));
            historyGridWrapper.add(emptyDiv);
            return;
        }

        historyGrid = new Grid<>();
        historyGrid.setWidthFull();
        historyGrid.setAllRowsVisible(true);
        historyGrid.setItems(items);

        // Bìa sách
        historyGrid.addColumn(new ComponentRenderer<>(item -> {
            String url = (item.getBookCopy() != null && item.getBookCopy().getBook() != null
                    && item.getBookCopy().getBook().getCoverImageUrl() != null && !item.getBookCopy().getBook().getCoverImageUrl().isBlank())
                    ? item.getBookCopy().getBook().getCoverImageUrl() : DEFAULT_COVER;
            Image img = new Image(url, "");
            img.setWidth("36px");
            img.setHeight("50px");
            img.getStyle().set("object-fit", "cover").set("border-radius", "4px").set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)");
            return img;
        })).setHeader("Bìa").setAutoWidth(true);

        // Nhan đề & Tác giả
        historyGrid.addColumn(new ComponentRenderer<>(item -> {
            VerticalLayout vl = new VerticalLayout();
            vl.setSpacing(false);
            vl.setPadding(false);

            String title = (item.getBookCopy() != null && item.getBookCopy().getBook() != null)
                    ? item.getBookCopy().getBook().getTitle() : "-";
            Span tSpan = new Span(title);
            tSpan.getStyle().set("font-weight", "600").set("color", "#1e293b");

            String authors = (item.getBookCopy() != null && item.getBookCopy().getBook() != null
                    && item.getBookCopy().getBook().getAuthors() != null && !item.getBookCopy().getBook().getAuthors().isEmpty())
                    ? item.getBookCopy().getBook().getAuthors().stream().map(Author::getFullName).collect(Collectors.joining(", ")) : "Chưa rõ";
            Span aSpan = new Span(authors);
            aSpan.getStyle().set("font-size", "12px").set("color", "#64748b");

            vl.add(tSpan, aSpan);
            return vl;
        })).setHeader("Nhan đề sách & Tác giả").setAutoWidth(true).setFlexGrow(2);

        // Mã vạch
        historyGrid.addColumn(item -> item.getBookCopy() != null ? item.getBookCopy().getBarcode() : "-")
                .setHeader("Mã vạch").setAutoWidth(true);

        // Ngày mượn
        historyGrid.addColumn(item -> (item.getLoan() != null && item.getLoan().getLoanDate() != null)
                        ? item.getLoan().getLoanDate().format(DATE_FMT) : "-")
                .setHeader("Ngày mượn").setAutoWidth(true);

        // Hạn trả
        historyGrid.addColumn(item -> item.getDueDate() != null ? item.getDueDate().format(DATE_FMT) : "-")
                .setHeader("Hạn trả").setAutoWidth(true);

        // Ngày thực trả
        historyGrid.addColumn(item -> item.getReturnedAt() != null ? item.getReturnedAt().format(DATE_FMT) : "-")
                .setHeader("Ngày hoàn trả").setAutoWidth(true);

        // Trạng thái trả
        historyGrid.addColumn(new ComponentRenderer<>(item -> {
            Span pill = new Span();
            pill.getStyle().set("padding", "3px 8px").set("border-radius", "8px").set("font-size", "11px").set("font-weight", "600");

            if (LoanItemStatus.LOST.getId().equalsIgnoreCase(item.getStatus())) {
                pill.setText("Báo mất sách");
                pill.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else if (item.getReturnedAt() != null && item.getDueDate() != null) {
                LocalDate returnDate = item.getReturnedAt().toLocalDate();
                if (returnDate.isAfter(item.getDueDate())) {
                    long days = ChronoUnit.DAYS.between(item.getDueDate(), returnDate);
                    pill.setText(String.format("Trễ %d ngày", days));
                    pill.getStyle().set("background", "#fef3c7").set("color", "#b45309");
                } else {
                    pill.setText("Đúng hạn");
                    pill.getStyle().set("background", "#dcfce7").set("color", "#15803d");
                }
            } else {
                pill.setText("Đã hoàn tất");
                pill.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            }

            return pill;
        })).setHeader("Kết quả mượn").setAutoWidth(true);

        // Đã gia hạn
        historyGrid.addColumn(item -> String.format("%d lần", item.getRenewCount() != null ? item.getRenewCount() : 0))
                .setHeader("Đã gia hạn").setAutoWidth(true);

        historyGridWrapper.add(historyGrid);
    }
}
