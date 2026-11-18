package com.finrisk.util;

import java.util.Arrays;
import java.util.List;

public final class ReturnsMath {

    private ReturnsMath() {}

    public static double[] logReturns(List<Double> pricesOldestFirst) {
        int n = Math.max(pricesOldestFirst.size() - 1, 0);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            r[i] = Math.log(pricesOldestFirst.get(i + 1) / pricesOldestFirst.get(i));
        }
        return r;
    }

    public static double mean(double[] a) {
        double sum = 0;
        for (double v : a) sum += v;
        return sum / a.length;
    }

    public static double stdDev(double[] a) {
        if (a.length < 2) return 0.0;
        double m = mean(a), ss = 0;
        for (double v : a) ss += (v - m) * (v - m);
        return Math.sqrt(ss / (a.length - 1));
    }

    public static double covariance(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        if (n < 2) return 0.0;
        double ma = mean(Arrays.copyOf(a, n));
        double mb = mean(Arrays.copyOf(b, n));
        double s = 0;
        for (int i = 0; i < n; i++) s += (a[i] - ma) * (b[i] - mb);
        return s / (n - 1);
    }

    public static Double correlation(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        if (n < 2) return null;
        double sa = stdDev(Arrays.copyOf(a, n));
        double sb = stdDev(Arrays.copyOf(b, n));
        if (sa == 0 || sb == 0) return null;
        return covariance(a, b) / (sa * sb);
    }

    public static double[] weightedSum(double[][] seriesPerHolding, double[] weights) {
        int n = Integer.MAX_VALUE;
        for (double[] s : seriesPerHolding) n = Math.min(n, s.length);
        if (seriesPerHolding.length == 0) n = 0;
        double[] out = new double[n];
        for (int i = 0; i < seriesPerHolding.length; i++) {
            for (int t = 0; t < n; t++) out[t] += weights[i] * seriesPerHolding[i][t];
        }
        return out;
    }
}
