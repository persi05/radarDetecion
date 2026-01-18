package com.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.*;

public class RadarTrackingSystem extends Application {
    static final int W = 600, H = 600;
    static final Random rand = new Random();

    Canvas canvas;
    GraphicsContext gc;
    List<Target> targets = new ArrayList<>();
    List<Blob> detections = new ArrayList<>();
    AssociationNode bestHyp;

    Timer timer;
    int frame = 0;
    boolean running = false;

    Slider numTgtSlider, noiseSlider, otsuSlider;
    Label statsLbl, numTgtLbl, noiseLbl, otsuLbl;
    TextArea hypArea;
    Button startBtn;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        canvas = new Canvas(W, H);
        gc = canvas.getGraphicsContext2D();

        Label title = new Label("WIZUALIZACJA RADAROWA");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        VBox centerBox = new VBox(10, title, canvas);
        centerBox.setPadding(new Insets(10));
        centerBox.setStyle("-fx-background-color: #2d2d2d;");
        root.setCenter(centerBox);

        root.setLeft(createControls());

        root.setRight(createStats());

        Scene scene = new Scene(root, 1400, 700);
        stage.setTitle("System Śledzenia Obiektów Radarowych 2D");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> { if (timer != null) timer.cancel(); });
        stage.show();

        initTargets();
        drawFrame();
    }

    VBox createControls() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setPrefWidth(300);
        box.setStyle("-fx-background-color: #2d2d2d;");

        Label title = new Label("PARAMETRY");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        numTgtLbl = new Label("Liczba celów: 3");
        numTgtLbl.setStyle("-fx-text-fill: white;");
        numTgtSlider = new Slider(1, 10, 3);
        numTgtSlider.setShowTickMarks(true);
        numTgtSlider.setMajorTickUnit(2);
        numTgtSlider.valueProperty().addListener((o, ol, nw) -> {
            numTgtLbl.setText("Liczba celów: " + nw.intValue());
            if (!running) { initTargets(); drawFrame(); }
        });

        noiseLbl = new Label("Szum: 30");
        noiseLbl.setStyle("-fx-text-fill: white;");
        noiseSlider = new Slider(10, 60, 30);
        noiseSlider.setShowTickMarks(true);
        noiseSlider.setMajorTickUnit(10);
        noiseSlider.valueProperty().addListener((o, ol, nw) ->
                noiseLbl.setText("Szum: " + nw.intValue()));

        otsuLbl = new Label("Otsu: 1.2");
        otsuLbl.setStyle("-fx-text-fill: white;");
        otsuSlider = new Slider(0.5, 2.0, 1.2);
        otsuSlider.setShowTickMarks(true);
        otsuSlider.setMajorTickUnit(0.5);
        otsuSlider.valueProperty().addListener((o, ol, nw) ->
                otsuLbl.setText(String.format("Otsu: %.1f", nw.doubleValue())));

        startBtn = new Button("START");
        startBtn.setPrefWidth(260);
        startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");
        startBtn.setOnAction(e -> toggle());

        Button resetBtn = new Button("RESET");
        resetBtn.setPrefWidth(260);
        resetBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14;");
        resetBtn.setOnAction(e -> {
            if (running) toggle();
            initTargets();
            drawFrame();
        });

        box.getChildren().addAll(title,
                numTgtLbl, numTgtSlider,
                noiseLbl, noiseSlider,
                otsuLbl, otsuSlider,
                startBtn, resetBtn);
        return box;
    }

    VBox createStats() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setPrefWidth(350);
        box.setStyle("-fx-background-color: #2d2d2d;");

        Label title = new Label("STATYSTYKI");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        statsLbl = new Label();
        statsLbl.setStyle("-fx-text-fill: #0f0; -fx-font-family: monospace;");

        Label hypTitle = new Label("NAJLEPSZA HIPOTEZA:");
        hypTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        hypArea = new TextArea();
        hypArea.setEditable(false);
        hypArea.setPrefHeight(400);
        hypArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #0f0; -fx-font-family: monospace;");

        box.getChildren().addAll(title, statsLbl, hypTitle, hypArea);
        return box;
    }

    void initTargets() {
        targets.clear();
        int n = (int) numTgtSlider.getValue();
        for (int i = 0; i < n; i++) {
            double x = rand.nextDouble() * (W - 100) + 50;
            double y = rand.nextDouble() * (H - 100) + 50;
            double vx = (rand.nextDouble() - 0.5) * 2;
            double vy = (rand.nextDouble() - 0.5) * 2;
            Color c = Color.hsb(i * 360.0 / n, 0.8, 1.0);
            targets.add(new Target(i, x, y, vx, vy, c));
        }
        frame = 0;
    }

    void toggle() {
        if (running) {
            timer.cancel();
            timer = null;
            startBtn.setText("START");
            startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    Platform.runLater(() -> {
                        for (Target t : targets) t.update(W, H);
                        drawFrame();
                        frame++;
                    });
                }
            }, 0, 100);
            startBtn.setText("STOP");
            startBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 14;");
        }
        running = !running;
    }

    void drawFrame() {
        int noise = (int) noiseSlider.getValue();
        double otsuMod = otsuSlider.getValue();

        int[] img = RadarProc.genRadar(targets, W, H, noise);

        int[] hist = new int[256];
        for (int v : img) hist[v]++;
        int thresh = RadarProc.otsu(hist, otsuMod);

        int[] bin = new int[img.length];
        for (int i = 0; i < img.length; i++) bin[i] = img[i] > thresh ? 1 : 0;

        int[] labels = RadarProc.ccl(bin, W, H);
        List<Blob> allBlobs = RadarProc.getBlobs(labels, W, H);

        detections = new ArrayList<>();
        for (Blob b : allBlobs) {
            if (b.pixelCount > 15) {
                detections.add(b);
            }
        }

        if (detections.size() > 10) {
            detections.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
            detections = detections.subList(0, 10);
        }

        if (!detections.isEmpty() && !targets.isEmpty()) {
            AssociationNode tree = RadarProc.buildTree(detections, targets, 0, null, new HashSet<>());
            List<AssociationNode> leaves = new ArrayList<>();
            tree.collectLeaves(leaves);
            leaves.sort((a, b) -> Double.compare(b.getTotalProbability(), a.getTotalProbability()));
            bestHyp = leaves.isEmpty() ? null : leaves.get(0);
            updateHypDisplay(leaves);
        } else {
            bestHyp = null;
            hypArea.setText("Brak detekcji");
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, W, H);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                gc.setFill(Color.gray(img[y * W + x] / 255.0));
                gc.fillRect(x, y, 1, 1);
            }
        }

        for (Target t : targets) {
            gc.setStroke(t.color);
            gc.setLineWidth(1);
            gc.setGlobalAlpha(0.5);
            if (t.history.size() > 1) {
                gc.beginPath();
                gc.moveTo(t.history.get(0).x, t.history.get(0).y);
                for (Target.Pt p : t.history) gc.lineTo(p.x, p.y);
                gc.stroke();
            }
            gc.setGlobalAlpha(1.0);
        }

        for (Blob b : detections) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);
            double r = Math.max(b.stdX, b.stdY) * 2;
            gc.strokeOval(b.meanX - r, b.meanY - r, r * 2, r * 2);
        }

        if (bestHyp != null) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.setGlobalAlpha(0.7);
            for (AssociationNode n : bestHyp.getPath()) {
                if (!n.isClutter() && n.getTarget() != null) {
                    Blob b = detections.get(n.getMeasurement());
                    Target t = targets.get(n.getTarget());
                    gc.strokeLine(b.meanX, b.meanY, t.x, t.y);
                }
            }
            gc.setGlobalAlpha(1.0);
        }

        for (Target t : targets) {
            gc.setFill(t.color);
            gc.fillOval(t.x - 6, t.y - 6, 12, 12);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(t.x - 6, t.y - 6, 12, 12);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(10));
            gc.fillText("T" + t.id, t.x + 10, t.y - 5);
        }

        updateStats(thresh);
    }

    void updateStats(int thresh) {
        int nHyp = 0;
        if (bestHyp != null) {
            List<AssociationNode> leaves = new ArrayList<>();
            AssociationNode root = bestHyp;
            while (root.getParent() != null) root = root.getParent();
            root.collectLeaves(leaves);
            nHyp = leaves.size();
        }

        statsLbl.setText(String.format(
                "Klatka: %d\nPróg: %d\nCele: %d\nDetekcje: %d\nHipotezy: %d\nStatus: %s",
                frame, thresh, targets.size(), detections.size(), nHyp, running ? "DZIAŁA" : "STOP"
        ));
    }

    void updateHypDisplay(List<AssociationNode> leaves) {
        if (leaves.isEmpty()) {
            hypArea.setText("Brak hipotez");
            return;
        }

        StringBuilder sb = new StringBuilder();
        AssociationNode best = leaves.get(0);
        sb.append(String.format("P = %.6f\n\n", best.getTotalProbability()));

        for (AssociationNode n : best.getPath()) {
            sb.append(String.format("Z%d → %s (%.4f)\n",
                    n.getMeasurement(),
                    n.isClutter() ? "Clutter" : "T" + n.getTarget(),
                    n.getProbability()));
        }

        sb.append("\n\nTop 10:\n");
        for (int i = 0; i < Math.min(10, leaves.size()); i++) {
            sb.append(String.format("%2d. P=%.6f\n", i+1, leaves.get(i).getTotalProbability()));
        }

        hypArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}