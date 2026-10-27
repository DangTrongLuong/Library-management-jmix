package com.company.librarymanagementsystem.view.manager;

import com.company.librarymanagementsystem.service.ManagerDashboardService;
import com.company.librarymanagementsystem.service.ToastService;
import com.company.librarymanagementsystem.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.download.ByteArrayDownloadDataProvider;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "manager-dashboard", layout = MainView.class)
@ViewController("ManagerDashboardView")
@ViewDescriptor("manager-dashboard-view.xml")
@JavaScript("https://cdn.jsdelivr.net/npm/chart.js")
public class ManagerDashboardView extends StandardView {

    @ViewComponent
    private Div kpiCardsContainer;
    @ViewComponent
    private Div chartsContainer;
    @ViewComponent
    private Div tablesContainer;

    @ViewComponent
    private Button refreshBtn;
    @ViewComponent
    private Button exportExcelBtn;

    @ViewComponent
    private Button navWaiveBtn;
    @ViewComponent
    private Button navExceptionBtn;
    @ViewComponent
    private Button navSettingsBtn;
    @ViewComponent
    private Button navAuditBtn;

    @Autowired
    private ManagerDashboardService dashboardService;
    @Autowired
    private ToastService toastService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private Downloader downloader;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Subscribe
    public void onInit(final InitEvent event) {
        getContent().setWidthFull();
        getContent().setMaxWidth("100%");
        getContent().getStyle().set("box-sizing", "border-box").set("overflow-x", "hidden");
        initNavigationButtons();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        loadDashboardData();
    }

    private void initNavigationButtons() {
        refreshBtn.addClickListener(e -> {
            dashboardService.invalidateCache();
            loadDashboardData();
            toastService.success("Đã làm mới dữ liệu thống kê!");
        });

        navWaiveBtn.addClickListener(e -> viewNavigators.view(this, FineWaiveApprovalView.class).navigate());
        navExceptionBtn.addClickListener(e -> viewNavigators.view(this, ExceptionHandlingView.class).navigate());
        navSettingsBtn.addClickListener(e -> viewNavigators.view(this, LibrarySettingView.class).navigate());
        navAuditBtn.addClickListener(e -> viewNavigators.view(this, AuditLogListView.class).navigate());

        exportExcelBtn.addClickListener(this::onExportReportClick);
    }

    private void loadDashboardData() {
        ManagerDashboardService.DashboardStats stats = dashboardService.getDashboardStats();

        renderKpiCards(stats);
        renderCharts(stats);
        renderDataTables(stats);
    }

    private void renderKpiCards(ManagerDashboardService.DashboardStats stats) {
        kpiCardsContainer.removeAll();

        Div grid = new Div();
        grid.addClassNames("grid", "grid-cols-2", "lg:grid-cols-4", "gap-s", "w-full");
        grid.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0");

        // Thẻ 1: Kho Sách & Bản Sao (Tông xanh dương nhẹ, viền trang nhã)
        grid.add(createKpiCard(
                "Kho Sách & Bản Sao",
                String.format("%,d cuốn", stats.totalCopies()),
                String.format("Sẵn sàng: %,d | Đang mượn: %,d", stats.availableCopies(), stats.borrowedCopies()),
                "#f0f9ff",
                "#bae6fd",
                "#0284c7",
                "#0369a1",
                VaadinIcon.BOOK));

        // Thẻ 2: Lượt Mượn Hoạt Động (Tông xanh ngọc nhẹ)
        grid.add(createKpiCard(
                "Lượt Mượn Hoạt Động",
                String.format("%,d cuốn", stats.activeLoansCount()),
                String.format("%,d độc giả đang mượn", stats.activeMembersCount()),
                "#f0fdf4",
                "#bbf7d0",
                "#16a34a",
                "#15803d",
                VaadinIcon.EXCHANGE));

        // Thẻ 3: Sách Quá Hạn (Tông cam hổ phách / đỏ nhạt)
        String overdueSub = String.format("1-7 ngày: %d | 8-30 ngày: %d | >30 ngày: %d",
                stats.overdue1to7Days(), stats.overdue8to30Days(), stats.overdueOver30Days());
        boolean hasOverdue = stats.overdueCount() > 0;
        grid.add(createKpiCard(
                "Sách Quá Hạn",
                String.format("%,d cuốn", stats.overdueCount()),
                overdueSub,
                hasOverdue ? "#fff1f2" : "#fffbeb",
                hasOverdue ? "#fecdd3" : "#fde68a",
                hasOverdue ? "#e11d48" : "#d97706",
                hasOverdue ? "#be123c" : "#b45309",
                VaadinIcon.WARNING));

        // Thẻ 4: Doanh Thu Phạt (Tông tím pastel nhẹ)
        grid.add(createKpiCard(
                "Doanh Thu Phạt",
                String.format("%s đ", currencyFormat.format(stats.totalRevenuePaid())),
                String.format("Nợ: %s đ | Miễn: %s đ",
                        currencyFormat.format(stats.totalOutstandingFines()),
                        currencyFormat.format(stats.totalWaivedFines())),
                "#faf5ff",
                "#e9d5ff",
                "#9333ea",
                "#7e22ce",
                VaadinIcon.MONEY));

        kpiCardsContainer.add(grid);
    }

