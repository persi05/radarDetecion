package com.example.demo;

import java.util.*;
import javafx.scene.paint.Color;

class Target {
    int id;
    double x, y, vx, vy;
    Color color;
    List<Pt> history = new ArrayList<>();

    static class Pt {
        double x, y;
        Pt(double x, double y) { this.x = x; this.y = y; }
    }

    public Target(int id, double x, double y, double vx, double vy, Color c) {
        this.id = id; this.x = x; this.y = y;
        this.vx = vx; this.vy = vy; color = c;
    }

    public void update(int w, int h) {
        history.add(new Pt(x, y));
        if (history.size() > 50) history.remove(0);
        x += vx; y += vy;
        if (x < 20 || x > w - 20) { vx = -vx; x = Math.max(20, Math.min(w - 20, x)); }
        if (y < 20 || y > h - 20) { vy = -vy; y = Math.max(20, Math.min(h - 20, y)); }
    }
}