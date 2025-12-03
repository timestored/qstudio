package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class SparkRenderer implements TableCellRenderer {
    private final JComponent comp;

    // add to enum to provide renderer 
    public static enum SparkType { 
    	SPARKLINE, 
    	SPARKAREA,
    	SPARKBAR, 
    	SPARKDISCRETE, 
    	SPARKPIE, 
    	SPARKBOXPLOT, 
    	SPARKBULLET;
    	
    	 JComponent getRenderer() {
			switch(this) {
			case SPARKLINE: return new SparkLineAreaComponent(false);
			case SPARKAREA: return new SparkLineAreaComponent(true);
			case SPARKBAR: return new SparkbarComponent();
			case SPARKDISCRETE: return new SparkDiscrete();
			case SPARKPIE: return new SparkPie();
			case SPARKBOXPLOT: return new SparkBoxPlot();
			case SPARKBULLET: return new SparkBullet();
			default: throw new IllegalArgumentException("Unknown spark type: "+this);
			}
		}
    }
    
    public SparkRenderer(SparkType sparkType) {
        this.comp = sparkType.getRenderer();
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int col) {

        double[] arr = extractNumericArray(value);
        if(comp instanceof SparkBase) ((SparkBase)comp).setData(arr);

        if (isSelected) {
            comp.setBackground(table.getSelectionBackground());
        } else {
            comp.setBackground(table.getBackground());
        }

        return comp;
    }


    
    static class SparkbarComponent extends SparkBase {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.length == 0) return;

            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double min = Arrays.stream(data).min().orElse(0);
            double max = Arrays.stream(data).max().orElse(0);

            boolean hasPos = max > 0;
            boolean hasNeg = min < 0;

            int baseY;

            if (hasPos && hasNeg) {
                // Mixed → baseline in the MIDDLE
                baseY = h / 2;
            } else if (hasPos) {
                // All positive → baseline at BOTTOM
                baseY = h - 1;
            } else {
                // All negative → baseline at TOP
                baseY = 0;
            }

            // Scaling
            double absMax = Math.max(Math.abs(min), Math.abs(max));
            if (absMax == 0) absMax = 1;

            int barW = Math.max(1, w / data.length);

            for (int i = 0; i < data.length; i++) {
                double v = data[i];

                int barHeight = (int) Math.round(Math.abs(v) / absMax * h);

                int x = i * barW;

                if (v >= 0) {
                    // Positive → draw UP from baseline
                    int y = baseY - barHeight;
                    g2.setColor(new Color(80, 180, 80));
                    g2.fillRect(x, y, barW - 1, barHeight);
                } else {
                    // Negative → draw DOWN from baseline
                    int y = baseY;
                    g2.setColor(new Color(200, 70, 70));
                    g2.fillRect(x, y, barW - 1, barHeight);
                }
            }

            // Draw baseline ONLY for mixed positive + negative
            if (hasPos && hasNeg) {
                g2.setColor(new Color(100, 100, 100, 140));
                g2.drawLine(0, baseY, w, baseY);
            }

            g2.dispose();
        }
    }
    
    
    static class SparkLineAreaComponent extends SparkBase {

        private final boolean fillArea;

        public SparkLineAreaComponent(boolean fillArea) {
            this.fillArea = fillArea;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.length < 2) return;

            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double min = Arrays.stream(data).min().orElse(0);
            double max = Arrays.stream(data).max().orElse(1);
            double range = max - min == 0 ? 1 : max - min;

            int[] xs = new int[data.length];
            int[] ys = new int[data.length];

            // Precompute points
            for (int i = 0; i < data.length; i++) {
                xs[i] = (int) ((i / (data.length - 1.0)) * (w - 1));
                ys[i] = h - (int)(((data[i] - min) / range) * h);
            }

            Color base = new Color(90, 140, 255);

            // ---- OPTION: Fill area under line ----
            if (fillArea) {
                Polygon p = new Polygon();
                for (int i = 0; i < xs.length; i++) {
                    p.addPoint(xs[i], ys[i]);
                }
                p.addPoint(xs[xs.length - 1], h);
                p.addPoint(xs[0], h);

                Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), 70);
                g2.setColor(fill);
                g2.fillPolygon(p);
            }

            // ---- Draw line ----
            g2.setColor(base);
            for (int i = 1; i < xs.length; i++) {
                g2.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            }

            g2.dispose();
        }
    }

    
    static abstract class SparkBase extends JComponent {
        double[] data=new double[0];
        void setData(double[] d){ data=d==null?new double[0]:d; }
    }
    static class SparkDiscrete extends SparkBase {
        protected void paintComponent(Graphics g){
            int w=getWidth(), h=getHeight();
            if(data.length==0)return;
            Graphics2D g2=(Graphics2D)g; int bw=Math.max(1,w/data.length);
            double avg=Arrays.stream(data).average().orElse(0);
            for(int i=0;i<data.length;i++){
                boolean up=data[i]>=avg;
                g2.setColor(up?new Color(80,180,80):new Color(200,70,70));
                int y=up?h/4:h/2; int bh=h/2;
                g2.fillRect(i*bw,y,bw-1,bh);
            }
        }
    }
    
    static class SparkPie extends SparkBase {
        protected void paintComponent(Graphics g){
            if(data.length==0)return;
            Graphics2D g2=(Graphics2D)g.create(); int w=getWidth(),h=getHeight(),sz=Math.min(w,h)-2;
            double sum=Arrays.stream(data).sum(); if(sum<=0)sum=1;
            int start=0;
            for(int i=0;i<data.length;i++){
                float hue=(float)(i/(double)data.length);
                g2.setColor(Color.getHSBColor(hue,0.6f,0.9f));
                int arc=(int)Math.round((data[i]/sum)*360);
                g2.fillArc(1,1,sz,sz,start,arc);
                start+=arc;
            }
            g2.dispose();
        }
    }
    
    static class SparkBoxPlot extends SparkBase {
        @Override
        protected void paintComponent(Graphics g){
            if(data.length < 2) return;

            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();

            double[] d = Arrays.stream(data).sorted().toArray();
            double min = d[0], max = d[d.length-1];
            double q1  = d[d.length/4];
            double q3  = d[(d.length*3)/4];
            double med = d[d.length/2];
            double range = max - min == 0 ? 1 : max - min;

            // inline version of map(v) = (v - min) / range * (w - 4) + 2
            int x1   = (int) (((q1  - min) / range) * (w - 4)) + 2;
            int x3   = (int) (((q3  - min) / range) * (w - 4)) + 2;
            int xm   = (int) (((med - min) / range) * (w - 4)) + 2;
            int xmin = (int) (((min - min) / range) * (w - 4)) + 2; // will be 2
            int xmax = (int) (((max - min) / range) * (w - 4)) + 2; // will be w-2

            int mid  = h / 2;
            int boxH = h / 3;

            g2.setColor(Color.GRAY);
            g2.drawLine(xmin, mid, xmax, mid); // whiskers

            g2.setColor(new Color(200, 200, 250));
            g2.fillRect(x1, mid - boxH/2, (x3 - x1), boxH); // box

            g2.setColor(Color.BLACK);
            g2.drawLine(xm, mid - boxH/2, xm, mid + boxH/2); // median line
        }
    }



    static class SparkBullet extends SparkBase {
        protected void paintComponent(Graphics g){
            if(data.length==0)return;
            Graphics2D g2=(Graphics2D)g; int w=getWidth(), h=getHeight();
            double max=data.length>2?data[2]:Arrays.stream(data).max().orElse(1); if(max==0)max=1;
            double val=data[0], tgt=data.length>1?data[1]:Double.NaN;
            int bgW=w, bgY=h/4, bgH=h/2;
            g2.setColor(new Color(180,180,180)); g2.fillRect(0,bgY,bgW,bgH);
            int vW=(int)(Math.min(val/max,1)*w); g2.setColor(Color.BLACK); g2.fillRect(0,bgY,vW,bgH);
            if(!Double.isNaN(tgt)){ int x=(int)(Math.min(tgt/max,1)*w); g2.setColor(Color.RED); g2.drawLine(x,bgY-2,x,bgY+bgH+2); }
        }
    }

    
    static double[] extractNumericArray(Object v) {
        if (v == null) return new double[0];

        if (v instanceof double[]) return (double[]) v;

        if (v instanceof float[]) {
            float[] a = (float[]) v; double[] o = new double[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }

        if (v instanceof long[]) {
            long[] a = (long[]) v; double[] o = new double[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }

        if (v instanceof int[]) {
            int[] a = (int[]) v; double[] o = new double[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }

        if (v instanceof Number[]) {
            Number[] a = (Number[]) v; double[] o = new double[a.length];
            for (int i = 0; i < a.length; i++) o[i] = (a[i] == null ? Double.NaN : a[i].doubleValue());
            return o;
        }

        if (v instanceof List<?>) {
            return ((List<?>) v).stream()
                .filter(o -> o instanceof Number)
                .mapToDouble(o -> ((Number) o).doubleValue())
                .toArray();
        }

        if (v instanceof Vector<?>) {
            return ((Vector<?>) v).stream()
                .filter(o -> o instanceof Number)
                .mapToDouble(o -> ((Number) o).doubleValue())
                .toArray();
        }

        return new double[0];
    }


}