    private Div createKpiCard(String title, String value, String subtext,
            String cardBg, String borderColor, String iconColor, String titleColor,
            VaadinIcon icon) {
        Div card = new Div();
        card.addClassNames("rounded-m", "p-s", "border", "shadow-xs", "flex", "flex-col", "justify-between");
        card.getStyle()
                .set("background", cardBg)
                .set("border-color", borderColor)
                .set("box-sizing", "border-box")
                .set("min-width", "0")
                .set("min-height", "82px")
                .set("position", "relative")
                .set("overflow", "hidden");

        // Dòng trên: Tiêu đề + Icon vuông nhỏ
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.setSpacing(false);
        topRow.setPadding(false);

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", titleColor)
                .set("letter-spacing", "0.2px");

        Div iconBox = new Div();
        iconBox.getStyle()
                .set("width", "26px")
                .set("height", "26px")
                .set("border-radius", "6px")
                .set("background", "rgba(255, 255, 255, 0.7)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("border", "1px solid " + borderColor)
                .set("flex-shrink", "0");

        var iconComp = icon.create();
        iconComp.getStyle().set("width", "14px").set("height", "14px").set("color", iconColor);
        iconBox.add(iconComp);

        topRow.add(titleSpan, iconBox);

        // Dòng giữa: Giá trị KPI lớn vừa phải, đậm nét (font chữ hỗ trợ đầy đủ tiếng Việt)
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-family", "var(--lumo-font-family, 'Plus Jakarta Sans', 'Inter', sans-serif)")
                .set("font-size", "20px")
                .set("font-weight", "800")
                .set("color", "#0f172a")
                .set("margin", "2px 0");

        // Dòng dưới: Chú thích nhỏ gọn, không tràn lề
        Span subSpan = new Span(subtext);
        subSpan.getStyle()
                .set("font-family", "var(--lumo-font-family, 'Plus Jakarta Sans', 'Inter', sans-serif)")
                .set("font-size", "11px")
                .set("color", "#64748b")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        card.add(topRow, valueSpan, subSpan);
        return card;
    }

    private void renderCharts(ManagerDashboardService.DashboardStats stats) {
        chartsContainer.removeAll();

        // Box 1: Biểu đồ kết hợp Cột & Đường mượt mà (Combo Line & Bar Chart)
        Div chartBox1 = createChartBox("Xu Hướng Mượn & Trả Sách (7 Ngày Gần Nhất)", "chartLoanTrend");
        // Box 2: Biểu đồ Donut: Cơ Cấu Mượn Theo Thể Loại
        Div chartBox2 = createChartBox("Cơ Cấu Mượn Sách Theo Thể Loại", "chartCategoryDonut");

        chartsContainer.add(chartBox1, chartBox2);

        // Nạp dữ liệu xu hướng mượn & trả 7 ngày gần nhất từ CSDL
        ManagerDashboardService.LoanReturnDailyTrend trend = dashboardService.get7DaysLoanReturnTrend();
        StringBuilder trendLabels = new StringBuilder("[");
        StringBuilder trendBorrows = new StringBuilder("[");
        StringBuilder trendReturns = new StringBuilder("[");
        for (int i = 0; i < trend.labels().size(); i++) {
            if (i > 0) {
                trendLabels.append(",");
                trendBorrows.append(",");
                trendReturns.append(",");
            }
            trendLabels.append("'").append(trend.labels().get(i)).append("'");
            trendBorrows.append(trend.borrowCounts().get(i));
            trendReturns.append(trend.returnCounts().get(i));
        }
        trendLabels.append("]");
        trendBorrows.append("]");
        trendReturns.append("]");

        // Nạp dữ liệu cơ cấu mượn theo thể loại
        Map<String, Long> catData = dashboardService.getBorrowCountByCategory();
        StringBuilder catLabels = new StringBuilder("[");
        StringBuilder catValues = new StringBuilder("[");
        int idx = 0;
        for (Map.Entry<String, Long> entry : catData.entrySet()) {
            if (idx > 0) {
                catLabels.append(",");
                catValues.append(",");
            }
            catLabels.append("'").append(entry.getKey()).append("'");
            catValues.append(entry.getValue());
            idx++;
        }
        catLabels.append("]");
        catValues.append("]");

        String jsCode = String.format("""
                    setTimeout(function() {
                        if (typeof Chart === 'undefined') return;

                        // 1. Chart Xu Hướng Mượn/Trả: Kết hợp Cột (Mượn) & Đường cong mượt mà (Trả) với dữ liệu 7 ngày thật
                        var ctx1 = document.getElementById('chartLoanTrend');
                        if (ctx1) {
                            if (window.myLoanTrendChart) window.myLoanTrendChart.destroy();
                            window.myLoanTrendChart = new Chart(ctx1, {
                                type: 'bar',
                                data: {
                                    labels: %s,
                                    datasets: [
                                        {
                                            type: 'bar',
                                            label: 'Lượt Mượn',
                                            data: %s,
                                            backgroundColor: 'rgba(14, 165, 233, 0.8)',
                                            borderColor: '#0284c7',
                                            borderWidth: 1,
                                            borderRadius: { topLeft: 3, topRight: 3, bottomLeft: 0, bottomRight: 0 },
                                            borderSkipped: 'bottom',
                                            barPercentage: 0.55,
                                            categoryPercentage: 0.7,
                                            order: 2
                                        },
                                        {
                                            type: 'line',
                                            label: 'Lượt Trả',
                                            data: %s,
                                            borderColor: '#10b981',
                                            backgroundColor: 'rgba(16, 185, 129, 0.12)',
                                            borderWidth: 2.5,
                                            tension: 0.4,
                                            fill: true,
                                            pointBackgroundColor: '#ffffff',
                                            pointBorderColor: '#10b981',
                                            pointBorderWidth: 1.5,
                                            pointRadius: 2.5,
                                            pointHoverRadius: 5,
                                            order: 1
                                        }
                                    ]
                                },
                                options: {
                                    responsive: true,
                                    maintainAspectRatio: false,
                                    interaction: {
                                        mode: 'index',
                                        intersect: false
                                    },
                                    plugins: {
                                        legend: {
                                            position: 'top',
                                            align: 'end',
                                            labels: {
                                                boxWidth: 12,
                                                boxHeight: 10,
                                                borderRadius: 2,
                                                padding: 12,
                                                font: { size: 11, family: "'Plus Jakarta Sans', 'Inter', sans-serif" },
                                                color: '#475569'
                                            }
                                        }
                                    },
                                    scales: {
                                        y: {
                                            beginAtZero: true,
                                            grid: { color: 'rgba(241, 245, 249, 0.9)' },
                                            ticks: {
                                                font: { size: 11, family: "'Plus Jakarta Sans', 'Inter', sans-serif" },
                                                precision: 0
                                            }
                                        },
                                        x: {
                                            grid: { display: false },
                                            ticks: { font: { size: 11, family: "'Plus Jakarta Sans', 'Inter', sans-serif" } }
                                        }
                                    }
                                }
                            });
                        }

                        // 2. Chart Cơ cấu thể loại Donut
                        var ctx2 = document.getElementById('chartCategoryDonut');
                        if (ctx2) {
                            if (window.myCatDonutChart) window.myCatDonutChart.destroy();
                            window.myCatDonutChart = new Chart(ctx2, {
                                type: 'doughnut',
                                data: {
                                    labels: %s,
                                    datasets: [{
                                        data: %s,
                                        backgroundColor: [
                                            '#0284c7', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#64748b'
                                        ],
                                        borderWidth: 2,
                                        borderColor: '#ffffff',
                                        hoverOffset: 4
                                    }]
                                },
                                options: {
                                    responsive: true,
                                    maintainAspectRatio: false,
                                    radius: '76%%',
                                    cutout: '66%%',
                                    layout: {
                                        padding: {
                                            top: 4,
                                            bottom: 4,
                                            left: 10,
                                            right: 10
                                        }
                                    },
                                    plugins: {
                                        legend: {
                                            position: 'right',
                                            labels: {
                                                boxWidth: 12,
                                                boxHeight: 10,
                                                borderRadius: 2,
                                                padding: 9,
                                                font: { size: 11, family: "'Plus Jakarta Sans', 'Inter', sans-serif" },
                                                color: '#475569'
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }, 300);
                """, trendLabels, trendBorrows, trendReturns, catLabels, catValues);

        UI.getCurrent().getPage().executeJs(jsCode);
    }

    private Div createChartBox(String title, String canvasId) {
        Div box = new Div();
        box.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs", "flex",
                "flex-col");
        box.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0")
                .set("min-height", "310px")
                .set("overflow", "hidden");

        H4 titleHeader = new H4(title);
        titleHeader.addClassNames("m-0", "mb-s", "text-m", "font-bold", "text-primary");
        box.add(titleHeader);

        Div canvasContainer = new Div();
        canvasContainer.getStyle()
                .set("position", "relative")
                .set("width", "100%")
                .set("max-width", "100%")
                .set("min-width", "0")
                .set("height", "240px")
                .set("overflow", "hidden");

        canvasContainer.getElement()
                .executeJs("this.innerHTML = '<canvas id=\"" + canvasId + "\" style=\"max-width:100%;\"></canvas>';");

        box.add(canvasContainer);
        return box;
    }

