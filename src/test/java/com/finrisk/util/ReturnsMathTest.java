package com.finrisk.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReturnsMathTest {

    @Test
    void logReturnsAreLnOfPriceRatios() {
        double[] r = ReturnsMath.logReturns(List.of(100.0, 110.0, 99.0));

        assertThat(r).hasSize(2);
        assertThat(r[0]).isCloseTo(Math.log(1.1), within(1e-12));
        assertThat(r[1]).isCloseTo(Math.log(0.9), within(1e-12));
    }

    @Test
    void logReturnsEmptyWhenFewerThanTwoPrices() {
        assertThat(ReturnsMath.logReturns(List.of())).isEmpty();
        assertThat(ReturnsMath.logReturns(List.of(100.0))).isEmpty();
    }

    @Test
    void meanIsArithmeticAverage() {
        assertThat(ReturnsMath.mean(new double[]{1, 2, 3, 4})).isEqualTo(2.5);
    }

    @Test
    void stdDevIsSampleWithNMinusOne() {
        // variance = (2.25+0.25+0.25+2.25)/3 = 5/3
        assertThat(ReturnsMath.stdDev(new double[]{1, 2, 3, 4})).isCloseTo(Math.sqrt(5.0 / 3), within(1e-12));
    }

    @Test
    void stdDevZeroWhenFewerThanTwoSamples() {
        assertThat(ReturnsMath.stdDev(new double[]{})).isZero();
        assertThat(ReturnsMath.stdDev(new double[]{7})).isZero();
    }

    @Test
    void covarianceIsSampleOverShorterLength() {
        assertThat(ReturnsMath.covariance(new double[]{1, 2, 3, 4}, new double[]{2, 4, 6, 8}))
                .isCloseTo(10.0 / 3, within(1e-12));
        // truncated to first 3: a={1,2,3} b={2,4,6} -> cov = 2
        assertThat(ReturnsMath.covariance(new double[]{1, 2, 3, 4}, new double[]{2, 4, 6}))
                .isCloseTo(2.0, within(1e-12));
    }

    @Test
    void correlationIsPlusOrMinusOneForLinearSeries() {
        double[] a = {1, 2, 3, 4};
        assertThat(ReturnsMath.correlation(a, new double[]{2, 4, 6, 8})).isCloseTo(1.0, within(1e-12));
        assertThat(ReturnsMath.correlation(a, new double[]{4, 3, 2, 1})).isCloseTo(-1.0, within(1e-12));
    }

    @Test
    void correlationNullWhenConstantOrTooFewSamples() {
        assertThat(ReturnsMath.correlation(new double[]{1, 2, 3}, new double[]{5, 5, 5})).isNull();
        assertThat(ReturnsMath.correlation(new double[]{5, 5, 5}, new double[]{1, 2, 3})).isNull();
        assertThat(ReturnsMath.correlation(new double[]{1}, new double[]{2})).isNull();
    }

    @Test
    void weightedSumIsElementwiseOverShortestSeries() {
        double[] out = ReturnsMath.weightedSum(
                new double[][]{{1, 2, 3}, {10, 20}}, new double[]{0.5, 0.5});

        assertThat(out).containsExactly(5.5, 11.0);
    }
}
