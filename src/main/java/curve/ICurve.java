package curve;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.*;

public interface ICurve<T, V> extends List<T> {
    ICurve<T, V> process(Consumer<T> c);

    ICurve<T, V> process(Function<T, V> f, BiConsumer<T, V> biC);

    ICurve<T, V> process(Predicate<T> p, Function<T, V> f, BiConsumer<T, V> biC);

    <U> ICurve<T, V> biProcess(ICurve<U, V> curve, BiFunction<T, U, V> biF, BiConsumer<T, V> biC);

    <U> ICurve<T, V> biProcess(ICurve<U, V> curve, BiPredicate<T, U> biP, BiFunction<T, U, V> biF, BiConsumer<T, V> biC);

    default <U> ICurve<T, V> biProcess(ICurve<U, V> curve, IndexAlignmentPolicy alignment, BiFunction<T, U, V> biF, BiConsumer<T, V> biC) {
        return biProcess(curve, alignment, null, biF, biC);
    }

    default <U> ICurve<T, V> biProcess(ICurve<U, V> curve, IndexAlignmentPolicy alignment, BiPredicate<T, U> biP, BiFunction<T, U, V> biF, BiConsumer<T, V> biC) {
        IndexAlignmentPolicy resolvedAlignment = alignment == null ? IndexAlignmentPolicy.SKIP_IF_MISMATCH : alignment;
        if (curve == null) {
            if (resolvedAlignment == IndexAlignmentPolicy.THROW) {
                throw new IllegalArgumentException("curve is null");
            }
            return this;
        }

        int leftSize = this.size();
        if (leftSize == 0) {
            return this;
        }

        int rightSize = curve.size();
        if (resolvedAlignment == IndexAlignmentPolicy.SKIP_IF_MISMATCH && rightSize != leftSize) {
            return this;
        }
        if (resolvedAlignment == IndexAlignmentPolicy.THROW && rightSize != leftSize) {
            throw new IllegalArgumentException("curve size mismatch: left=" + leftSize + ", right=" + rightSize);
        }

        int limit = resolvedAlignment == IndexAlignmentPolicy.TRUNCATE ? Math.min(leftSize, rightSize) : leftSize;
        for (int i = 0; i < limit; i++) {
            T t = this.get(i);
            U u = i < rightSize ? curve.get(i) : null;
            if (biP == null || biP.test(t, u)) {
                biC.accept(t, biF.apply(t, u));
            }
        }
        return this;
    }

    default <R, W> ICurve<R, W> map(Function<? super T, ? extends R> mapper) {
        ArrayList<R> out = new ArrayList<>(this.size());
        for (T t : this) {
            out.add(mapper.apply(t));
        }
        return new Curve<>(out);
    }

    default ICurve<T, V> filter(Predicate<? super T> predicate) {
        ArrayList<T> out = new ArrayList<>();
        for (T t : this) {
            if (predicate.test(t)) {
                out.add(t);
            }
        }
        return new Curve<>(out);
    }

    default <R> R reduce(R identity, BiFunction<R, ? super T, R> accumulator) {
        R out = identity;
        for (T t : this) {
            out = accumulator.apply(out, t);
        }
        return out;
    }

    default double sum(ToDoubleFunction<? super T> mapper) {
        double sum = 0.0;
        for (T t : this) {
            sum += mapper.applyAsDouble(t);
        }
        return sum;
    }

    default double average(ToDoubleFunction<? super T> mapper) {
        int n = this.size();
        if (n == 0) {
            return Double.NaN;
        }
        return sum(mapper) / n;
    }

    default ICurve<Double, Double> diff(ToDoubleFunction<? super T> mapper) {
        int n = this.size();
        if (n <= 1) {
            return new Curve<>();
        }

        ArrayList<Double> out = new ArrayList<>(n - 1);
        double prev = mapper.applyAsDouble(this.get(0));
        for (int i = 1; i < n; i++) {
            double curr = mapper.applyAsDouble(this.get(i));
            out.add(curr - prev);
            prev = curr;
        }
        return new Curve<>(out);
    }

    default ICurve<Double, Double> movingAverage(ToDoubleFunction<? super T> mapper, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }

        int n = this.size();
        if (n == 0) {
            return new Curve<>();
        }

        ArrayList<Double> out = new ArrayList<>(n);
        double sum = 0.0;
        int start = 0;
        for (int end = 0; end < n; end++) {
            sum += mapper.applyAsDouble(this.get(end));
            while (end - start + 1 > windowSize) {
                sum -= mapper.applyAsDouble(this.get(start));
                start++;
            }
            int windowLen = end - start + 1;
            out.add(sum / windowLen);
        }

        return new Curve<>(out);
    }

    default <K> ICurveGroup<K, T, V> groupBy(Function<? super T, ? extends K> keyFn) {
        return CurveGroup.create(this, keyFn::apply);
    }

    default <K, U> ICurve<T, V> joinByKeyProcess(
            ICurve<U, V> right,
            Function<? super T, ? extends K> leftKeyFn,
            Function<? super U, ? extends K> rightKeyFn,
            KeyJoinType joinType,
            DuplicateKeyPolicy duplicateKeyPolicy,
            MissingPointPolicy missingPointPolicy,
            BiFunction<T, U, V> biF,
            BiConsumer<T, V> biC
    ) {
        KeyJoinType resolvedJoinType = joinType == null ? KeyJoinType.LEFT : joinType;
        if (resolvedJoinType != KeyJoinType.LEFT && resolvedJoinType != KeyJoinType.INNER) {
            throw new UnsupportedOperationException("joinByKeyProcess supports only LEFT or INNER join");
        }

        MissingPointPolicy resolvedMissing = missingPointPolicy == null ? MissingPointPolicy.SKIP : missingPointPolicy;
        Map<K, U> rightIndex = CurveJoins.indexByKey(right, rightKeyFn, duplicateKeyPolicy);

        for (T t : this) {
            K key = leftKeyFn.apply(t);
            U u = rightIndex.get(key);
            if (u == null && resolvedJoinType == KeyJoinType.INNER) {
                continue;
            }
            if (u == null && resolvedMissing == MissingPointPolicy.THROW) {
                throw new IllegalArgumentException("missing right value for key: " + key);
            }
            if (u == null && resolvedMissing == MissingPointPolicy.SKIP) {
                continue;
            }
            biC.accept(t, biF.apply(t, u));
        }

        return this;
    }
}
