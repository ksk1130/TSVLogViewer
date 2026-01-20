package logviewer;

class LoadResult {
    final int columns;
    final boolean truncated;
    LoadResult(int columns, boolean truncated) { this.columns = columns; this.truncated = truncated; }
}