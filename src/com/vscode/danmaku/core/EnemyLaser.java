package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class EnemyLaser {
    public double startX, startY;
    public double targetX, targetY;
    public double endX, endY;
    
    private long startTime;
    private long warningDuration = 800_000_000L; // Reduced to 0.8s for performance
    private long activeDuration = 300_000_000L;  // Reduced to 0.3s for performance
    
    private boolean isActive = false;
    private boolean isFinished = false;

    // Cache polygon arrays to reduce GC pressure
    private final double[] polyX = new double[4];
    private final double[] polyY = new double[4];
    
    public EnemyLaser(double startX, double startY, double targetX, double targetY, long now) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.startTime = now;
        
        // Calculate endX, endY to extend far beyond the screen
        double dx = targetX - startX;
        double dy = targetY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) {
            dx = 0;
            dy = 1;
            length = 1;
        }
        // Extend to a large distance
        double extend = 2000;
        this.endX = startX + (dx / length) * extend;
        this.endY = startY + (dy / length) * extend;
    }

    public void update(long now) {
        long elapsed = now - startTime;
        if (elapsed < warningDuration) {
            isActive = false;
        } else if (elapsed < warningDuration + activeDuration) {
            isActive = true;
        } else {
            isFinished = true;
        }
    }

    public void draw(GraphicsContext gc) {
        if (isFinished) return;

        if (!isActive) {
            // Draw warning area (translucent fill) - Switched to Yellow for contrast
            gc.setFill(Color.web("#FFFF00", 0.15));
            double width = 10;
            double angle = Math.atan2(endY - startY, endX - startX);
            double dx = Math.sin(angle) * width / 2;
            double dy = Math.cos(angle) * width / 2;
            
            // Optimization: reuse arrays to reduce GC pressure
            polyX[0] = startX - dx; polyX[1] = startX + dx; polyX[2] = endX + dx; polyX[3] = endX - dx;
            polyY[0] = startY + dy; polyY[1] = startY - dy; polyY[2] = endY - dy; polyY[3] = endY + dy;
            gc.fillPolygon(polyX, polyY, 4);

            // Draw warning line (center line) - Switched to Yellow
            gc.setStroke(Color.web("#FFFF00", 0.8));
            gc.setLineWidth(2);
            gc.setLineDashes(15, 10);
            gc.strokeLine(startX, startY, endX, endY);
            gc.setLineDashes(0);
        } else {
            // Draw active laser
            gc.setStroke(Color.web("#FF0000", 0.8));
            gc.setLineWidth(10);
            gc.strokeLine(startX, startY, endX, endY);
            
            // Add a core white line for intensity
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeLine(startX, startY, endX, endY);
        }
    }

    public boolean collidesWithPlayer(double px, double py, double pw, double ph) {
        if (!isActive || isFinished) return false;

        // 檢查矩形的四個頂點以及中心點到雷射直線的距離
        double laserWidth = 10;
        double threshold = laserWidth / 2 + 2; // 雷射半寬度加上一點緩衝

        if (pointToLineDistance(px, py, startX, startY, endX, endY) < threshold) return true;
        if (pointToLineDistance(px + pw, py, startX, startY, endX, endY) < threshold) return true;
        if (pointToLineDistance(px, py + ph, startX, startY, endX, endY) < threshold) return true;
        if (pointToLineDistance(px + pw, py + ph, startX, startY, endX, endY) < threshold) return true;
        if (pointToLineDistance(px + pw / 2, py + ph / 2, startX, startY, endX, endY) < threshold) return true;
        
        return false;
    }

    private double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;
        if (len_sq != 0) param = dot / len_sq;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isFinished() { return isFinished; }
}
