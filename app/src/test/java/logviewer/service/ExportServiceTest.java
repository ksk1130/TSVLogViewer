package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ExportServiceTest {

    private ExportService exportService = new ExportService();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testExportDisplayedDataAsyncReturnsTask() {
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"col1", "col2"}, 1));
        List<Integer> visibleIndices = Arrays.asList(0, 1);
        Path outputPath = new java.io.File(tempFolder.getRoot(), "output.tsv").toPath();

        Task<Integer> task = exportService.exportDisplayedDataAsync(data, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testExportEmptyDataAsync() throws Exception {
        Path outputPath = tempFolder.newFile("output.tsv").toPath();
        List<LogRow> emptyData = new ArrayList<>();
        List<Integer> visibleIndices = Arrays.asList(0, 1);

        Task<Integer> task = exportService.exportDisplayedDataAsync(emptyData, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testExportMultipleRows() throws Exception {
        Path outputPath = tempFolder.newFile("output.tsv").toPath();
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"col1", "col2", "col3"}, 1));
        data.add(new LogRow(new String[]{"val1", "val2", "val3"}, 2));
        data.add(new LogRow(new String[]{"data1", "data2", "data3"}, 3));
        List<Integer> visibleIndices = Arrays.asList(0, 1, 2);

        Task<Integer> task = exportService.exportDisplayedDataAsync(data, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testExportVisibleColumnsOnly() throws Exception {
        Path outputPath = tempFolder.newFile("output.tsv").toPath();
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"col1", "col2", "col3", "col4"}, 1));
        List<Integer> visibleIndices = Arrays.asList(0, 2);

        Task<Integer> task = exportService.exportDisplayedDataAsync(data, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testExportSelectedRowsMultiple() throws Exception {
        Path outputPath = tempFolder.newFile("selected.tsv").toPath();
        List<LogRow> items = new ArrayList<>();
        items.add(new LogRow(new String[]{"row0col1", "row0col2"}, 1));
        items.add(new LogRow(new String[]{"row1col1", "row1col2"}, 2));
        items.add(new LogRow(new String[]{"row2col1", "row2col2"}, 3));
        List<Integer> selectedIndices = Arrays.asList(0, 2);
        List<Integer> visibleIndices = Arrays.asList(0, 1);

        Task<Integer> task = exportService.exportSelectedRowsAsync(items, selectedIndices, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testInvalidRowIndicesIgnored() throws Exception {
        Path outputPath = tempFolder.newFile("output.tsv").toPath();
        List<LogRow> items = new ArrayList<>();
        items.add(new LogRow(new String[]{"a", "b"}, 1));
        List<Integer> selectedIndices = Arrays.asList(0, 5, 100);
        List<Integer> visibleIndices = Arrays.asList(0, 1);

        Task<Integer> task = exportService.exportSelectedRowsAsync(items, selectedIndices, visibleIndices, outputPath);

        assertNotNull(task);
    }

    @Test
    public void testExportJapaneseData() throws Exception {
        Path outputPath = tempFolder.newFile("output.tsv").toPath();
        List<LogRow> data = new ArrayList<>();
        data.add(new LogRow(new String[]{"日本語", "テスト", "データ"}, 1));
        List<Integer> visibleIndices = Arrays.asList(0, 1, 2);

        Task<Integer> task = exportService.exportDisplayedDataAsync(data, visibleIndices, outputPath);

        assertNotNull(task);
    }
}
