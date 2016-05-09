package net.lorenzobianconi.sensortester;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class SensorTester extends Activity implements SensorEventListener {
    private static final int HISTORY_SIZE = 10;

    private XYPlot sensorPlot;
    private SimpleXYSeries xSensorDataSeries;
    private SimpleXYSeries ySensorDataSeries;
    private SimpleXYSeries zSensorDataSeries;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_tester);

        sensorPlot = (XYPlot) findViewById(R.id.sensor_plot);
        sensorPlot.setRangeBoundaries(-20, 20, BoundaryMode.FIXED);
        sensorPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);

        xSensorDataSeries = new SimpleXYSeries(
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "X data");
        xSensorDataSeries.useImplicitXVals();

        LineAndPointFormatter xFormat = new LineAndPointFormatter();
        xFormat.setPointLabelFormatter(new PointLabelFormatter());
        xFormat.configure(getApplicationContext(), R.xml.x_point_formatter);

        ySensorDataSeries = new SimpleXYSeries(
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Y data");
        ySensorDataSeries.useImplicitXVals();

        LineAndPointFormatter yFormat = new LineAndPointFormatter();
        yFormat.setPointLabelFormatter(new PointLabelFormatter());
        yFormat.configure(getApplicationContext(), R.xml.y_point_formatter);

        zSensorDataSeries = new SimpleXYSeries(
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Z data");
        zSensorDataSeries.useImplicitXVals();

        LineAndPointFormatter zFormat = new LineAndPointFormatter();
        zFormat.setPointLabelFormatter(new PointLabelFormatter());
        zFormat.configure(getApplicationContext(), R.xml.z_point_formatter);

        sensorPlot.addSeries(xSensorDataSeries, xFormat);
        sensorPlot.addSeries(ySensorDataSeries, yFormat);
        sensorPlot.addSeries(zSensorDataSeries, zFormat);

        mSensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor =
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public final void onSensorChanged(SensorEvent event) {
        if (xSensorDataSeries.size() > HISTORY_SIZE) {
            xSensorDataSeries.removeFirst();
            ySensorDataSeries.removeFirst();
            zSensorDataSeries.removeFirst();
        }
        xSensorDataSeries.addLast(null, event.values[0]);
        ySensorDataSeries.addLast(null, event.values[1]);
        zSensorDataSeries.addLast(null, event.values[2]);

        sensorPlot.redraw();
    }

    protected void onResume() {
        super.onResume();

        if (mSensor != null)
            mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();

        if (mSensor != null)
            mSensorManager.unregisterListener(this);
    }
}
