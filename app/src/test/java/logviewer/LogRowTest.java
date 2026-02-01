package logviewer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * LogRow クラスのテストケース。
 */
public class LogRowTest {

    /**
     * 正常系：通常のフィールド配列から LogRow を生成。
     */
    @Test
    public void testLogRowCreation() {
        String[] parts = {"field1", "field2", "field3"};
        LogRow row = new LogRow(parts, 42);
        
        assertEquals(3, row.fieldCount());
        assertEquals(42, row.getLineNumber());
        assertEquals("field1", row.getField(0));
        assertEquals("field2", row.getField(1));
        assertEquals("field3", row.getField(2));
    }

    /**
     * 境界値：インデックス範囲外アクセスは空文字を返す。
     */
    @Test
    public void testGetFieldOutOfBounds() {
        String[] parts = {"field1", "field2"};
        LogRow row = new LogRow(parts, 1);
        
        // 範囲外の正の値
        assertEquals("", row.getField(10));
        assertEquals("", row.getField(100));
        
        // 負のインデックス
        assertEquals("", row.getField(-1));
        assertEquals("", row.getField(-100));
    }

    /**
     * 境界値：空のフィールド配列。
     */
    @Test
    public void testEmptyFields() {
        String[] parts = {};
        LogRow row = new LogRow(parts, 1);
        
        assertEquals(0, row.fieldCount());
        assertEquals("", row.getField(0));
    }

    /**
     * 境界値：null を含むフィールド。
     */
    @Test
    public void testNullField() {
        String[] parts = {"field1", null, "field3"};
        LogRow row = new LogRow(parts, 1);
        
        assertEquals(3, row.fieldCount());
        assertEquals("field1", row.getField(0));
        assertNull(row.getField(1));
        assertEquals("field3", row.getField(2));
    }

    /**
     * 通常系：行番号の一貫性。
     */
    @Test
    public void testLineNumberConsistency() {
        String[] parts = {"data"};
        int lineNum = 12345;
        LogRow row = new LogRow(parts, lineNum);
        
        assertEquals(lineNum, row.getLineNumber());
    }

    /**
     * 通常系：複数行のフィールド値が独立している。
     */
    @Test
    public void testMultipleRowsIndependence() {
        LogRow row1 = new LogRow(new String[]{"a", "b"}, 1);
        LogRow row2 = new LogRow(new String[]{"x", "y"}, 2);
        
        assertEquals("a", row1.getField(0));
        assertEquals("x", row2.getField(0));
        assertEquals(1, row1.getLineNumber());
        assertEquals(2, row2.getLineNumber());
    }
}
