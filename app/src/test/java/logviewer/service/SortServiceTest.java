package logviewer.service;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * SortService クラスのテストケース。
 */
public class SortServiceTest {
    private SortService sortService;

    @Before
    public void setUp() {
        sortService = new SortService();
    }

    /**
     * 通常系：行番号による昇順ソート。
     */
    @Test
    public void testBuildComparatorLineNumberAscending() {
        Comparator<LogRow> comparator = sortService.buildComparator(-1, true);
        
        LogRow row1 = new LogRow(new String[]{"data"}, 1);
        LogRow row2 = new LogRow(new String[]{"data"}, 2);
        LogRow row3 = new LogRow(new String[]{"data"}, 3);
        
        assertTrue(comparator.compare(row1, row2) < 0);
        assertTrue(comparator.compare(row2, row3) < 0);
        assertTrue(comparator.compare(row3, row1) > 0);
    }

    /**
     * 通常系：行番号による降順ソート。
     */
    @Test
    public void testBuildComparatorLineNumberDescending() {
        Comparator<LogRow> comparator = sortService.buildComparator(-1, false);
        
        LogRow row1 = new LogRow(new String[]{"data"}, 1);
        LogRow row2 = new LogRow(new String[]{"data"}, 2);
        
        assertTrue(comparator.compare(row1, row2) > 0);
        assertTrue(comparator.compare(row2, row1) < 0);
    }

    /**
     * 通常系：文字列カラムによる昇順ソート。
     */
    @Test
    public void testBuildComparatorColumnAscending() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        LogRow rowA = new LogRow(new String[]{"apple"}, 1);
        LogRow rowB = new LogRow(new String[]{"banana"}, 2);
        LogRow rowC = new LogRow(new String[]{"cherry"}, 3);
        
        assertTrue(comparator.compare(rowA, rowB) < 0);
        assertTrue(comparator.compare(rowB, rowC) < 0);
    }

    /**
     * 通常系：文字列カラムによる降順ソート。
     */
    @Test
    public void testBuildComparatorColumnDescending() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, false);
        
        LogRow rowA = new LogRow(new String[]{"apple"}, 1);
        LogRow rowB = new LogRow(new String[]{"banana"}, 2);
        
        assertTrue(comparator.compare(rowA, rowB) > 0);
    }

    /**
     * 通常系：大文字小文字を区別しないソート。
     */
    @Test
    public void testBuildComparatorCaseInsensitive() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        LogRow rowA = new LogRow(new String[]{"Apple"}, 1);
        LogRow rowB = new LogRow(new String[]{"banana"}, 2);
        
        assertTrue(comparator.compare(rowA, rowB) < 0);
    }

    /**
     * 境界値：同じ値のソート比較は 0。
     */
    @Test
    public void testBuildComparatorEqual() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        LogRow row1 = new LogRow(new String[]{"same"}, 1);
        LogRow row2 = new LogRow(new String[]{"same"}, 2);
        
        assertEquals(0, comparator.compare(row1, row2));
    }

    /**
     * 通常系：複数カラムのソート（異なるカラムインデックス）。
     */
    @Test
    public void testBuildComparatorMultipleColumns() {
        Comparator<LogRow> comparator0 = sortService.buildComparator(0, true);
        Comparator<LogRow> comparator1 = sortService.buildComparator(1, true);
        
        LogRow row = new LogRow(new String[]{"col0", "col1"}, 1);
        
        // 同じ行に対して異なるカラムをソートしても安定性がある
        assertEquals(0, comparator0.compare(row, row));
        assertEquals(0, comparator1.compare(row, row));
    }

    /**
     * 境界値：数値文字列のソート（字句順）。
     */
    @Test
    public void testBuildComparatorNumericAsString() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        LogRow row1 = new LogRow(new String[]{"10"}, 1);
        LogRow row2 = new LogRow(new String[]{"2"}, 2);
        
        // 字句順なので "10" < "2"
        assertTrue(comparator.compare(row1, row2) < 0);
    }

    /**
     * 通常系：空文字列カラムのソート。
     */
    @Test
    public void testBuildComparatorEmptyString() {
        Comparator<LogRow> comparator = sortService.buildComparator(0, true);
        
        LogRow rowEmpty = new LogRow(new String[]{""}, 1);
        LogRow rowA = new LogRow(new String[]{"a"}, 2);
        
        assertTrue(comparator.compare(rowEmpty, rowA) < 0);
    }
}
