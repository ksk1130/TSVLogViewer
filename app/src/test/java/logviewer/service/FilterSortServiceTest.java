package logviewer.service;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * FilterSortService クラスのテストケース。
 * 注：Task の実行には JavaFX 初期化が必要なため、ここではメソッドの存在と戻り値型のみテストします。
 */
public class FilterSortServiceTest {
    private FilterSortService filterSortService;
    private FilterService filterService;
    private SortService sortService;

    @Before
    public void setUp() {
        filterSortService = new FilterSortService();
        filterService = new FilterService();
        sortService = new SortService();
    }

    /**
     * 通常系：filterAndSortAsync メソッドが Task を返す。
     */
    @Test
    public void testFilterAndSortAsyncReturnsTask() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"test"}, 1));
        
        Predicate<LogRow> predicate = r -> true;
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
        assertTrue(task instanceof javafx.concurrent.Task);
    }

    /**
     * 通常系：空のデータで Task が返される。
     */
    @Test
    public void testFilterAndSortAsyncEmptyData() {
        List<LogRow> data = new ArrayList<>();
        
        Predicate<LogRow> predicate = r -> true;
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
    }

    /**
     * 通常系：複数データで Task が返される。
     */
    @Test
    public void testFilterAndSortAsyncMultipleRows() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"z"}, 1));
        data.add(new LogRow(new String[]{"a"}, 2));
        data.add(new LogRow(new String[]{"m"}, 3));
        
        Predicate<LogRow> predicate = r -> true;
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
    }

    /**
     * 通常系：フィルタが機能する Predicate での Task。
     */
    @Test
    public void testFilterAndSortAsyncWithPredicate() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"apple"}, 1));
        data.add(new LogRow(new String[]{"banana"}, 2));
        
        Predicate<LogRow> predicate = filterService.buildPredicate("apple", "All", -1);
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
    }

    /**
     * 通常系：降順 Comparator での Task。
     */
    @Test
    public void testFilterAndSortAsyncDescending() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"apple"}, 1));
        data.add(new LogRow(new String[]{"banana"}, 2));
        
        Predicate<LogRow> predicate = r -> true;
        Comparator<LogRow> comparator = sortService.buildComparator(0, false);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
    }

    /**
     * 通常系：行番号でのソート Task。
     */
    @Test
    public void testFilterAndSortAsyncByLineNumber() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"row1"}, 100));
        data.add(new LogRow(new String[]{"row2"}, 50));
        
        Predicate<LogRow> predicate = r -> true;
        Comparator<LogRow> comparator = sortService.buildComparator(-1, true);
        
        javafx.concurrent.Task<List<LogRow>> task = filterSortService.filterAndSortAsync(data, predicate, comparator);
        
        assertNotNull(task);
    }
}
