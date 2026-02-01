package logviewer.service;

import logviewer.LogRow;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileLoadResultTest {

    @Test
    public void testConstructor() {
        List<LogRow> rows = new ArrayList<>();
        rows.add(new LogRow(new String[]{"col1", "col2"}, 1));
        
        FileLoadResult result = new FileLoadResult(rows, 2, false);
        
        assertEquals(rows, result.rows);
        assertEquals(2, result.columns);
        assertFalse(result.truncated);
    }

    @Test
    public void testConstructorEmptyRows() {
        List<LogRow> emptyRows = new ArrayList<>();
        
        FileLoadResult result = new FileLoadResult(emptyRows, 0, false);
        
        assertEquals(0, result.rows.size());
        assertEquals(0, result.columns);
        assertFalse(result.truncated);
    }

    @Test
    public void testConstructorTruncated() {
        List<LogRow> rows = new ArrayList<>();
        FileLoadResult result = new FileLoadResult(rows, 1, true);
        
        assertTrue(result.truncated);
    }

    @Test
    public void testMultipleRows() {
        List<LogRow> rows = new ArrayList<>();
        rows.add(new LogRow(new String[]{"a", "b", "c"}, 1));
        rows.add(new LogRow(new String[]{"d", "e", "f"}, 2));
        rows.add(new LogRow(new String[]{"g", "h", "i"}, 3));
        
        FileLoadResult result = new FileLoadResult(rows, 3, false);
        
        assertEquals(3, result.rows.size());
        assertEquals(3, result.columns);
    }

    @Test
    public void testLargeColumnCount() {
        List<LogRow> rows = new ArrayList<>();
        FileLoadResult result = new FileLoadResult(rows, 1000, false);
        
        assertEquals(1000, result.columns);
    }

    @Test
    public void testConstructorNullRows() {
        FileLoadResult result = new FileLoadResult(null, 1, false);
        
        assertNull(result.rows);
        assertEquals(1, result.columns);
    }

    @Test
    public void testFieldsAreFinal() {
        List<LogRow> rows = new ArrayList<>();
        FileLoadResult result = new FileLoadResult(rows, 2, false);
        
        assertNotNull(result.rows);
        assertEquals(2, result.columns);
        assertFalse(result.truncated);
    }
}
