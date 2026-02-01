package logviewer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoadResultTest {

    private LoadResult loadResult;

    @Before
    public void setUp() {
        loadResult = new LoadResult(10, false);
    }

    @Test
    public void testConstructorColumns() {
        LoadResult result = new LoadResult(5, true);
        assertEquals(5, result.columns);
    }

    @Test
    public void testConstructorTruncated() {
        LoadResult result = new LoadResult(10, true);
        assertTrue(result.truncated);
    }

    @Test
    public void testTruncatedFalse() {
        LoadResult result = new LoadResult(10, false);
        assertFalse(result.truncated);
    }

    @Test
    public void testColumnsZero() {
        LoadResult result = new LoadResult(0, false);
        assertEquals(0, result.columns);
    }

    @Test
    public void testColumnsNegative() {
        LoadResult result = new LoadResult(-1, false);
        assertEquals(-1, result.columns);
    }

    @Test
    public void testColumnsLarge() {
        LoadResult result = new LoadResult(10000, false);
        assertEquals(10000, result.columns);
    }

    @Test
    public void testFieldAccess() {
        LoadResult result = new LoadResult(15, true);
        assertNotNull(result);
        assertEquals(15, result.columns);
        assertTrue(result.truncated);
    }

    @Test
    public void testIndependentInstances() {
        LoadResult result1 = new LoadResult(5, true);
        LoadResult result2 = new LoadResult(10, false);
        
        assertEquals(5, result1.columns);
        assertEquals(10, result2.columns);
        assertTrue(result1.truncated);
        assertFalse(result2.truncated);
    }
}
