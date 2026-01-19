```markdown
# JavaFX タブ区切りログビューア

シンプルで軽量なTSVログファイルビューアアプリケーションです。

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

## 技術スタック

- **Java**: 21
- **JavaFX**: GUI フレームワーク
- **Gradle**: ビルドツール
- **依存関係**:
  - JavaFX Controls & FXML
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

## 使い方

1. **ファイルを開く**: メニューバーから `File → Open...` を選択し、TSVログファイルを選択
2. **フィルタリング**: 
   - `Column` ドロップダウンで対象列を選択（`All` で全列検索）
   - `Filter` テキストボックスに検索文字列を入力
   - 正規表現を使用する場合は `/regex/` の形式で入力（例: `/ERROR|FATAL/`）
3. **ソート**: 列ヘッダをクリックして昇順・降順を切り替え
4. **詳細表示**: 行をダブルクリックで全フィールドの詳細を表示
5. **コピー**: Ctrl を押しながらセルをクリックして内容をコピー

## サンプルファイル

プロジェクトには3つのサンプルログファイルが含まれています：

- **sample_log.tsv**: 基本的なアプリケーションログ（30行、6列）
- **sample_log_with_header.tsv**: ヘッダー付きの構造化ログ（30行、カラム名付き）
- **error_log.tsv**: エラーログのサンプル（20行、ERROR/FATALレベルのみ）

アプリケーション起動後、これらのファイルで機能を試すことができます。

## プロジェクト構成

```
JavaFXLogViewer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── logviewer/
│   │   │   │       └── Main.java    # メインアプリケーション
│   │   │   └── resources/
│   │   └── test/
│   └── build.gradle                  # アプリケーションビルド設定
├── gradle/                           # Gradle設定
├── sample_log.tsv                    # サンプルログ1
├── sample_log_with_header.tsv        # サンプルログ2
├── error_log.tsv                     # サンプルログ3
├── gradlew / gradlew.bat            # Gradle Wrapper
├── settings.gradle
└── README.md
```

## ファイル形式

- **エンコーディング**: UTF-8
- **区切り文字**: タブ (`\t`)
- **拡張子**: `.tsv`, `.txt`, またはすべてのファイル
- 各行が1レコードとして扱われます
- 列数は自動検出され、最大列数に合わせてテーブルが生成されます

## ライセンス

このプロジェクトはサンプルコードです。
```