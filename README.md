# JavaFX タブ区切りログビューア

シンプルで軽量なTSVログファイルビューアアプリケーション。大容量ログの効率的な閲覧、フィルタリング、ソート機能を備えた JavaFX ベースのデスクトップアプリケーションです。

## 機能

- **タブ区切り（TSV）ログの読み込み**: 1行を1レコードとして TableView に表示
- **動的な列生成**: ファイルの列数に応じて自動的にテーブル列を生成
- **高度なフィルタリング機能**:
  - 全列または特定列に対する検索
  - 大文字小文字を区別しない部分一致検索
  - 正規表現サポート（`/pattern/`の形式で指定）
- **ソート機能**: 列ヘッダクリックで昇順・降順ソート
- **詳細表示**: 行をダブルクリックで全フィールドを表示
- **クイックコピー**: Ctrl+クリックでセルの内容をクリップボードにコピー
- **ドラッグ＆ドロップ**: ファイルをウィンドウにドラッグして読み込み
- **エクスポート機能**: フィルタされたデータを別のファイルに出力
- **列の表示/非表示**: 特定の列を選択的に表示・非表示

## 技術スタック

- **Java**: 21
- **JavaFX**: GUI フレームワーク（Controls, FXML）
- **Gradle**: ビルドツール
- **ライブラリ**:
  - Guava
  - Spring JDBC
  - Lombok

## 必要要件

- Java 21 以上
- Gradle（Gradle Wrapper が含まれています）

## ビルド・実行方法

### アプリケーションの実行

```bash
# Linux/Mac
./gradlew run

# Windows
gradlew.bat run
```

### ビルド

```bash
# Linux/Mac
./gradlew build

# Windows
gradlew.bat build
```

### 配布版の作成

```bash
# 最小JRE付きの配布パッケージを作成
run-create-dist-no-cache.bat
```

## 使い方

1. **ファイルを開く**: 
   - メニューバーから `File → Open...` を選択するか
   - ファイルをウィンドウにドラッグ＆ドロップ

2. **フィルタリング**: 
   - `Column` ドロップダウンで対象列を選択（`All` で全列検索）
   - `Filter` テキストボックスに検索文字列を入力
   - 正規表現を使用する場合は `/regex/` の形式で入力（例: `/ERROR|FATAL/`）

3. **ソート**: 列ヘッダをクリックして昇順・降順を切り替え

4. **詳細表示**: 行をダブルクリックで全フィールドの詳細を表示

5. **コピー**: Ctrl を押しながらセルをクリックして内容をコピー

6. **列の管理**: 右クリックまたはメニューから列の表示/非表示を設定

7. **データエクスポート**: フィルタされたデータを File → Export で出力

## サンプルファイル

プロジェクトには3つのサンプルログファイルが含まれています：

- **sample_log.tsv**: 基本的なアプリケーションログ（30行、6列）
- **sample_log_with_header.tsv**: ヘッダー付きの構造化ログ（30行、カラム名付き）
- **error_log.tsv**: エラーログのサンプル（20行、ERROR/FATALレベルのみ）

大容量テストファイル：
- **large_sample_log_100m.tsv**: 100MB のログファイル
- **large_sample_log_1g.tsv**: 1GB のログファイル

## プロジェクト構成

```
TSVLogViewer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/logviewer/
│   │   │   │   ├── Main.java              # メインアプリケーション
│   │   │   │   ├── LogRow.java            # ログレコードモデル
│   │   │   │   ├── LoadResult.java        # ファイル読み込み結果
│   │   │   │   ├── controller/            # UI イベントハンドラ
│   │   │   │   ├── model/                 # ビジネスロジック
│   │   │   │   ├── service/               # 機能サービス
│   │   │   │   └── ui/                    # UI ユーティリティ
│   │   │   └── resources/                 # リソースファイル
│   │   └── test/
│   └── build.gradle
├── gradle/                               # Gradle ラッパー設定
├── build/                                # ビルド出力
├── *.tsv                                 # サンプルログファイル
├── gradlew / gradlew.bat                # Gradle Wrapper
├── settings.gradle
└── README.md
```

