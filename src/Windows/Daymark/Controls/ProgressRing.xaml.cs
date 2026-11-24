using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace Daymark.Controls;

/// <summary>Lightweight circular progress indicator drawn directly, no timer or animation overhead.</summary>
public sealed class ProgressRing : UserControl
{
    public static readonly DependencyProperty PercentProperty =
        DependencyProperty.Register(nameof(Percent), typeof(int), typeof(ProgressRing),
            new FrameworkPropertyMetadata(0, FrameworkPropertyMetadataOptions.AffectsRender));

    public static readonly DependencyProperty RingBrushProperty =
        DependencyProperty.Register(nameof(RingBrush), typeof(Brush), typeof(ProgressRing),
            new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.AffectsRender));

    public int Percent
    {
        get => (int)GetValue(PercentProperty);
        set => SetValue(PercentProperty, value);
    }

    public Brush RingBrush
    {
        get => (Brush)GetValue(RingBrushProperty);
        set => SetValue(RingBrushProperty, value);
    }

    protected override void OnRender(DrawingContext dc)
    {
        base.OnRender(dc);
        double size = Math.Min(ActualWidth, ActualHeight);
        if (size <= 4) return;
        double thickness = Math.Max(6, size * 0.085);
        var center = new Point(ActualWidth / 2, ActualHeight / 2);
        var radius = (size - thickness) / 2;
        var track = (Brush)FindResource("SurfaceAlt") ?? Brushes.LightGray;
        dc.DrawEllipse(null, new Pen(track, thickness), center, radius, radius);

        int pct = Math.Max(0, Math.Min(100, Percent));
        if (pct == 0) return;
        if (pct == 100)
        {
            var fullBrush = RingBrush ?? (Brush)FindResource("Primary") ?? Brushes.Violet;
            dc.DrawEllipse(null, new Pen(fullBrush, thickness), center, radius, radius);
            return;
        }
        double sweep = pct * 3.6;
        var start = new Point(center.X, center.Y - radius);
        var end = PointAtAngle(center, radius, sweep - 90);
        var geo = new StreamGeometry();
        using (var ctx = geo.Open())
        {
            bool large = sweep > 180;
            ctx.BeginFigure(start, false, false);
            ctx.ArcTo(end, new Size(radius, radius), 0, large, SweepDirection.Clockwise, true, true);
        }
        var brush = RingBrush ?? (Brush)FindResource("Primary") ?? Brushes.Violet;
        dc.DrawGeometry(null, new Pen(brush, thickness) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round }, geo);
    }

    private static Point PointAtAngle(Point c, double r, double degrees)
    {
        double rad = degrees * Math.PI / 180.0;
        return new Point(c.X + r * Math.Cos(rad), c.Y + r * Math.Sin(rad));
    }
}
