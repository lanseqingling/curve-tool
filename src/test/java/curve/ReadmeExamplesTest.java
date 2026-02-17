package curve;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReadmeExamplesTest {
    static final class Data1 {
        private double val;
        private final long timestamp;

        Data1(double val, long timestamp) {
            this.val = val;
            this.timestamp = timestamp;
        }

        double getVal() {
            return val;
        }

        void setVal(double val) {
            this.val = val;
        }

        long getTimestamp() {
            return timestamp;
        }
    }

    static final class Data2 {
        private final double val;
        private final long timestamp;

        Data2(double val, long timestamp) {
            this.val = val;
            this.timestamp = timestamp;
        }

        double getVal() {
            return val;
        }

        long getTimestamp() {
            return timestamp;
        }
    }

    @Test
    void joinByKeyProcess_example_runs() {
        ICurve<Data1, Double> curve1 = new Curve<>(Arrays.asList(new Data1(10, 1), new Data1(10, 2), new Data1(10, 3)));
        ICurve<Data2, Double> curve2 = new Curve<>(Arrays.asList(new Data2(1, 1), new Data2(3, 3)));

        curve1.joinByKeyProcess(
                curve2,
                Data1::getTimestamp,
                Data2::getTimestamp,
                KeyJoinType.LEFT,
                DuplicateKeyPolicy.THROW,
                MissingPointPolicy.SKIP,
                (d1, d2) -> d1.getVal() + d2.getVal(),
                Data1::setVal
        );

        assertEquals(11.0, curve1.get(0).getVal(), 1e-9);
        assertEquals(10.0, curve1.get(1).getVal(), 1e-9);
        assertEquals(13.0, curve1.get(2).getVal(), 1e-9);
    }
}

