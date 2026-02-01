package logviewer.service;

import logviewer.LogRow;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * FilterService クラスのテストケース。
 */
public class FilterServiceTest {
    private FilterService filterService;

    @Before
    public void setUp() {
        filterService = new FilterService();
    }

    /**
     * 通常系：フィルタなし（空文字列）。
     */
    @Test
    public void testBuildPredicateNoFilter() {
        Predicate<LogRow> predicate = filterService.buildPredicate("", "All", -1);
        
        LogRow row = new LogRow(new String[]{"test"}, 1);
        assertTrue(predicate.test(row));
    }

    /**
     * 通常系：フィルタなし（null）。
     */
    @Test
    public void testBuildPredicateNullFilter() {
        Predicate<LogRow> predicate = filterService.buildPredicate(null, "All", -1);
        
        LogRow row = new LogRow(new String[]{"test"}, 1);
        assertTrue(predicate.test(row));
    }

    /**
     * 通常系：単純な部分文字列検索（All カラム）。
     */
    @Test
    public void testBuildPredicateSubstringAll() {
        Predicate<LogRow> predicate = filterService.buildPredicate("test", "All", -1);
        
        assertTrue(predicate.test(new LogRow(new String[]{"this is a test"}, 1)));
        assertTrue(predicate.test(new LogRow(new String[]{"TEST"}, 1)));
        assertFalse(predicate.test(new LogRow(new String[]{"nope"}, 1)));
    }

    /**
     * 通常系：単純な部分文字列検索（特定カラム）。
     */
    @Test
    public void testBuildPredicateSubstringSpecificColumn() {
        Predicate<LogRow> predicate = filterService.buildPredicate("error", "Column 1", 1);
        
        assertTrue(predicate.test(new LogRow(new String[]{"info", "error occurred"}, 1)));
        assertFalse(predicate.test(new LogRow(new String[]{"error occurred", "info"}, 1)));
        assertFalse(predicate.test(new LogRow(new String[]{"nope"}, 1)));
    }

    /**
     * 通常系：正規表現フィルタ（All カラム）。
     */
    @Test
    public void testBuildPredicateRegexAll() {
        Predicate<LogRow> predicate = filterService.buildPredicate("/^test.*$/", "All", -1);
        
        assertTrue(predicate.test(new LogRow(new String[]{"test123"}, 1)));
        assertTrue(predicate.test(new LogRow(new String[]{"other", "testing"}, 1)));
        assertFalse(predicate.test(new LogRow(new String[]{"notest"}, 1)));
    }

    /**
     * 通常系：正規表現フィルタ（特定カラム）。
     */
    @Test
    public void testBuildPredicateRegexSpecificColumn() {
        Predicate<LogRow> predicate = filterService.buildPredicate("/\\d+/", "Column 0", 0);
        
        assertTrue(predicate.test(new LogRow(new String[]{"123", "text"}, 1)));
        assertFalse(predicate.test(new LogRow(new String[]{"text", "123"}, 1)));
    }

    /**
     * 異常系：無効な正規表現はフィルタなしとして扱う。
     */
    @Test
    public void testBuildPredicateInvalidRegex() {
        Predicate<LogRow> predicate = filterService.buildPredicate("/[invalid(/", "All", -1);
        
        // 無効な正規表現は素通り（すべて true）
        assertTrue(predicate.test(new LogRow(new String[]{"anything"}, 1)));
        assertTrue(predicate.test(new LogRow(new String[]{"whatever"}, 1)));
    }

    /**
     * 通常系：大文字小文字を区別しない検索。
     */
    @Test
    public void testBuildPredicateCaseInsensitive() {
        Predicate<LogRow> predicate = filterService.buildPredicate("ERROR", "All", -1);
        
        assertTrue(predicate.test(new LogRow(new String[]{"error occurred"}, 1)));
        assertTrue(predicate.test(new LogRow(new String[]{"Error"}, 1)));
        assertTrue(predicate.test(new LogRow(new String[]{"ERROR"}, 1)));
    }

    /**
     * 通常系：複数 Predicate を AND 結合。
     */
    @Test
    public void testCombinePredicates() {
        List<Predicate<LogRow>> predicates = new ArrayList<>();
        predicates.add(filterService.buildPredicate("error", "All", -1));
        predicates.add(filterService.buildPredicate("2024", "All", -1));
        
        Predicate<LogRow> combined = filterService.combinePredicates(predicates);
        
        assertTrue(combined.test(new LogRow(new String[]{"error 2024"}, 1)));
        assertFalse(combined.test(new LogRow(new String[]{"error only"}, 1)));
        assertFalse(combined.test(new LogRow(new String[]{"2024 only"}, 1)));
    }

    /**
     * 通常系：空の Predicate リストは常に true。
     */
    @Test
    public void testCombinePredicatesEmpty() {
        List<Predicate<LogRow>> predicates = new ArrayList<>();
        Predicate<LogRow> combined = filterService.combinePredicates(predicates);
        
        assertTrue(combined.test(new LogRow(new String[]{"anything"}, 1)));
    }

    /**
     * 通常系：単一の Predicate 結合。
     */
    @Test
    public void testCombinePredicatesSingle() {
        List<Predicate<LogRow>> predicates = new ArrayList<>();
        predicates.add(filterService.buildPredicate("test", "All", -1));
        
        Predicate<LogRow> combined = filterService.combinePredicates(predicates);
        
        assertTrue(combined.test(new LogRow(new String[]{"test"}, 1)));
        assertFalse(combined.test(new LogRow(new String[]{"nope"}, 1)));
    }

    /**
     * 境界値：複数 Predicate で 1 つが false なら全体も false。
     */
    @Test
    public void testCombinePredicatesShortCircuit() {
        List<Predicate<LogRow>> predicates = new ArrayList<>();
        predicates.add(r -> true);
        predicates.add(r -> false);
        predicates.add(r -> true);
        
        Predicate<LogRow> combined = filterService.combinePredicates(predicates);
        assertFalse(combined.test(new LogRow(new String[]{"any"}, 1)));
    }

    /**
     * 通常系：カラムインデックス無効時は false を返す。
     */
    @Test
    public void testBuildPredicateInvalidColumnIndex() {
        Predicate<LogRow> predicate = filterService.buildPredicate("test", "Column 10", 10);
        
        assertFalse(predicate.test(new LogRow(new String[]{"test"}, 1)));
    }

    /**
     * 通常系：null カラム名は "All" として扱われる。
     */
    @Test
    public void testBuildPredicateNullColumnName() {
        Predicate<LogRow> predicate = filterService.buildPredicate("test", null, -1);
        
        assertTrue(predicate.test(new LogRow(new String[]{"test"}, 1)));
    }
}
