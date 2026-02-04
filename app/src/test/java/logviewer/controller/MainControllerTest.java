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
 * 
 * テスト対象：
 * - ファイル読み込み処理 (handleLoadFile)
 * - ファイルクローズ処理 (handleCloseFile)
 * - コンストラクタと getter メソッド
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

    /**
     * 通常系：handleCloseFile でモデルがクリアされること。
     */
    @Test
    public void testHandleCloseFile_ClearsModel() {
        // モデルにデータを追加
        model.addBaseDataRow(new logviewer.LogRow(new String[]{"test"}, 1));
        model.setColumnCount(5);
        model.setSortConfig(1, false);
        
        // 初期状態の確認
        assertEquals(1, model.getBaseData().size());
        assertEquals(5, model.getColumnCount());
        
        // handleCloseFile を呼び出す
        controller.handleCloseFile(null);
        
        // モデルがクリアされたことを確認
        assertEquals(0, model.getBaseData().size());
        assertEquals(0, model.getTableData().size());
        assertEquals(0, model.getColumnCount());
        assertEquals(-1, model.getSortColumnIndex());
        assertTrue(model.isSortAscending());
    }

    /**
     * 通常系：handleCloseFile で currentLoadPath がリセットされること。
     */
    @Test
    public void testHandleCloseFile_ResetsCurrentLoadPath() {
        Path testPath = Paths.get("test", "sample.tsv");
        
        // パスを設定（例外が出ないようにコールバックを指定）
        try {
            controller.handleLoadFile(testPath, () -> {}, result -> {}, ex -> {});
            // タスクが開始されるのを待つ
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // handleCloseFile を呼び出す
        controller.handleCloseFile(null);
        
        // パスがリセットされたことを確認
        assertNull(controller.getCurrentLoadPath());
    }

    /**
     * 通常系：handleCloseFile でコールバックが実行されること。
     */
    @Test
    public void testHandleCloseFile_ExecutesCallback() {
        final boolean[] callbackExecuted = {false};
        
        controller.handleCloseFile(() -> {
            callbackExecuted[0] = true;
        });
        
        assertTrue("onClosed コールバックが実行されていません", callbackExecuted[0]);
    }

    /**
     * 通常系：handleCloseFile で null コールバックを指定してもエラーが出ないこと。
     */
    @Test
    public void testHandleCloseFile_WithNullCallback() {
        Path testPath = Paths.get("test", "sample.tsv");
        
        try {
            controller.handleLoadFile(testPath, () -> {}, result -> {}, ex -> {});
            // タスクが開始されるのを待つ
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // null コールバックで呼び出す（例外が出ないことを確認）
        controller.handleCloseFile(null);
        
        // パスがリセットされていることを確認
        assertNull(controller.getCurrentLoadPath());
    }

    /**
     * 通常系：handleCloseFile でステータスメッセージが「準備完了」になること。
     */
    @Test
    public void testHandleCloseFile_SetsStatusMessage() {
        model.setStatusMessage("読み込み中");
        assertEquals("読み込み中", model.getStatusMessage());
        
        controller.handleCloseFile(null);
        
        assertEquals("準備完了", model.getStatusMessage());
    }
}
