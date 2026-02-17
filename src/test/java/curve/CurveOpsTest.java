package curve;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurveOpsTest {
    static final class Point {
        private final long ts;
        private double val;

        Point(long ts, double val) {
            this.ts = ts;
            this.val = val;
        }

        long getTs() {
            return ts;
        }

        double getVal() {
            return val;
        }

        void setVal(double val) {
            this.val = val;
        }
    }

    @Test
    void map_filter_reduce_work() {
        ICurve<Integer, Integer> c = new Curve<>(Arrays.asList(1, 2, 3, 4));
        ICurve<Integer, Object> mapped = c.filter(x -> x % 2 == 0).map(x -> x * 10);
        int sum = mapped.reduce(0, Integer::sum);
        assertEquals(60, sum);
    }

    @Test
    void movingAverage_usesTrailingWindow() {
        ICurve<Point, Double> c = new Curve<>(Arrays.asList(
                new Point(1, 1),
                new Point(2, 2),
                new Point(3, 3),
                new Point(4, 4)
        ));
        ICurve<Double, Double> ma = c.movingAverage(Point::getVal, 3);
        assertEquals(1.0, ma.get(0), 1e-9);
        assertEquals(1.5, ma.get(1), 1e-9);
        assertEquals(2.0, ma.get(2), 1e-9);
        assertEquals(3.0, ma.get(3), 1e-9);
    }

    @Test
    void diff_returnsNMinus1() {
        ICurve<Point, Double> c = new Curve<>(Arrays.asList(new Point(1, 2), new Point(2, 5), new Point(3, 9)));
        ICurve<Double, Double> d = c.diff(Point::getVal);
        assertEquals(2, d.size());
        assertEquals(3.0, d.get(0), 1e-9);
        assertEquals(4.0, d.get(1), 1e-9);
    }

    @Test
    void joinByKeyProcess_leftJoin_updatesMatching() {
        ICurve<Point, Double> left = new Curve<>(Arrays.asList(new Point(1, 10), new Point(2, 10), new Point(3, 10)));
        ICurve<Point, Double> right = new Curve<>(Arrays.asList(new Point(1, 1), new Point(3, 3)));

        left.joinByKeyProcess(
                right,
                Point::getTs,
                Point::getTs,
                KeyJoinType.LEFT,
                DuplicateKeyPolicy.THROW,
                MissingPointPolicy.SKIP,
                (l, r) -> l.getVal() + r.getVal(),
                (p, v) -> p.setVal(v)
        );

        assertEquals(11.0, left.get(0).getVal(), 1e-9);
        assertEquals(10.0, left.get(1).getVal(), 1e-9);
        assertEquals(13.0, left.get(2).getVal(), 1e-9);
    }
}

