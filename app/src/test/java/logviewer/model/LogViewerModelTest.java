package logviewer.model;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * LogViewerModel クラスのテストケース。
 */
public class LogViewerModelTest {
    private LogViewerModel model;

    @Before
    public void setUp() {
        model = new LogViewerModel();
    }

    /**
     * 通常系：初期状態の確認。
     */
    @Test
    public void testInitialState() {
        assertEquals(0, model.getBaseData().size());
        assertEquals(0, model.getTableData().size());
        assertEquals(-1, model.getSortColumnIndex());
        assertTrue(model.isSortAscending());
        assertEquals(0, model.getColumnCount());
        assertEquals("", model.getSingleFilterText());
        assertEquals("All", model.getSingleFilterColumn());
    }

    /**
     * 通常系：ベースデータに行を追加。
     */
    @Test
    public void testAddBaseDataRow() {
        LogRow row = new LogRow(new String[]{"data"}, 1);
        model.addBaseDataRow(row);
        
        assertEquals(1, model.getBaseData().size());
        assertSame(row, model.getBaseData().get(0));
    }

    /**
     * 通常系：複数行をベースデータに追加。
     */
    @Test
    public void testAddBaseDataRows() {
        List<LogRow> rows = Arrays.asList(
            new LogRow(new String[]{"row1"}, 1),
            new LogRow(new String[]{"row2"}, 2),
            new LogRow(new String[]{"row3"}, 3)
        );
        model.addBaseDataRows(rows);
        
        assertEquals(3, model.getBaseData().size());
    }

    /**
     * 通常系：テーブルデータを設定。
     */
    @Test
    public void testSetTableData() {
        List<LogRow> rows = Arrays.asList(
            new LogRow(new String[]{"row1"}, 1),
            new LogRow(new String[]{"row2"}, 2)
        );
        model.setTableData(rows);
        
        assertEquals(2, model.getTableData().size());
    }

    /**
     * 通常系：ソート設定。
     */
    @Test
    public void testSetSortConfig() {
        model.setSortConfig(1, false);
        
        assertEquals(1, model.getSortColumnIndex());
        assertFalse(model.isSortAscending());
    }

    /**
     * 通常系：単一フィルタ設定。
     */
    @Test
    public void testSetSingleFilter() {
        model.setSingleFilter("search text", "Column 0");
        
        assertEquals("search text", model.getSingleFilterText());
        assertEquals("Column 0", model.getSingleFilterColumn());
    }

    /**
     * 通常系：列数を設定。
     */
    @Test
    public void testSetColumnCount() {
        model.setColumnCount(10);
        assertEquals(10, model.getColumnCount());
    }

    /**
     * 通常系：操作開始時刻を設定。
     */
    @Test
    public void testSetOperationStartTime() {
        long nanoTime = System.nanoTime();
        model.setOperationStartTime(nanoTime);
        
        assertEquals(nanoTime, model.getOperationStartTime());
    }

    /**
     * 通常系：フィルタステータス更新スキップフラグ。
     */
    @Test
    public void testSetSkipFilterStatusUpdate() {
        assertFalse(model.isSkipFilterStatusUpdate());
        
        model.setSkipFilterStatusUpdate(true);
        assertTrue(model.isSkipFilterStatusUpdate());
        
        model.setSkipFilterStatusUpdate(false);
        assertFalse(model.isSkipFilterStatusUpdate());
    }

    /**
     * 通常系：ステータスメッセージ。
     */
    @Test
    public void testSetStatusMessage() {
        String message = "Processing...";
        model.setStatusMessage(message);
        
        assertEquals(message, model.getStatusMessage());
    }

    /**
     * 通常系：表示カラムインデックス。
     */
    @Test
    public void testSetVisibleColumnIndices() {
        List<Integer> indices = Arrays.asList(0, 2, 4);
        model.setVisibleColumnIndices(indices);
        
        assertEquals(3, model.getVisibleColumnIndices().size());
        assertTrue(model.getVisibleColumnIndices().contains(0));
        assertTrue(model.getVisibleColumnIndices().contains(2));
        assertTrue(model.getVisibleColumnIndices().contains(4));
    }

