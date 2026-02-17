package curve;

public final class KeyJoinRow<K, L, R> {
    private final K key;
    private final L left;
    private final R right;

    public KeyJoinRow(K key, L left, R right) {
        this.key = key;
        this.left = left;
        this.right = right;
    }

    public K getKey() {
        return key;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
