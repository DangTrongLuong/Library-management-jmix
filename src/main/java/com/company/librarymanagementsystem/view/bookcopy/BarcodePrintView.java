package com.company.librarymanagementsystem.view.bookcopy;

import com.company.librarymanagementsystem.entity.BookCopy;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.view.*;

import java.util.List;

@ViewController(id = "Barcode.print")
@ViewDescriptor(path = "barcode-print-view.xml")
@DialogMode(width = "52em")
public class BarcodePrintView extends StandardView {

    @ViewComponent
    private VerticalLayout barcodesContainer;

    private List<BookCopy> copiesToPrint;

    public void setCopiesToPrint(List<BookCopy> copies) {
        this.copiesToPrint = copies;
        if (barcodesContainer != null) {
            renderBarcodeCards();
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        renderBarcodeCards();
    }

    private void renderBarcodeCards() {
        if (copiesToPrint == null || copiesToPrint.isEmpty() || barcodesContainer == null) {
            return;
        }

        barcodesContainer.removeAll();

        HorizontalLayout rowLayout = null;
        int count = 0;

        for (BookCopy copy : copiesToPrint) {
            if (count % 2 == 0) {
                rowLayout = new HorizontalLayout();
                rowLayout.setWidthFull();
                rowLayout.setSpacing(true);
                barcodesContainer.add(rowLayout);
            }

            Div card = createBarcodeCard(copy);
            if (rowLayout != null) {
                rowLayout.add(card);
            }
            count++;
        }
    }

    private Div createBarcodeCard(BookCopy copy) {
        Div card = new Div();
        card.getStyle()
                .set("border", "2px dashed #94a3b8")
                .set("border-radius", "8px")
                .set("padding", "14px 18px")
                .set("background", "#ffffff")
                .set("width", "48%")
                .set("box-sizing", "border-box")
                .set("text-align", "center")
                .set("display", "inline-block")
                .set("margin-bottom", "10px");

        Span libHeader = new Span("THƯ VIỆN TRUNG TÂM");
        libHeader.getStyle()
                .set("font-size", "0.75rem")
                .set("font-weight", "bold")
                .set("color", "#475569")
                .set("letter-spacing", "1px")
                .set("display", "block");

        String bookTitle = copy.getBook() != null ? copy.getBook().getTitle() : "Tài liệu thư viện";
        if (bookTitle.length() > 38) {
            bookTitle = bookTitle.substring(0, 35) + "...";
        }
        H4 titleHeader = new H4(bookTitle);
        titleHeader.getStyle()
                .set("margin", "4px 0 8px")
                .set("font-size", "0.95rem")
                .set("color", "#0f172a")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        // SVG Barcode representation
        String barcode = (copy != null && copy.getBarcode() != null) ? copy.getBarcode() : "";
        Div svgBarcode = generateSvgBarcode(barcode);

        Span barcodeText = new Span("* " + (!barcode.isEmpty() ? barcode : "NO-BARCODE") + " *");
        barcodeText.getStyle()
                .set("font-family", "monospace")
                .set("font-weight", "bold")
                .set("font-size", "1.1rem")
                .set("letter-spacing", "2px")
                .set("display", "block")
                .set("margin-top", "4px")
                .set("color", "#000000");

        String locText = (copy != null && copy.getLocation() != null && copy.getLocation().getCode() != null) 
                ? copy.getLocation().getCode() : "Chưa xếp kệ";
        Span locationText = new Span("Vị trí: " + locText);
        locationText.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "#64748b")
                .set("display", "block")
                .set("margin-top", "4px");

        card.add(libHeader, titleHeader, svgBarcode, barcodeText, locationText);
        return card;
    }

    private Div generateSvgBarcode(String code) {
        if (code == null || code.isEmpty()) {
            code = "UNKNOWN";
        }
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "flex-end")
                .set("height", "45px")
                .set("margin", "6px 0");

        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='180' height='45' xmlns='http://www.w3.org/2000/svg'>");

        // Generate lines based on characters of the barcode
        int x = 10;
        byte[] bytes = code.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            int val = bytes[i];
            int width1 = (val % 3 == 0) ? 3 : ((val % 2 == 0) ? 2 : 1);
            int width2 = ((val + 1) % 3 == 0) ? 3 : 1;
            svg.append(String.format("<rect x='%d' y='0' width='%d' height='45' fill='black'/>", x, width1));
            x += width1 + 2;
            svg.append(String.format("<rect x='%d' y='0' width='%d' height='45' fill='black'/>", x, width2));
            x += width2 + 2;
        }

        svg.append("</svg>");
        container.getElement().setProperty("innerHTML", svg.toString());
        return container;
    }

    @Subscribe("printButton")
    public void onPrintButtonClick(final ClickEvent<Button> event) {
        UI.getCurrent().getPage().executeJs("window.print();");
    }

    @Subscribe("closeButton")
    public void onCloseButtonClick(final ClickEvent<Button> event) {
        close(StandardOutcome.CLOSE);
    }
}
