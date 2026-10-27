package com.company.librarymanagementsystem.service;

import com.company.librarymanagementsystem.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class OpacService {

    private final DataManager dataManager;

    public OpacService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public record OpacStats(
            long totalTitles,
            long totalCopies,
            long availableCopies,
            long borrowedCopies) {
    }

    public record BookAvailability(
            int totalCopies,
            int availableCopies,
            int borrowedCopies,
            int reservedCount,
            int pendingReservations,
            String locationSummary,
            boolean canReserve) {
    }

    public record CopyInfo(
            String barcode,
            String locationName,
            String condition,
            String status,
            LocalDate dueDate) {
    }

    private volatile OpacStats cachedStats;
    private volatile long statsCachedTime = 0;
    private static final long STATS_CACHE_TTL = 120_000; // 2 phút

    /**
     * Thống kê tổng quan cho OPAC (được cache 2 phút để không query liên tục)
     */
    public OpacStats getStats() {
        long now = System.currentTimeMillis();
        if (cachedStats != null && (now - statsCachedTime < STATS_CACHE_TTL)) {
            return cachedStats;
        }

        Long titles = dataManager.loadValue("select count(b) from Book b where b.status = 'ACTIVE'", Long.class).one();
        Long copies = dataManager.loadValue("select count(bc) from BookCopy bc", Long.class).one();
        Long available = dataManager
                .loadValue("select count(bc) from BookCopy bc where bc.status = 'AVAILABLE'", Long.class).one();
        Long borrowed = dataManager
                .loadValue("select count(bc) from BookCopy bc where bc.status = 'BORROWED'", Long.class).one();

        cachedStats = new OpacStats(
                titles != null ? titles : 0L,
                copies != null ? copies : 0L,
                available != null ? available : 0L,
                borrowed != null ? borrowed : 0L);
        statsCachedTime = now;
        return cachedStats;
    }

    public void invalidateStatsCache() {
        statsCachedTime = 0;
    }

    /**
     * Lấy tình trạng bản sao và vị trí kệ trực tiếp từ đối tượng Book đã nạp sẵn fetchPlan (Không tốn query DB)
     */
    public BookAvailability getBookAvailability(Book book) {
        if (book == null) {
            return new BookAvailability(0, 0, 0, 0, 0, "Chưa xếp kệ", false);
        }
        List<BookCopy> copies = book.getCopies();
        if (copies == null) {
            return getBookAvailability(book.getId());
        }

        int total = copies.size();
        int available = 0;
        int borrowed = 0;
        int reserved = 0;
        Set<String> locations = new LinkedHashSet<>();

        for (BookCopy c : copies) {
            String st = c.getStatus();
            if (BookCopyStatus.AVAILABLE.getId().equalsIgnoreCase(st)) {
                available++;
            } else if (BookCopyStatus.BORROWED.getId().equalsIgnoreCase(st)) {
                borrowed++;
            } else if (BookCopyStatus.RESERVED.getId().equalsIgnoreCase(st)) {
                reserved++;
            }
            if (c.getLocation() != null && c.getLocation().getName() != null) {
                String loc = c.getLocation().getName().trim();
                if (!loc.isEmpty()) {
                    locations.add(loc);
                }
            }
        }

        String locSummary = locations.isEmpty()
                ? (total == 0 ? "Chưa có bản sao trong kho" : "Kho tổng / Chưa xếp kệ")
                : String.join(", ", locations);
        boolean canReserve = (total > 0 && available == 0);

        return new BookAvailability(total, available, borrowed, reserved, 0, locSummary, canReserve);
    }

    /**
     * Lấy tình trạng bản sao và vị trí kệ của một đầu sách
     */
    public BookAvailability getBookAvailability(Long bookId) {
        if (bookId == null) {
            return new BookAvailability(0, 0, 0, 0, 0, "Chưa xếp kệ", false);
        }

        List<BookCopy> copies = dataManager.load(BookCopy.class)
                .query("select bc from BookCopy bc where bc.book.id = :bId")
                .parameter("bId", bookId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("location", FetchPlan.BASE))
                .list();

        int total = copies.size();
        int available = 0;
        int borrowed = 0;
        int reserved = 0;
        Set<String> locations = new LinkedHashSet<>();

        for (BookCopy c : copies) {
            String st = c.getStatus();
            if (BookCopyStatus.AVAILABLE.getId().equalsIgnoreCase(st)) {
                available++;
            } else if (BookCopyStatus.BORROWED.getId().equalsIgnoreCase(st)) {
                borrowed++;
            } else if (BookCopyStatus.RESERVED.getId().equalsIgnoreCase(st)) {
                reserved++;
            }
            if (c.getLocation() != null && c.getLocation().getName() != null) {
                String loc = c.getLocation().getName().trim();
                if (!loc.isEmpty()) {
                    locations.add(loc);
                }
            }
        }

        Long pendingRes = dataManager.loadValue(
                "select count(r) from Reservation r where r.book.id = :bId and r.status = 'PENDING'", Long.class)
                .parameter("bId", bookId)
                .one();
        int pending = pendingRes != null ? pendingRes.intValue() : 0;

        String locSummary = locations.isEmpty()
                ? (total == 0 ? "Chưa có bản sao trong kho" : "Kho tổng / Chưa xếp kệ")
                : String.join(", ", locations);
        boolean canReserve = (total > 0 && available == 0);

        return new BookAvailability(total, available, borrowed, reserved, pending, locSummary, canReserve);
    }

    /**
     * Lấy danh sách từng bản sao cụ thể của đầu sách (kèm mã vạch, vị trí, hạn trả
     * nếu đang mượn)
     */
    public List<CopyInfo> getCopiesForBook(Long bookId) {
        if (bookId == null)
            return Collections.emptyList();

        List<BookCopy> copies = dataManager.load(BookCopy.class)
                .query("select bc from BookCopy bc where bc.book.id = :bId order by bc.barcode asc")
                .parameter("bId", bookId)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("location", FetchPlan.BASE))
                .list();

        List<CopyInfo> result = new ArrayList<>();
        for (BookCopy c : copies) {
            String loc = (c.getLocation() != null && c.getLocation().getName() != null)
                    ? c.getLocation().getName()
                    : "Chưa phân vị trí";

            LocalDate due = null;
            if (BookCopyStatus.BORROWED.getId().equalsIgnoreCase(c.getStatus())) {
                List<LocalDate> dueDates = dataManager.loadValue(
                        "select li.dueDate from LoanItem li where li.bookCopy = :copy and li.status = 'BORROWED'",
                        LocalDate.class)
                        .parameter("copy", c)
                        .list();
                if (!dueDates.isEmpty()) {
                    due = dueDates.get(0);
                }
            }

            result.add(new CopyInfo(
                    c.getBarcode(),
                    loc,
                    c.getCondition(),
                    c.getStatus(),
                    due));
        }
        return result;
    }

    /**
     * Sách mới nhập
     */
    public List<Book> getRecentBooks(int limit) {
        return dataManager.load(Book.class)
                .query("select b from Book b where b.status = 'ACTIVE' order by b.id desc")
                .maxResults(limit)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("authors", FetchPlan.BASE)
                        .add("categories", FetchPlan.BASE)
                        .add("publisher", FetchPlan.BASE)
                        .add("copies", copyBuilder -> copyBuilder.addFetchPlan(FetchPlan.BASE).add("location", FetchPlan.BASE)))
                .list();
    }

    /**
     * Sách được mượn nhiều nhất
     */
    public List<Book> getPopularBooks(int limit) {
        List<Long> ids = dataManager.loadValue(
                "select bc.book.id from LoanItem li join li.bookCopy bc group by bc.book.id order by count(li) desc",
                Long.class)
                .maxResults(limit)
                .list();

        if (ids.isEmpty()) {
            return getRecentBooks(limit);
        }

        List<Book> books = dataManager.load(Book.class)
                .query("select b from Book b where b.id in :ids")
                .parameter("ids", ids)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("authors", FetchPlan.BASE)
                        .add("categories", FetchPlan.BASE)
                        .add("publisher", FetchPlan.BASE)
                        .add("copies", copyBuilder -> copyBuilder.addFetchPlan(FetchPlan.BASE).add("location", FetchPlan.BASE)))
                .list();

        Map<Long, Book> map = new HashMap<>();
        books.forEach(b -> map.put(b.getId(), b));
        List<Book> sorted = new ArrayList<>();
        for (Long id : ids) {
            if (map.containsKey(id))
                sorted.add(map.get(id));
        }

        if (sorted.size() < limit) {
            List<Book> recents = getRecentBooks(limit);
            for (Book r : recents) {
                if (!map.containsKey(r.getId()) && sorted.size() < limit) {
                    sorted.add(r);
                }
            }
        }
        return sorted;
    }
}