    private void renderDataTables(ManagerDashboardService.DashboardStats stats) {
        tablesContainer.removeAll();

        // Bảng 1: Top 5 sách mượn nhiều nhất
        Div topBooksBox = createCardContainer("Top 5 Đầu Sách Mượn Nhiều Nhất");
        List<ManagerDashboardService.TopBookItem> topBooks = dashboardService.getTopBorrowedBooks(5);

        VerticalLayout bookListLayout = new VerticalLayout();
        bookListLayout.setPadding(false);
        bookListLayout.setSpacing(true);

        int rank = 1;
        for (ManagerDashboardService.TopBookItem item : topBooks) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.addClassNames("p-s", "border-b", "border-contrast-5");
            row.getStyle()
                    .set("gap", "12px")
                    .set("box-sizing", "border-box")
                    .set("min-width", "0");

            Span rankBadge = new Span("#" + rank);
            rankBadge.getElement().getThemeList().add("badge");
            if (rank == 1) {
                rankBadge.getElement().getThemeList().add("primary");
            } else {
                rankBadge.getElement().getThemeList().add("contrast");
            }
            rankBadge.getStyle()
                    .set("font-size", "11px")
                    .set("font-weight", "700")
                    .set("padding", "2px 6px")
                    .set("border-radius", "6px")
                    .set("background-color", rank == 1 ? "#0284c7" : "#f1f5f9")
                    .set("color", rank == 1 ? "#ffffff" : "#475569")
                    .set("border", rank == 1 ? "1px solid #0284c7" : "1px solid #cbd5e1")
                    .set("min-width", "28px")
                    .set("text-align", "center")
                    .set("flex-shrink", "0")
                    .set("white-space", "nowrap");

            VerticalLayout infoCol = new VerticalLayout();
            infoCol.setPadding(false);
            infoCol.setSpacing(false);
            infoCol.getStyle().set("flex-grow", "1").set("min-width", "0");

            Span title = new Span(item.title());
            title.getStyle()
                    .set("font-weight", "600")
                    .set("font-size", "14px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");

            Span sub = new Span(item.category() + " • " + item.author());
            sub.addClassNames("text-secondary");
            sub.getStyle()
                    .set("font-size", "12px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");

            infoCol.add(title, sub);

            Span countBadge = new Span(item.borrowCount() + " lượt mượn");
            countBadge.getElement().getThemeList().add("badge");
            countBadge.getElement().getThemeList().add("success");
            countBadge.getElement().getThemeList().add("small");
            countBadge.getStyle()
                    .set("font-size", "11px")
                    .set("font-weight", "600")
                    .set("padding", "2px 8px")
                    .set("border-radius", "12px")
                    .set("background-color", "#ecfdf5")
                    .set("color", "#047857")
                    .set("border", "1px solid #a7f3d0")
                    .set("white-space", "nowrap")
                    .set("flex-shrink", "0");

            row.add(rankBadge, infoCol, countBadge);
            bookListLayout.add(row);
            rank++;
        }
        topBooksBox.add(bookListLayout);

