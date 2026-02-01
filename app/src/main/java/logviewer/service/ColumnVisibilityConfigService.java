package logviewer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * カラムの表示/非表示設定をJSON形式で保存・管理するサービスクラス。
 * ファイル名をキーとして、非表示カラムのインデックスを記録します。
 */
public class ColumnVisibilityConfigService {
    private static final String CONFIG_DIR_NAME = ".logviewer";
    private static final String CONFIG_FILE_NAME = "column_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path configFilePath;

    /**
     * ColumnVisibilityConfigService のコンストラクタ。
     * 設定ファイルのパスを初期化します（~/.logviewer/column_config.json）。
     */
    public ColumnVisibilityConfigService() {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path configDir = userHome.resolve(CONFIG_DIR_NAME);
        this.configFilePath = configDir.resolve(CONFIG_FILE_NAME);
    }

    /**
     * 指定されたファイル名のカラム非表示設定を取得します。
     *
     * @param fileName ファイル名（パスを除いたファイル名のみ）
     * @return 非表示カラムのインデックスリスト
     */
    public List<Integer> loadHiddenColumns(String fileName) {
        List<Integer> hiddenColumns = new ArrayList<>();
        
        if (!Files.exists(configFilePath)) {
            return hiddenColumns;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null && root.has(fileName)) {
                JsonArray array = root.getAsJsonArray(fileName);
                for (var element : array) {
                    hiddenColumns.add(element.getAsInt());
                }
            }
        } catch (IOException e) {
            System.err.println("カラム設定ファイルの読み込みに失敗しました: " + e.getMessage());
        }

        return hiddenColumns;
    }

    /**
     * 指定されたファイル名のカラム非表示設定を保存します。
     *
     * @param fileName      ファイル名（パスを除いたファイル名のみ）
     * @param hiddenColumns 非表示カラムのインデックスリスト
     */
    public void saveHiddenColumns(String fileName, List<Integer> hiddenColumns) {
        try {
            // 設定ディレクトリが存在しない場合は作成
            Path configDir = configFilePath.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // 既存の設定を読み込む
            JsonObject root = new JsonObject();
            if (Files.exists(configFilePath)) {
                try (BufferedReader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
                    JsonObject existing = gson.fromJson(reader, JsonObject.class);
                    if (existing != null) {
                        root = existing;
                    }
                }
            }

            // 新しい設定を追加
            JsonArray array = new JsonArray();
            for (Integer columnIndex : hiddenColumns) {
                array.add(columnIndex);
            }
            root.add(fileName, array);

            // 設定ファイルに保存
            try (BufferedWriter writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("カラム設定ファイルの保存に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 指定されたファイル名の設定を削除します。
     *
     * @param fileName ファイル名（パスを除いたファイル名のみ）
     */
    public void deleteHiddenColumnsConfig(String fileName) {
        if (!Files.exists(configFilePath)) {
            return;
        }

        try {
            JsonObject root = new JsonObject();
            try (BufferedReader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
                JsonObject existing = gson.fromJson(reader, JsonObject.class);
                if (existing != null) {
                    root = existing;
                }
            }

            root.remove(fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("カラム設定ファイルの削除に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 全てのカラム非表示設定を削除します（初期化）。
     */
    public void clearAllConfig() {
        try {
            if (Files.exists(configFilePath)) {
                Files.delete(configFilePath);
            }
        } catch (IOException e) {
            System.err.println("カラム設定ファイルの全削除に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 設定ファイルのパスを取得します（デバッグ用）。
     *
     * @return 設定ファイルのパス
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }
}