    /**
     * 通常系：すべてのデータをクリア。
     */
    @Test
    public void testClearAllData() {
        // データを追加
        model.addBaseDataRow(new LogRow(new String[]{"data"}, 1));
        model.setTableData(Arrays.asList(new LogRow(new String[]{"data"}, 1)));
        model.setColumnCount(5);
        model.setSortConfig(1, false);
        model.setSingleFilter("filter", "Column 0");
        
        // すべてをクリア
        model.clearAllData();
        
        assertEquals(0, model.getBaseData().size());
        assertEquals(0, model.getTableData().size());
        assertEquals(0, model.getColumnCount());
        assertEquals(-1, model.getSortColumnIndex());
        assertTrue(model.isSortAscending());
        assertEquals("", model.getSingleFilterText());
        assertEquals("All", model.getSingleFilterColumn());
    }

    /**
     * 境界値：null フィルタ設定。
     */
    @Test
    public void testSetSingleFilterNull() {
        model.setSingleFilter(null, null);
        
        assertEquals("", model.getSingleFilterText());
        assertEquals("All", model.getSingleFilterColumn());
    }

    /**
     * 通常系：ステータスメッセージプロパティ。
     */
    @Test
    public void testStatusMessageProperty() {
        assertNotNull(model.statusMessageProperty());
        
        model.setStatusMessage("Test message");
        assertEquals("Test message", model.statusMessageProperty().get());
    }

    /**
     * 通常系：複数回のデータ追加。
     */
    @Test
    public void testMultipleDataAdditions() {
        model.addBaseDataRow(new LogRow(new String[]{"row1"}, 1));
        assertEquals(1, model.getBaseData().size());
        
        model.addBaseDataRow(new LogRow(new String[]{"row2"}, 2));
        assertEquals(2, model.getBaseData().size());
        
        model.addBaseDataRows(Arrays.asList(
            new LogRow(new String[]{"row3"}, 3),
            new LogRow(new String[]{"row4"}, 4)
        ));
        assertEquals(4, model.getBaseData().size());
    }

    /**
     * 通常系：表示カラムインデックスの複数回更新。
     */
    @Test
    public void testVisibleColumnIndicesUpdate() {
        List<Integer> indices1 = Arrays.asList(0, 1, 2);
        model.setVisibleColumnIndices(indices1);
        assertEquals(3, model.getVisibleColumnIndices().size());
        
        List<Integer> indices2 = Arrays.asList(0, 3);
        model.setVisibleColumnIndices(indices2);
        assertEquals(2, model.getVisibleColumnIndices().size());
    }

    /**
     * 境界値：空の表示カラムインデックス。
     */
    @Test
    public void testEmptyVisibleColumnIndices() {
        List<Integer> emptyIndices = Arrays.asList();
        model.setVisibleColumnIndices(emptyIndices);
        
        assertEquals(0, model.getVisibleColumnIndices().size());
    }

    /**
     * 通常系：現在のファイル名を設定・取得。
     */
    @Test
    public void testSetAndGetCurrentFileName() {
        String fileName = "sample_log.tsv";
        model.setCurrentFileName(fileName);
        
        assertEquals(fileName, model.getCurrentFileName());
    }

    /**
     * 通常系：初期状態でのファイル名は空文字列。
     */
    @Test
    public void testInitialCurrentFileName() {
        assertEquals("", model.getCurrentFileName());
    }

    /**
     * 境界値：nullファイル名を設定した場合は空文字列になる。
     */
    @Test
    public void testSetCurrentFileNameNull() {
        model.setCurrentFileName(null);
        
        assertEquals("", model.getCurrentFileName());
    }

    /**
     * 通常系：ファイル名を複数回更新。
     */
    @Test
    public void testUpdateCurrentFileName() {
        model.setCurrentFileName("file1.tsv");
        assertEquals("file1.tsv", model.getCurrentFileName());
        
        model.setCurrentFileName("file2.tsv");
        assertEquals("file2.tsv", model.getCurrentFileName());
    }

    /**
     * 通常系：特殊文字を含むファイル名。
     */
    @Test
    public void testCurrentFileNameWithSpecialCharacters() {
        String fileName = "sample-log_2024.01.01.tsv";
        model.setCurrentFileName(fileName);
        
        assertEquals(fileName, model.getCurrentFileName());
    }
}