## アーキテクチャ

### 層構造 (3層アーキテクチャ)

```
┌─────────────────────────────────────────────┐
│  Presentation Layer (UI)                     │
│  ├─ Main.java (JavaFX Application)          │
│  ├─ MenuBarFactory (メニュー生成)           │
│  ├─ TableInitializer (テーブル初期化)       │
│  ├─ FilterConditionPanel (フィルタUI)       │
│  └─ DialogService, ProgressDialogService    │
├─────────────────────────────────────────────┤
│  Business Logic Layer (Controller/Service)   │
│  ├─ MainController (メイン制御)             │
│  ├─ ExportController (エクスポート制御)     │
│  ├─ FilterService (フィルタ処理)            │
│  ├─ SortService (ソート処理)                │
│  ├─ FilterSortService (複合処理)            │
│  ├─ FileIOService (ファイルI/O)             │
│  ├─ ClipboardService (クリップボード)       │
│  ├─ NavigationService (ナビゲーション)      │
│  ├─ SelectionService (選択管理)             │
│  ├─ ColumnVisibilityConfigService (列表示)  │
│  └─ ExportService (エクスポート)            │
├─────────────────────────────────────────────┤
│  Data Layer (Model)                         │
│  ├─ LogViewerModel (データ管理)             │
│  ├─ LogRow (ログレコード)                   │
│  ├─ LoadResult (読み込み結果)               │
│  └─ FileLoadResult (ファイル読み込み結果)   │
└─────────────────────────────────────────────┘
```

### コンポーネント詳細

#### Controller層
- **MainController**: ファイル読み込みと全体的なイベント制御
- **ExportController**: フィルタされたデータのエクスポート機能

#### Service層
- **FileIOService**: TSVファイルの読み込み・解析
- **FilterService**: 検索条件に基づくデータフィルタリング
- **SortService**: 列データのソート処理
- **FilterSortService**: フィルタとソートの複合処理
- **ClipboardService**: クリップボードへのコピー機能
- **NavigationService**: テーブル内のナビゲーション
- **SelectionService**: セル・行の選択管理
- **ColumnVisibilityConfigService**: 列の表示/非表示設定
- **ExportService**: データの別形式への出力
- **ProgressDialogService**: 進捗表示ダイアログ

#### Model層
- **LogViewerModel**: ログデータ、フィルタ・ソート状態、UI状態を一元管理
- **LogRow**: 単一ログレコード（行）を表現
- **LoadResult**: ファイル読み込み処理の結果

#### UI層
- **MenuBarFactory**: メニューバーの動的生成
- **TableInitializer**: TableView の列・データの初期化
- **FilterConditionPanel**: 複数フィルタ条件のUI
- **SingleFilterPanel**: 単一フィルタ条件のUI
- **DialogService**: 各種ダイアログの表示
- **DragAndDropHandler**: ドラッグ＆ドロップの処理

### データフロー

```
1. ファイル読み込み
   User → Main (File Open) → MainController → FileIOService
   → LogRow リスト → LogViewerModel.baseData

2. フィルタ・ソート適用
   FilterService / SortService → FilterSortService
   → LogViewerModel.tableData (UI表示用)

3. 詳細表示
   User (Double Click) → Main → DialogService
   → LogRow の全フィールド表示

4. クリップボードコピー
   User (Ctrl+Click) → ClipboardService
   → Clipboard (OS)
```

## ファイル形式

- **エンコーディング**: UTF-8
- **区切り文字**: タブ (`\t`)
- **拡張子**: `.tsv`, `.txt`, またはすべてのファイル
- 各行が1レコードとして扱われます
- 列数は自動検出され、最大列数に合わせてテーブルが生成されます

## ライセンス
MIT License
```