        // Bảng 2: Danh sách sách quá hạn khẩn cấp
        Div overdueBox = createCardContainer("Danh Sách Quá Hạn Cần Nhắc Nhở");
        List<ManagerDashboardService.OverdueBookItem> overdues = dashboardService.getRecentOverdueList(5);

        VerticalLayout overdueListLayout = new VerticalLayout();
        overdueListLayout.setPadding(false);
        overdueListLayout.setSpacing(true);

        if (overdues.isEmpty()) {
            Div emptyBox = new Div(new Span("Hiện không có sách nào bị trễ hạn cần nhắc nhở!"));
            emptyBox.addClassNames("p-m", "text-secondary", "text-center", "w-full");
            overdueListLayout.add(emptyBox);
        } else {
            for (ManagerDashboardService.OverdueBookItem item : overdues) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.addClassNames("p-s", "border-b", "border-contrast-5");
                row.getStyle()
                        .set("gap", "12px")
                        .set("box-sizing", "border-box")
                        .set("min-width", "0");

                Span warnIcon = new Span(VaadinIcon.EXCLAMATION_CIRCLE_O.create());
                warnIcon.getStyle().set("color", "#e11d48").set("font-size", "20px");

                VerticalLayout infoCol = new VerticalLayout();
                infoCol.setPadding(false);
                infoCol.setSpacing(false);
                infoCol.getStyle().set("flex-grow", "1").set("min-width", "0");

                Span title = new Span(item.bookTitle() + " (" + item.copyBarcode() + ")");
                title.getStyle()
                        .set("font-weight", "600")
                        .set("font-size", "13.5px")
                        .set("white-space", "nowrap")
                        .set("overflow", "hidden")
                        .set("text-overflow", "ellipsis");

                Span sub = new Span(String.format("Độc giả: %s • SĐT: %s • Hạn trả: %s",
                        item.memberName(), item.memberPhone(),
                        item.dueDate() != null ? dateFormatter.format(item.dueDate()) : "-"));
                sub.addClassNames("text-secondary");
                sub.getStyle()
                        .set("font-size", "11.5px")
                        .set("white-space", "nowrap")
                        .set("overflow", "hidden")
                        .set("text-overflow", "ellipsis");

                infoCol.add(title, sub);

                Span fineBadge = new Span(
                        "Trễ " + item.daysOverdue() + " ngày (" + currencyFormat.format(item.estimatedFine()) + "đ)");
                fineBadge.getElement().getThemeList().add("badge");
                fineBadge.getElement().getThemeList().add("error");
                fineBadge.getElement().getThemeList().add("small");
                fineBadge.getStyle()
                        .set("font-size", "11px")
                        .set("font-weight", "600")
                        .set("padding", "2px 8px")
                        .set("border-radius", "12px")
                        .set("background-color", "#fff1f2")
                        .set("color", "#be123c")
                        .set("border", "1px solid #fecdd3")
                        .set("white-space", "nowrap")
                        .set("flex-shrink", "0");

                row.add(warnIcon, infoCol, fineBadge);
                overdueListLayout.add(row);
            }
        }
        overdueBox.add(overdueListLayout);

        tablesContainer.add(topBooksBox, overdueBox);
    }

    private Div createCardContainer(String title) {
        Div box = new Div();
        box.addClassNames("bg-base", "p-m", "rounded-l", "border", "border-contrast-10", "shadow-xs", "flex",
                "flex-col");
        box.getStyle()
                .set("box-sizing", "border-box")
                .set("max-width", "100%")
                .set("min-width", "0")
                .set("min-height", "280px")
                .set("overflow", "hidden");

        H4 titleHeader = new H4(title);
        titleHeader.addClassNames("m-0", "mb-s", "text-m", "font-bold", "text-primary");
        box.add(titleHeader);
        return box;
    }

    private void onExportReportClick(ClickEvent<Button> event) {
        ManagerDashboardService.DashboardStats stats = dashboardService.getDashboardStats();

        StringBuilder csv = new StringBuilder();
        // BOM for UTF-8 in Excel
        csv.append("\uFEFF");
        csv.append("BÁO CÁO VẬN HÀNH THƯ VIỆN - ROLE MANAGER\n");
        csv.append("Ngày xuất báo cáo:,").append(LocalDate.now().format(dateFormatter)).append("\n\n");

        csv.append("--- CHỈ SỐ LƯU THÔNG & KHO SÁCH ---\n");
        csv.append("Tổng số đầu sách:,").append(stats.totalBooks()).append("\n");
        csv.append("Tổng số bản sao:,").append(stats.totalCopies()).append("\n");
        csv.append("Bản sao sẵn sàng (AVAILABLE):,").append(stats.availableCopies()).append("\n");
        csv.append("Bản sao đang mượn (BORROWED):,").append(stats.borrowedCopies()).append("\n");
        csv.append("Bản sao hư hỏng/mất/bảo trì:,").append(stats.maintenanceCopies()).append("\n\n");

        csv.append("--- CHỈ SỐ MƯỢN TRẢ & QUÁ HẠN ---\n");
        csv.append("Số lượt mượn đang diễn ra:,").append(stats.activeLoansCount()).append("\n");
        csv.append("Tổng số sách quá hạn:,").append(stats.overdueCount()).append("\n");
        csv.append("Quá hạn từ 1-7 ngày:,").append(stats.overdue1to7Days()).append("\n");
        csv.append("Quá hạn từ 8-30 ngày:,").append(stats.overdue8to30Days()).append("\n");
        csv.append("Quá hạn trên 30 ngày:,").append(stats.overdueOver30Days()).append("\n\n");

        csv.append("--- TÀI CHÍNH & TIỀN PHẠT ---\n");
        csv.append("Tổng doanh thu phạt đã thu (VNĐ):,").append(stats.totalRevenuePaid()).append("\n");
        csv.append("Tổng tiền phạt còn nợ (VNĐ):,").append(stats.totalOutstandingFines()).append("\n");
        csv.append("Tổng tiền phạt đã miễn giảm (VNĐ):,").append(stats.totalWaivedFines()).append("\n\n");

        csv.append("--- ĐỘC GIẢ & ĐẶT TRƯỚC ---\n");
        csv.append("Độc giả hoạt động trong 90 ngày:,").append(stats.activeMembersCount()).append("\n");
        csv.append("Yêu cầu đặt trước chờ nhận (PENDING):,").append(stats.pendingReservationsCount()).append("\n");

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        downloader.download(new ByteArrayDownloadDataProvider(bytes, 8192, "UTF-8"),
                "BaoCao_QuanTri_ThuVien_" + LocalDate.now() + ".csv");
        toastService.success("Đã xuất báo cáo thống kê quản trị thành công!");
    }
}
