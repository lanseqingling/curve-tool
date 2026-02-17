package curve;

import function.TriConsumer;
import function.TriFunction;
import function.TriPredicate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public final class CurveGroupOps {
    private CurveGroupOps() {
    }

    public static <K, T, V, U> ICurveGroup<K, T, V> biProcess(
            ICurveGroup<K, T, V> leftGroup,
            ICurveGroup<K, U, V> rightGroup,
            GroupJoinType joinType,
            MissingCurvePolicy missingCurvePolicy,
            TriFunction<K, T, U, V> triF,
            BiConsumer<T, V> biC
    ) {
        return biProcess(leftGroup, rightGroup, joinType, missingCurvePolicy, null, triF, biC);
    }

    public static <K, T, V, U> ICurveGroup<K, T, V> biProcess(
            ICurveGroup<K, T, V> leftGroup,
            ICurveGroup<K, U, V> rightGroup,
            GroupJoinType joinType,
            MissingCurvePolicy missingCurvePolicy,
            TriPredicate<K, T, U> triP,
            TriFunction<K, T, U, V> triF,
            BiConsumer<T, V> biC
    ) {
        Objects.requireNonNull(leftGroup, "leftGroup");
        Objects.requireNonNull(triF, "triF");
        Objects.requireNonNull(biC, "biC");

        GroupJoinType resolvedJoinType = joinType == null ? GroupJoinType.LEFT : joinType;
        MissingCurvePolicy resolvedMissing = missingCurvePolicy == null ? MissingCurvePolicy.SKIP : missingCurvePolicy;

        for (K k : keys(leftGroup, rightGroup, resolvedJoinType)) {
            ICurve<T, V> left = leftGroup.get(k);
            ICurve<U, V> right = rightGroup == null ? null : rightGroup.get(k);

            if (left == null) {
                if (resolvedMissing == MissingCurvePolicy.THROW) {
                    throw new IllegalArgumentException("missing left curve for key: " + k);
                }
                if (resolvedMissing == MissingCurvePolicy.CREATE_EMPTY) {
                    left = new Curve<>();
                    leftGroup.put(k, left);
                } else {
                    continue;
                }
            }

            if (right == null) {
                if (resolvedMissing == MissingCurvePolicy.THROW) {
                    throw new IllegalArgumentException("missing right curve for key: " + k);
                }
                if (resolvedMissing == MissingCurvePolicy.CREATE_EMPTY) {
                    right = new Curve<>();
                } else {
                    continue;
                }
            }

            ICurve<U, V> finalRight = right;
            left.biProcess(finalRight, (t, u) -> triP == null || triP.test(k, t, u), (t, u) -> triF.apply(k, t, u), biC);
        }
        return leftGroup;
    }

    public static <K, T, V, U> ICurveGroup<K, T, V> forCurve(
            ICurveGroup<K, T, V> leftGroup,
            ICurveGroup<K, U, V> rightGroup,
            GroupJoinType joinType,
            MissingCurvePolicy missingCurvePolicy,
            TriConsumer<K, ICurve<T, V>, ICurve<U, V>> triC
    ) {
        Objects.requireNonNull(leftGroup, "leftGroup");
        Objects.requireNonNull(triC, "triC");

        GroupJoinType resolvedJoinType = joinType == null ? GroupJoinType.LEFT : joinType;
        MissingCurvePolicy resolvedMissing = missingCurvePolicy == null ? MissingCurvePolicy.SKIP : missingCurvePolicy;

        for (K k : keys(leftGroup, rightGroup, resolvedJoinType)) {
            ICurve<T, V> left = leftGroup.get(k);
            ICurve<U, V> right = rightGroup == null ? null : rightGroup.get(k);

            if (left == null) {
                if (resolvedMissing == MissingCurvePolicy.THROW) {
                    throw new IllegalArgumentException("missing left curve for key: " + k);
                }
                if (resolvedMissing == MissingCurvePolicy.CREATE_EMPTY) {
                    left = new Curve<>();
                    leftGroup.put(k, left);
                } else {
                    continue;
                }
            }

            if (right == null) {
                if (resolvedMissing == MissingCurvePolicy.THROW) {
                    throw new IllegalArgumentException("missing right curve for key: " + k);
                }
                if (resolvedMissing == MissingCurvePolicy.CREATE_EMPTY) {
                    right = new Curve<>();
                } else {
                    continue;
                }
            }

            triC.accept(k, left, right);
        }

        return leftGroup;
    }

    private static <K, T, V, U> Set<K> keys(ICurveGroup<K, T, V> leftGroup, ICurveGroup<K, U, V> rightGroup, GroupJoinType joinType) {
        if (joinType == GroupJoinType.INNER) {
            Set<K> keys = new HashSet<>(leftGroup.keySet());
            if (rightGroup == null) {
                keys.clear();
            } else {
                keys.retainAll(rightGroup.keySet());
            }
            return keys;
        }

        if (joinType == GroupJoinType.FULL) {
            Set<K> keys = new HashSet<>(leftGroup.keySet());
            if (rightGroup != null) {
                keys.addAll(rightGroup.keySet());
            }
            return keys;
        }

        return leftGroup.keySet();
    }
}
