package curve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class CurveJoins {
    private CurveJoins() {
    }

    public static <K, U> Map<K, U> indexByKey(ICurve<U, ?> curve, Function<? super U, ? extends K> keyFn, DuplicateKeyPolicy duplicateKeyPolicy) {
        Objects.requireNonNull(keyFn, "keyFn");
        DuplicateKeyPolicy resolvedPolicy = duplicateKeyPolicy == null ? DuplicateKeyPolicy.LAST : duplicateKeyPolicy;

        if (curve == null || curve.isEmpty()) {
            return new HashMap<>();
        }

        Map<K, U> out = new LinkedHashMap<>();
        for (U u : curve) {
            K key = keyFn.apply(u);
            if (out.containsKey(key)) {
                if (resolvedPolicy == DuplicateKeyPolicy.FIRST) {
                    continue;
                }
                if (resolvedPolicy == DuplicateKeyPolicy.THROW) {
                    throw new IllegalArgumentException("duplicate key: " + key);
                }
            }
            out.put(key, u);
        }
        return out;
    }

    public static <K, T, V, U, W> ICurve<KeyJoinRow<K, T, U>, Object> joinByKey(
            ICurve<T, V> left,
            ICurve<U, W> right,
            Function<? super T, ? extends K> leftKeyFn,
            Function<? super U, ? extends K> rightKeyFn,
            KeyJoinType joinType,
            DuplicateKeyPolicy duplicateKeyPolicy
    ) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(leftKeyFn, "leftKeyFn");
        Objects.requireNonNull(rightKeyFn, "rightKeyFn");

        KeyJoinType resolvedJoinType = joinType == null ? KeyJoinType.LEFT : joinType;

        Map<K, U> rightIndex = indexByKey(right, rightKeyFn, duplicateKeyPolicy);
        if (resolvedJoinType == KeyJoinType.LEFT || resolvedJoinType == KeyJoinType.INNER) {
            ArrayList<KeyJoinRow<K, T, U>> out = new ArrayList<>(left.size());
            for (T t : left) {
                K key = leftKeyFn.apply(t);
                U u = rightIndex.get(key);
                if (resolvedJoinType == KeyJoinType.INNER && u == null) {
                    continue;
                }
                out.add(new KeyJoinRow<>(key, t, u));
            }
            return new Curve<>(out);
        }

        Map<K, T> leftIndex = indexByKey(left, leftKeyFn, duplicateKeyPolicy);
        if (resolvedJoinType == KeyJoinType.RIGHT) {
            ArrayList<KeyJoinRow<K, T, U>> out = new ArrayList<>(rightIndex.size());
            for (Map.Entry<K, U> e : rightIndex.entrySet()) {
                K key = e.getKey();
                out.add(new KeyJoinRow<>(key, leftIndex.get(key), e.getValue()));
            }
            return new Curve<>(out);
        }

        if (resolvedJoinType == KeyJoinType.FULL) {
            Set<K> keys = new LinkedHashSet<>(leftIndex.keySet());
            keys.addAll(rightIndex.keySet());
            ArrayList<KeyJoinRow<K, T, U>> out = new ArrayList<>(keys.size());
            for (K key : keys) {
                out.add(new KeyJoinRow<>(key, leftIndex.get(key), rightIndex.get(key)));
            }
            return new Curve<>(out);
        }

        throw new IllegalArgumentException("unsupported joinType: " + resolvedJoinType);
    }

}
