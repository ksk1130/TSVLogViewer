package logviewer.service;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * NavigationService クラスのテストケース。
 */
public class NavigationServiceTest {
    private NavigationService navigationService;

    @Before
    public void setUp() {
        navigationService = new NavigationService();
    }

    /**
     * 通常系：存在する行番号を検索。
     */
    @Test
    public void testFindRowIndexByLineNumberFound() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"row1"}, 1));
        data.add(new LogRow(new String[]{"row2"}, 2));
        data.add(new LogRow(new String[]{"row3"}, 3));
        
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, 1));
        assertEquals(1, navigationService.findRowIndexByLineNumber(data, 2));
        assertEquals(2, navigationService.findRowIndexByLineNumber(data, 3));
    }

    /**
     * 通常系：存在しない行番号は -1 を返す。
     */
    @Test
    public void testFindRowIndexByLineNumberNotFound() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"row1"}, 1));
        data.add(new LogRow(new String[]{"row2"}, 2));
        
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, 99));
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, 0));
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, -1));
    }

    /**
     * 境界値：空のリスト。
     */
    @Test
    public void testFindRowIndexByLineNumberEmptyList() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, 1));
    }

    /**
     * 通常系：最初の行を検索。
     */
    @Test
    public void testFindRowIndexByLineNumberFirst() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"first"}, 100));
        data.add(new LogRow(new String[]{"second"}, 101));
        
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, 100));
    }

    /**
     * 通常系：最後の行を検索。
     */
    @Test
    public void testFindRowIndexByLineNumberLast() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"first"}, 1));
        data.add(new LogRow(new String[]{"second"}, 2));
        data.add(new LogRow(new String[]{"last"}, 1000));
        
        assertEquals(2, navigationService.findRowIndexByLineNumber(data, 1000));
    }

    /**
     * 通常系：連続していない行番号のリスト。
     */
    @Test
    public void testFindRowIndexByLineNumberNonSequential() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"row1"}, 10));
        data.add(new LogRow(new String[]{"row2"}, 50));
        data.add(new LogRow(new String[]{"row3"}, 100));
        
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, 10));
        assertEquals(1, navigationService.findRowIndexByLineNumber(data, 50));
        assertEquals(2, navigationService.findRowIndexByLineNumber(data, 100));
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, 30));
    }

    /**
     * 通常系：重複した行番号（最初のマッチを返す）。
     */
    @Test
    public void testFindRowIndexByLineNumberDuplicate() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"first occurrence"}, 1));
        data.add(new LogRow(new String[]{"second occurrence"}, 1));
        
        // 最初にマッチしたインデックスを返す
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, 1));
    }

    /**
     * 境界値：非常に大きい行番号。
     */
    @Test
    public void testFindRowIndexByLineNumberLargeNumber() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"row"}, Integer.MAX_VALUE));
        
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, Integer.MAX_VALUE));
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, Integer.MAX_VALUE - 1));
    }

    /**
     * 通常系：単一行のリスト。
     */
    @Test
    public void testFindRowIndexByLineNumberSingleRow() {
        java.util.List<LogRow> data = new java.util.ArrayList<>();
        data.add(new LogRow(new String[]{"only"}, 42));
        
        assertEquals(0, navigationService.findRowIndexByLineNumber(data, 42));
        assertEquals(-1, navigationService.findRowIndexByLineNumber(data, 41));
    }
}
