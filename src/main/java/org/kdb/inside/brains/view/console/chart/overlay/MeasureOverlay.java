package org.kdb.inside.brains.view.console.chart.overlay;

import com.intellij.ui.JBColor;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.kdb.inside.brains.view.console.chart.ChartColors;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MeasureOverlay extends AbstractOverlay implements Overlay, ChartMouseListener {
    private MeasureArea activeArea;

    private final ChartPanel panel;
    private final List<MeasureArea> pinnedAreas = new ArrayList<>();

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    static {
        NUMBER_FORMAT.setPositivePrefix("+");
        NUMBER_FORMAT.setNegativePrefix("-");
    }

    public MeasureOverlay(ChartPanel panel) {
        this.panel = panel;
        panel.addOverlay(this);
        panel.addChartMouseListener(this);
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (activeArea == null) {
            activeArea = new MeasureArea(calculateValuesPoint(event));
        } else {
            activeArea.finish = calculateValuesPoint(event);
            pinnedAreas.add(activeArea);
            activeArea = null;
        }
        fireOverlayChanged();
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
        if (activeArea != null) {
            activeArea.finish = calculateValuesPoint(event);
        }
    }

    private Point2D calculateValuesPoint(ChartMouseEvent event) {
        final JFreeChart chart = event.getChart();

        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis xAxis = plot.getDomainAxis();
        final ValueAxis yAxis = plot.getRangeAxis();
        final Rectangle2D dataArea = panel.getScreenDataArea();

        final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

        return new Point2D.Double(x, y);
    }

    private boolean isEmpty(Point2D point) {
        return point.getX() < 0;
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (activeArea == null && pinnedAreas.isEmpty()) {
            return;
        }

        final Shape savedClip = g2.getClip();

        final JFreeChart chart = chartPanel.getChart();
        final Rectangle2D screenArea = chartPanel.getScreenDataArea();
        final XYPlot plot = (XYPlot) chart.getPlot();

        g2.clip(screenArea);

        for (MeasureArea pinnedArea : pinnedAreas) {
            pinnedArea.draw(g2, plot, screenArea);
        }

        if (activeArea != null && activeArea.finish != null) {
            activeArea.draw(g2, plot, screenArea);
        }

        g2.setClip(savedClip);
    }

    public void cancel() {
        if (activeArea != null) {
            activeArea = null;
            fireOverlayChanged();
        }
    }

    private static class MeasureArea {
        private final Point2D start;
        private Point2D finish;

        private final Rectangle2D area = new Rectangle2D.Double();

        public MeasureArea(Point2D start) {
            this.start = start;
        }

        public void draw(Graphics2D g2, XYPlot plot, Rectangle2D screenArea) {
            updateDrawingArea(plot, screenArea);

            final double sy = start.getY();
            final double fy = finish.getY();
            final double diffVal = fy - sy;
            final double diffPrc = diffVal * 100d / Math.max(sy, fy);
            final Color foreground = sy < fy ? ChartColors.POSITIVE : ChartColors.NEGATIVE;
            final Color background = sy < fy ? ChartColors.POSITIVE_40 : ChartColors.NEGATIVE_40;
            final Color border = background.darker();

            g2.setStroke(new BasicStroke(2));
            g2.setPaint(background);
            g2.fill(area);
            g2.setPaint(border);
            g2.draw(area);

            g2.setPaint(foreground);
//            g2.setFont(g2.getFont().deriveFont(Font.BOLD));
            final float y = (float) (area.getY() + area.getHeight() / 2);
            TextUtils.drawAlignedString(NUMBER_FORMAT.format(diffVal), g2, (float) (area.getX() + area.getWidth()) + 5, y, TextAnchor.CENTER_LEFT);
            g2.setPaint(JBColor.foreground());
            TextUtils.drawAlignedString(NUMBER_FORMAT.format(diffPrc) + "%", g2, (float) (area.getX() + area.getWidth()) + 5, y + 15, TextAnchor.CENTER_LEFT);
        }

        private void updateDrawingArea(XYPlot plot, Rectangle2D screenArea) {
            final ValueAxis domain = plot.getDomainAxis();
            final ValueAxis range = plot.getRangeAxis();
            final RectangleEdge domainEdge = plot.getDomainAxisEdge();
            final RectangleEdge rangeEdge = plot.getRangeAxisEdge();
            final double x1 = domain.valueToJava2D(start.getX(), screenArea, domainEdge);
            final double y1 = range.valueToJava2D(start.getY(), screenArea, rangeEdge);
            final double x2 = domain.valueToJava2D(finish.getX(), screenArea, domainEdge);
            final double y2 = range.valueToJava2D(finish.getY(), screenArea, rangeEdge);
            if (y1 < y2) {
                if (x1 < x2) {
                    area.setRect(x1, y1, x2 - x1, y2 - y1);
                } else {
                    area.setRect(x2, y1, x1 - x2, y2 - y1);
                }
            } else {
                if (x1 < x2) {
                    area.setRect(x1, y2, x2 - x1, y1 - y2);
                } else {
                    area.setRect(x2, y2, x1 - x2, y1 - y2);
                }
            }
        }
    }
}