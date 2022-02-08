package org.kdb.inside.brains.view.console.chart.overlay;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCrosshairLabelGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

public class MovingCrosshairOverlay implements ChartMouseListener {
    private final ChartPanel panel;

    private final Crosshair xCrosshair;
    private final Crosshair yCrosshair;

    public static final JBColor CROSSHAIR_PAINT = new JBColor(new Color(0xa4a4a5), new Color(0xa4a4a5));
    public static final JBColor CROSSHAIR_LABEL = new JBColor(new Color(0x595959), new Color(0x595959));
    public static final JBColor CROSSHAIR_OUTLINE = new JBColor(new Color(0xe0e0e0), new Color(0xe0e0e0));
    public static final JBColor CROSSHAIR_BACKGROUND = new JBColor(new Color(0xc4c4c4), new Color(0xc4c4c4));

    public MovingCrosshairOverlay(ChartPanel panel) {
        this.panel = panel;

        xCrosshair = createCrosshair(false);
        yCrosshair = createCrosshair(true);

        final CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        crosshairOverlay.addDomainCrosshair(this.xCrosshair);
        crosshairOverlay.addRangeCrosshair(this.yCrosshair);

        panel.addOverlay(crosshairOverlay);
        panel.addChartMouseListener(this);
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
        final Rectangle2D dataArea = panel.getScreenDataArea();
        final JFreeChart chart = event.getChart();
        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis xAxis = plot.getDomainAxis();
        final ValueAxis yAxis = plot.getRangeAxis();

        double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        if (!xAxis.getRange().contains(x)) {
            x = Double.NaN;
        }
        double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
        if (!yAxis.getRange().contains(y)) {
            y = Double.NaN;
        }
        xCrosshair.setValue(x);
        yCrosshair.setValue(y);
    }

    @NotNull
    private Crosshair createCrosshair(boolean vertical) {
        final Crosshair crosshair = new Crosshair(Double.NaN, CROSSHAIR_PAINT, new BasicStroke(0.5F));
        crosshair.setLabelVisible(true);
        crosshair.setLabelAnchor(vertical ? RectangleAnchor.LEFT : RectangleAnchor.BOTTOM);
        crosshair.setLabelPaint(CROSSHAIR_LABEL);
        crosshair.setLabelOutlinePaint(CROSSHAIR_OUTLINE);
        crosshair.setLabelBackgroundPaint(CROSSHAIR_BACKGROUND);
        final ValueAxis domainAxis = ((XYPlot) panel.getChart().getPlot()).getDomainAxis();
        if (!vertical && domainAxis instanceof DateAxis) {
            final DateAxis dateAxis = (DateAxis) domainAxis;
            crosshair.setLabelGenerator(g -> dateAxis.getTickUnit().valueToString(g.getValue()));
        } else {
            crosshair.setLabelGenerator(new StandardCrosshairLabelGenerator("  {0}  ", NumberFormat.getNumberInstance()));
        }

        return crosshair;
    }
}