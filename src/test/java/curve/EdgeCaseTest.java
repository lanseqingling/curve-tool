package curve;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeCaseTest {
    static final class P {
        private final long ts;
        private double v;

        P(long ts, double v) {
            this.ts = ts;
            this.v = v;
        }

        long ts() {
            return ts;
        }

        double v() {
            return v;
        }

        void setV(double v) {
            this.v = v;
        }
    }

    @Test
    void movingAverage_empty_isEmpty() {
        ICurve<P, Double> c = new Curve<>();
        assertEquals(0, c.movingAverage(P::v, 3).size());
    }

    @Test
    void joinByKeyProcess_passNull_allowsCustomHandling() {
        ICurve<P, Double> left = new Curve<>(Arrays.asList(new P(1, 10), new P(2, 10)));
        ICurve<P, Double> right = new Curve<>(Arrays.asList(new P(1, 1)));

        left.joinByKeyProcess(
                right,
                P::ts,
                P::ts,
                KeyJoinType.LEFT,
                DuplicateKeyPolicy.THROW,
                MissingPointPolicy.PASS_NULL,
                (l, r) -> l.v() + (r == null ? 0.0 : r.v()),
                P::setV
        );

        assertEquals(11.0, left.get(0).v(), 1e-9);
        assertEquals(10.0, left.get(1).v(), 1e-9);
    }

    @Test
    void joinIndex_duplicateKeyPolicy_behaves() {
        ICurve<P, Double> c = new Curve<>(Arrays.asList(new P(1, 1), new P(1, 2)));

        assertThrows(IllegalArgumentException.class, () -> CurveJoins.indexByKey(c, P::ts, DuplicateKeyPolicy.THROW));
        assertEquals(1.0, CurveJoins.indexByKey(c, P::ts, DuplicateKeyPolicy.FIRST).get(1L).v(), 1e-9);
        assertEquals(2.0, CurveJoins.indexByKey(c, P::ts, DuplicateKeyPolicy.LAST).get(1L).v(), 1e-9);
    }

    @Test
    void groupJoin_throwOnMissingRightKey() {
        ICurveGroup<String, P, Double> left = CurveGroup.create(Arrays.asList(new P(1, 1)), p -> "a");
        ICurveGroup<String, P, Double> right = CurveGroup.create(Arrays.asList(new P(1, 1)), p -> "b");

        assertThrows(IllegalArgumentException.class, () ->
                left.biProcess(right, GroupJoinType.LEFT, MissingCurvePolicy.THROW, (k, p1, p2) -> p1.v(), P::setV)
        );
    }

    @Test
    void biProcess_padNull_passesNullWhenRightMissing() {
        ICurve<P, Double> left = new Curve<>(Arrays.asList(new P(1, 1), new P(2, 2)));
        ICurve<P, Double> right = new Curve<>(Arrays.asList(new P(1, 10)));

        left.biProcess(right, IndexAlignmentPolicy.PAD_NULL, (l, r) -> l.v() + (r == null ? 0.0 : r.v()), P::setV);

        assertEquals(11.0, left.get(0).v(), 1e-9);
        assertEquals(2.0, left.get(1).v(), 1e-9);
    }
}

