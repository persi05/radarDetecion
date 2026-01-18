package com.example.demo;

class Blob {
    double meanX, meanY, stdX, stdY;
    int pixelCount;

    public Blob(double mx, double my, double sx, double sy, int pc) {
        meanX = mx; meanY = my;
        stdX = Math.max(sx, 1);
        stdY = Math.max(sy, 1);
        pixelCount = pc;
    }
}