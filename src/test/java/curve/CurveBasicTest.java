package curve;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurveBasicTest {
    static final class Data {
        private double val;
        private final String tag;

        Data(double val) {
            this(val, null);
        }

        Data(double val, String tag) {
            this.val = val;
            this.tag = tag;
        }

        double getVal() {
            return val;
        }

        void setVal(double val) {
            this.val = val;
        }

        String getTag() {
            return tag;
        }
    }

    @Test
    void process_appliesFunctionAndConsumer() {
        ICurve<Data, Double> curve = new Curve<>(Arrays.asList(new Data(1.0), new Data(2.0)));
        curve.process(d -> d.getVal() * 2, (d, v) -> d.setVal(v));
        assertEquals(2.0, curve.get(0).getVal(), 1e-9);
        assertEquals(4.0, curve.get(1).getVal(), 1e-9);
    }

    @Test
    void process_withPredicate_skipsNonMatching() {
        ICurve<Data, Double> curve = new Curve<>(Arrays.asList(new Data(0.4), new Data(0.6)));
        curve.process(d -> d.getVal() > 0.5, d -> d.getVal() * 2, (d, v) -> d.setVal(v));
        assertEquals(0.4, curve.get(0).getVal(), 1e-9);
        assertEquals(1.2, curve.get(1).getVal(), 1e-9);
    }

    @Test
    void biProcess_joinsByIndex() {
        ICurve<Data, Double> c1 = new Curve<>(Arrays.asList(new Data(1.0), new Data(2.0)));
        ICurve<Data, Double> c2 = new Curve<>(Arrays.asList(new Data(10.0), new Data(20.0)));
        c1.biProcess(c2, (d1, d2) -> d1.getVal() + d2.getVal(), (d, v) -> d.setVal(v));
        assertEquals(11.0, c1.get(0).getVal(), 1e-9);
        assertEquals(22.0, c1.get(1).getVal(), 1e-9);
    }

    @Test
    void biProcess_sizeMismatch_isNoOpByDefault() {
        ICurve<Data, Double> c1 = new Curve<>(Arrays.asList(new Data(1.0), new Data(2.0)));
        ICurve<Data, Double> c2 = new Curve<>(Arrays.asList(new Data(10.0)));
        c1.biProcess(c2, (d1, d2) -> d1.getVal() + d2.getVal(), (d, v) -> d.setVal(v));
        assertEquals(1.0, c1.get(0).getVal(), 1e-9);
        assertEquals(2.0, c1.get(1).getVal(), 1e-9);
    }

    @Test
    void curveGroup_createAndProcess() {
        List<Data> data = Arrays.asList(new Data(1.0, "a"), new Data(2.0, "a"), new Data(3.0, "b"));
        ICurveGroup<String, Data, Double> group = CurveGroup.create(data, Data::getTag);
        group.process((tag, d) -> d.getVal() + 1, (d, v) -> d.setVal(v));
        assertEquals(2.0, group.get("a").get(0).getVal(), 1e-9);
        assertEquals(3.0, group.get("a").get(1).getVal(), 1e-9);
        assertEquals(4.0, group.get("b").get(0).getVal(), 1e-9);
    }
}
