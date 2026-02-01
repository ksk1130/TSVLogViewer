package logviewer.controller;

import logviewer.model.LogViewerModel;
import logviewer.service.FileIOService;
import logviewer.service.FilterService;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * MainController クラスのテストケース。
 */
public class MainControllerTest {
    private MainController controller;
    private LogViewerModel model;
    private FileIOService fileIOService;
    private FilterService filterService;

    @Before
    public void setUp() {
        model = new LogViewerModel();
        fileIOService = new FileIOService();
        filterService = new FilterService();
        controller = new MainController(model, fileIOService, filterService);
    }

    /**
     * 通常系：コンストラクタとgetterの確認。
     */
    @Test
    public void testConstructorAndGetters() {
        assertNotNull(controller.getModel());
        assertNotNull(controller.getFileIOService());
        assertNotNull(controller.getFilterService());
        
        assertSame(model, controller.getModel());
        assertSame(fileIOService, controller.getFileIOService());
        assertSame(filterService, controller.getFilterService());
    }

    /**
     * 通常系：初期状態では currentLoadPath は null。
     */
    @Test
    public void testInitialCurrentLoadPath() {
        assertNull(controller.getCurrentLoadPath());
    }

    /**
     * 通常系：handleLoadFile でファイルパスが記録されること。
     */
    @Test
    public void testHandleLoadFileSetsCurrentPath() {
        Path testPath = Paths.get("test", "sample.tsv");
        
        // handleLoadFile を呼び出す（実際のファイルは存在しないが、パスの記録はされる）
        controller.handleLoadFile(
            testPath,
            () -> {},  // onStart
            result -> {},  // onSuccess
            ex -> {}  // onFailed
        );
        
        // パスが記録されていることを確認
        assertNotNull(controller.getCurrentLoadPath());
        assertEquals(testPath, controller.getCurrentLoadPath());
    }

    /**
     * 通常系：複数回 handleLoadFile を呼び出すと最新のパスが記録される。
     */
    @Test
    public void testHandleLoadFileUpdatesCurrentPath() {
        Path path1 = Paths.get("test", "file1.tsv");
        Path path2 = Paths.get("test", "file2.tsv");
        
        controller.handleLoadFile(path1, () -> {}, result -> {}, ex -> {});
        assertEquals(path1, controller.getCurrentLoadPath());
        
        // タスクのキャンセル待ちのため短時間スリープ
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        controller.handleLoadFile(path2, () -> {}, result -> {}, ex -> {});
        assertEquals(path2, controller.getCurrentLoadPath());
    }

    /**
     * 通常系：絶対パスと相対パスの両方を扱える。
     */
    @Test
    public void testHandleLoadFileWithAbsolutePath() {
        Path absolutePath = Paths.get("C:", "Users", "test", "sample.tsv").toAbsolutePath();
        
        controller.handleLoadFile(absolutePath, () -> {}, result -> {}, ex -> {});
        
        assertNotNull(controller.getCurrentLoadPath());
        assertEquals(absolutePath, controller.getCurrentLoadPath());
    }
}
