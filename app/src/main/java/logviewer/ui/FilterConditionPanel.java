package logviewer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 複数検索条件パネル。
 */
public class FilterConditionPanel extends VBox {
    public static class FilterCondition {
        public ComboBox<String> columnSelector;
        public TextField filterField;
    }

    private final VBox conditionListPane = new VBox(5);
    private final List<FilterCondition> conditions = new ArrayList<>();
    private List<String> availableColumns = new ArrayList<>();

    /**
     * 複数検索条件パネルを構築します。
     * 
     * @param initialColumns 初期カラムリスト
     * @param onApply        適用ボタン押下時のコールバック
     * @param onClearAll     すべてクリアボタン押下時のコールバック
     */
    public FilterConditionPanel(List<String> initialColumns, Runnable onApply, Runnable onClearAll) {
        this.availableColumns = new ArrayList<>(initialColumns);

        setPadding(new Insets(10));
        setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");

        Label titleLabel = new Label("検索条件");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        conditionListPane.setPadding(new Insets(5, 0, 5, 0));

        ScrollPane scrollPane = new ScrollPane(conditionListPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addButton = new Button("＋ 条件を追加");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addFilterCondition());

        Button applyButton = new Button("適用");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        applyButton.setOnAction(e -> {
            if (onApply != null) {
                onApply.run();
            }
        });

        Button clearAllButton = new Button("すべてクリア");
        clearAllButton.setMaxWidth(Double.MAX_VALUE);
        clearAllButton.setOnAction(e -> {
            clearAllConditions();
            if (onClearAll != null) {
                onClearAll.run();
            }
        });

        VBox buttonBox = new VBox(5, addButton, applyButton, clearAllButton);

        getChildren().addAll(titleLabel, new Separator(), scrollPane, buttonBox);
        setPrefWidth(300);
        setMinWidth(200);
    }

    /**
     * 現在設定されているすべての検索条件を取得します。
     * 
     * @return 検索条件のリスト
     */
    public List<FilterCondition> getConditions() {
        return conditions;
    }

    /**
     * 利用可能なカラムを更新し、すべての条件セレクタを同期します。
     * 
     * @param columns 新しいカラムリスト
     */
    public void updateColumns(List<String> columns) {
        this.availableColumns = new ArrayList<>(columns);
        for (FilterCondition condition : conditions) {
            String currentSelection = condition.columnSelector.getValue();
            condition.columnSelector.getItems().setAll(columns);
            if (columns.contains(currentSelection)) {
                condition.columnSelector.setValue(currentSelection);
            } else {
                condition.columnSelector.getSelectionModel().selectFirst();
            }
        }
    }

    /**
     * すべての検索条件をクリアします。
     */
    public void clearAllConditions() {
        conditions.clear();
        conditionListPane.getChildren().clear();
    }

    /**
     * 新しい検索条件行をパネルに追加します。
     */
    private void addFilterCondition() {
        FilterCondition condition = new FilterCondition();
        conditions.add(condition);

        HBox conditionPane = new HBox(5);
        conditionPane.setPadding(new Insets(5));
        conditionPane.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;");

        VBox contentBox = new VBox(5);

        ComboBox<String> columnCombo = new ComboBox<>();
        columnCombo.getItems().setAll(availableColumns);
        if (availableColumns.isEmpty()) {
            columnCombo.getItems().add("All");
        }
        columnCombo.getSelectionModel().selectFirst();
        columnCombo.setMaxWidth(Double.MAX_VALUE);
        condition.columnSelector = columnCombo;

        TextField filterText = new TextField();
        filterText.setPromptText("検索文字列");
        filterText.setMaxWidth(Double.MAX_VALUE);
        condition.filterField = filterText;

        contentBox.getChildren().addAll(
            new Label("カラム:"),
            columnCombo,
            new Label("条件:"),
            filterText
        );

        Button removeButton = new Button("ー");
        removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        removeButton.setOnAction(e -> {
            conditions.remove(condition);
            conditionListPane.getChildren().remove(conditionPane);
        });

        HBox.setHgrow(contentBox, Priority.ALWAYS);
        conditionPane.getChildren().addAll(contentBox, removeButton);

        conditionListPane.getChildren().add(conditionPane);
    }
}
