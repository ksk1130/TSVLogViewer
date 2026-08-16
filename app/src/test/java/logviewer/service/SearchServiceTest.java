package logviewer.service;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SearchService クラスのテストケース。
 */
public class SearchServiceTest {
    private SearchService searchService;

    @Before
    public void setUp() {
        searchService = new SearchService();
    }

    /**
     * 通常系：All列相当（対象列未指定）で部分一致検索できること。
     */
    @Test
    public void testFindMatchesAllColumnsCaseInsensitive() {
        List<LogRow> rows = List.of(
            new LogRow(new String[]{"INFO", "Boot Completed"}, 1),
            new LogRow(new String[]{"ERROR", "Disk Full"}, 2),
            new LogRow(new String[]{"WARN", "retry"}, 3)
        );

        List<SearchService.CellMatch> matches = searchService.findMatches(rows, "error", List.of());

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).rowIndex());
        assertEquals(0, matches.get(0).columnIndex());
    }

    /**
     * 通常系：対象カラムを指定した検索ができること。
     */
    @Test
    public void testFindMatchesSpecificColumnsOnly() {
        List<LogRow> rows = List.of(
            new LogRow(new String[]{"error", "ok"}, 1),
            new LogRow(new String[]{"ok", "error"}, 2)
        );

        List<SearchService.CellMatch> matches = searchService.findMatches(rows, "error", List.of(1));

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).rowIndex());
        assertEquals(1, matches.get(0).columnIndex());
    }

    /**
     * 通常系：無効な対象カラムは無視されること。
     */
    @Test
    public void testFindMatchesIgnoresInvalidColumnIndices() {
        List<LogRow> rows = List.of(
            new LogRow(new String[]{"alpha", "beta"}, 1)
        );

        List<SearchService.CellMatch> matches = searchService.findMatches(rows, "alpha", List.of(-1, 0, 99));

        assertEquals(1, matches.size());
        assertEquals(0, matches.get(0).columnIndex());
    }

    /**
     * 通常系：空文字検索は一致なしを返すこと。
     */
    @Test
    public void testFindMatchesBlankQueryReturnsEmpty() {
        List<SearchService.CellMatch> matches = searchService.findMatches(
            List.of(new LogRow(new String[]{"x"}, 1)),
            " ",
            List.of(0)
        );

        assertTrue(matches.isEmpty());
    }

    /**
     * 通常系：次へ移動は末尾で先頭に循環すること。
     */
    @Test
    public void testNextIndexWrapAround() {
        assertEquals(0, searchService.nextIndex(-1, 3));
        assertEquals(1, searchService.nextIndex(0, 3));
        assertEquals(0, searchService.nextIndex(2, 3));
    }

    /**
     * 通常系：前へ移動は先頭で末尾に循環すること。
     */
    @Test
    public void testPreviousIndexWrapAround() {
        assertEquals(2, searchService.previousIndex(0, 3));
        assertEquals(1, searchService.previousIndex(2, 3));
        assertEquals(2, searchService.previousIndex(-1, 3));
    }
}