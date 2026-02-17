package curve;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyBehaviorTest {
    static final class Data {
        private double val;

        Data(double val) {
            this.val = val;
        }

        double getVal() {
            return val;
        }

        void setVal(double val) {
            this.val = val;
        }
    }

    @Test
    void biProcess_truncate_updatesMinLength() {
        ICurve<Data, Double> left = new Curve<>(Arrays.asList(new Data(1.0), new Data(2.0)));
        ICurve<Data, Double> right = new Curve<>(Arrays.asList(new Data(10.0)));

        left.biProcess(right, IndexAlignmentPolicy.TRUNCATE, (l, r) -> l.getVal() + r.getVal(), (d, v) -> d.setVal(v));

        assertEquals(11.0, left.get(0).getVal(), 1e-9);
        assertEquals(2.0, left.get(1).getVal(), 1e-9);
    }

    @Test
    void biProcess_throw_throwsOnMismatch() {
        ICurve<Data, Double> left = new Curve<>(Arrays.asList(new Data(1.0), new Data(2.0)));
        ICurve<Data, Double> right = new Curve<>(Arrays.asList(new Data(10.0)));

        assertThrows(IllegalArgumentException.class, () ->
                left.biProcess(right, IndexAlignmentPolicy.THROW, (l, r) -> l.getVal() + r.getVal(), (d, v) -> d.setVal(v))
        );
    }

    @Test
    void curveGroup_fullJoin_createEmpty_addsMissingLeftKey() {
        ICurveGroup<String, Data, Double> left = CurveGroup.create(Arrays.asList(new Data(1.0)), d -> "a");
        ICurveGroup<String, Data, Double> right = CurveGroup.create(Arrays.asList(new Data(2.0)), d -> "b");

        left.forCurve(right, GroupJoinType.FULL, MissingCurvePolicy.CREATE_EMPTY, (k, c1, c2) -> {
        });

        assertNotNull(left.get("a"));
        assertNotNull(left.get("b"));
        assertEquals(0, left.get("b").size());
    }
}

