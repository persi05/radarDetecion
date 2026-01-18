package com.example.demo;

import java.util.*;

class RadarProc {
    static Random rand = new Random();

    public static int otsu(int[] hist, double mod) {
        int total = 0;
        for (int c : hist) total += c;
        double sum = 0;
        for (int i = 0; i < hist.length; i++) sum += i * hist[i];
        double sumB = 0;
        int wB = 0, wF, thresh = 0;
        double maxVar = 0;
        for (int t = 0; t < hist.length; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            wF = total - wB;
            if (wF == 0) break;
            sumB += t * hist[t];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double var = (double) wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) { maxVar = var; thresh = t; }
        }
        return (int) (thresh * mod);
    }

    public static int[] ccl(int[] bin, int w, int h) {
        int[] labels = new int[w * h];
        Map<Integer, Integer> eq = new HashMap<>();
        int next = 1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (bin[idx] == 1) {
                    List<Integer> nl = new ArrayList<>();
                    if (x > 0 && y > 0 && labels[(y-1)*w+(x-1)] > 0) nl.add(labels[(y-1)*w+(x-1)]);
                    if (y > 0 && labels[(y-1)*w+x] > 0) nl.add(labels[(y-1)*w+x]);
                    if (x < w-1 && y > 0 && labels[(y-1)*w+(x+1)] > 0) nl.add(labels[(y-1)*w+(x+1)]);
                    if (x > 0 && labels[y*w+(x-1)] > 0) nl.add(labels[y*w+(x-1)]);

                    if (nl.isEmpty()) {
                        labels[idx] = next;
                        eq.put(next, next);
                        next++;
                    } else {
                        int min = Collections.min(nl);
                        labels[idx] = min;
                        for (int n : nl) {
                            int r1 = findRoot(eq, min);
                            int r2 = findRoot(eq, n);
                            if (r1 != r2) eq.put(Math.max(r1, r2), Math.min(r1, r2));
                        }
                    }
                }
            }
        }
        for (int i = 0; i < labels.length; i++)
            if (labels[i] > 0) labels[i] = findRoot(eq, labels[i]);
        return labels;
    }

    static int findRoot(Map<Integer, Integer> eq, int l) {
        while (eq.containsKey(l) && eq.get(l) != l) l = eq.get(l);
        return l;
    }

    public static List<Blob> getBlobs(int[] labels, int w, int h) {
        Map<Integer, List<int[]>> bm = new HashMap<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int l = labels[idx];
                if (l > 0) bm.computeIfAbsent(l, k -> new ArrayList<>()).add(new int[]{x, y});
            }
        }

        List<Blob> blobs = new ArrayList<>();
        for (List<int[]> px : bm.values()) {
            int n = px.size();
            if (n < 15) continue;
            double mx = px.stream().mapToDouble(p -> p[0]).average().orElse(0);
            double my = px.stream().mapToDouble(p -> p[1]).average().orElse(0);
            double vx = px.stream().mapToDouble(p -> Math.pow(p[0] - mx, 2)).sum() / n;
            double vy = px.stream().mapToDouble(p -> Math.pow(p[1] - my, 2)).sum() / n;
            blobs.add(new Blob(mx, my, Math.sqrt(vx), Math.sqrt(vy), n));
        }
        return blobs;
    }

    public static double gauss(double x, double mu, double sig) {
        return Math.exp(-0.5 * Math.pow((x - mu) / sig, 2)) / (sig * Math.sqrt(2 * Math.PI));
    }

    public static double assocProb(Blob m, Target t) {
        double dx = m.meanX - t.x;
        double dy = m.meanY - t.y;
        double sx = 5 + Math.abs(t.vx) * 2;
        double sy = 5 + Math.abs(t.vy) * 2;
        return gauss(dx, 0, sx) * gauss(dy, 0, sy);
    }

    public static AssociationNode buildTree(List<Blob> meas, List<Target> tgts,
                                            int idx, AssociationNode parent, Set<Integer> used) {
        if (parent == null) {
            parent = new AssociationNode(null, null, 1.0, false);
            used = new HashSet<>();
        }

        if (idx >= meas.size() || idx >= Math.min(meas.size(), 5)) return parent;

        Blob m = meas.get(idx);

        List<Integer> candidates = new ArrayList<>();
        List<Double> probs = new ArrayList<>();

        for (int i = 0; i < tgts.size(); i++) {
            if (!used.contains(i)) {
                double p = assocProb(m, tgts.get(i));
                if (p > 0.01) { // Próg odcięcia
                    candidates.add(i);
                    probs.add(p);
                }
            }
        }

        for (int i = 0; i < candidates.size() - 1; i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                if (probs.get(j) > probs.get(i)) {
                    Collections.swap(candidates, i, j);
                    Collections.swap(probs, i, j);
                }
            }
        }

        int limit = Math.min(2, candidates.size());
        for (int i = 0; i < limit; i++) {
            int targetIdx = candidates.get(i);
            double prob = probs.get(i);

            AssociationNode child = new AssociationNode(idx, targetIdx, prob, false);
            parent.addChild(child);
            Set<Integer> newUsed = new HashSet<>(used);
            newUsed.add(targetIdx);
            buildTree(meas, tgts, idx + 1, child, newUsed);
        }

        if (candidates.isEmpty() || probs.get(0) < 0.2) {
            AssociationNode clutter = new AssociationNode(idx, null, 0.1, true);
            parent.addChild(clutter);
            buildTree(meas, tgts, idx + 1, clutter, new HashSet<>(used));
        }

        return parent;
    }

    public static int[] genRadar(List<Target> tgts, int w, int h, int noise) {
        int[] img = new int[w * h];
        for (int i = 0; i < img.length; i++) img[i] = rand.nextInt(noise);

        for (Target t : tgts) {
            int cx = (int) t.x;
            int cy = (int) t.y;
            int r = 8;
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d <= r) {
                        int x = cx + dx;
                        int y = cy + dy;
                        if (x >= 0 && x < w && y >= 0 && y < h) {
                            int val = (int) (200 * Math.exp(-(d * d) / 18));
                            img[y * w + x] = Math.min(255, img[y * w + x] + val);
                        }
                    }
                }
            }
        }
        return img;
    }
}