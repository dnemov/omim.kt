package com.mapswithme.maps.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Handler
import android.os.Message

import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.LocationUtils

internal class SensorHelper : SensorEventListener {
    private val mSensorManager: SensorManager?
    private var mAccelerometer: Sensor? = null
    private var mMagnetometer: Sensor? = null
    private var mMagneticField: GeomagneticField? = null
    private val mSensorData: SensorData
    private val mR = FloatArray(9)
    private val mI = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private val mHandler: Handler

    override fun onSensorChanged(event: SensorEvent) {
        if (!MwmApplication.get().arePlatformAndCoreInitialized())
            return

        if (mSensorData.isAbsent) {
            notifyImmediately(event)
            return
        }

        val type = SensorType[event]

        if (type !== SensorType.TYPE_MAGNETIC_FIELD) {
            notifyImmediately(event)
            return
        }

        if (!mHandler.hasMessages(MARKER)) {
            notifyImmediately(event)
            addRateLimitMessage()
        }
    }

    private fun addRateLimitMessage() {
        val message = Message.obtain()
        message.what = MARKER
        mHandler.sendMessageDelayed(message, AGGREGATION_TIMEOUT_IN_MILLIS.toLong())
    }

    private fun notifyImmediately(event: SensorEvent) {
        val sensorType = SensorType[event]
        sensorType.updateData(mSensorData, event)

        val hasOrientation = hasOrientation()

        if (hasOrientation)
            notifyInternal(event)
    }

    private fun hasOrientation(): Boolean {
        if (mSensorData.isAbsent)
            return false

        val isSuccess = SensorManager.getRotationMatrix(
            mR, mI, mSensorData.gravity,
            mSensorData.geomagnetic
        )
        if (isSuccess)
            SensorManager.getOrientation(mR, mOrientation)

        return isSuccess
    }

    private fun notifyInternal(event: SensorEvent) {
        var trueHeading = -1.0
        var offset = -1.0
        val magneticHeading = LocationUtils.correctAngle(mOrientation[0].toDouble(), 0.0)

        if (mMagneticField != null) {
            // Positive 'offset' means the magnetic field is rotated east that match from true north
            offset = Math.toRadians(mMagneticField!!.declination.toDouble())
            trueHeading = LocationUtils.correctAngle(magneticHeading, offset)
        }

        LocationHelper.INSTANCE.notifyCompassUpdated(event.timestamp, magneticHeading, trueHeading, offset)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    init {
        mSensorManager = MwmApplication.get().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }
        mSensorData = SensorData()
        mHandler = Handler()
    }

    fun start() {
        if (mAccelerometer != null)
            mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI)

        if (mMagnetometer != null)
            mSensorManager!!.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        mMagneticField = null
        mSensorManager?.unregisterListener(this)
    }

    fun resetMagneticField(oldLocation: Location?, newLocation: Location) {
        if (mSensorManager == null)
            return

        // Recreate magneticField if location has changed significantly
        if (mMagneticField == null || oldLocation == null ||
            newLocation.distanceTo(oldLocation) > DISTANCE_TO_RECREATE_MAGNETIC_FIELD_M
        ) {
            mMagneticField = GeomagneticField(
                newLocation.latitude.toFloat(),
                newLocation.longitude.toFloat(),
                newLocation.altitude.toFloat(),
                newLocation.time
            )
        }
    }

    internal enum class SensorType private constructor(private val mAccelerometer: Int = SensorType.INVALID_ID) {
        TYPE_ACCELEROMETER(Sensor.TYPE_ACCELEROMETER) {
            override fun updateDataInternal(data: SensorData, params: FloatArray) {
                data.gravity = params
            }
        },
        TYPE_MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD) {
            override fun updateDataInternal(data: SensorData, params: FloatArray) {
                data.geomagnetic = params
            }
        },
        DEFAULT {
            override fun updateDataInternal(data: SensorData, params: FloatArray) {
                /* Do nothing */
            }

            override fun getSensorParam(event: SensorEvent): FloatArray {
                return floatArrayOf()
            }
        };

        protected open fun getSensorParam(event: SensorEvent): FloatArray {
            return LocationHelper.nativeUpdateCompassSensor(ordinal, event.values)!!
        }

        fun updateData(data: SensorData, event: SensorEvent) {
            updateDataInternal(data, getSensorParam(event))
        }

        abstract fun updateDataInternal(data: SensorData, params: FloatArray)

        companion object {

            const val INVALID_ID = -0x8c204

            operator fun get(value: SensorEvent): SensorType {
                for (each in values()) {
                    if (each.mAccelerometer == value.sensor.type)
                        return each
                }
                return DEFAULT
            }
        }
    }

    companion object {
        private val DISTANCE_TO_RECREATE_MAGNETIC_FIELD_M = 1000f
        private val MARKER = 0x39867
        private val AGGREGATION_TIMEOUT_IN_MILLIS = 10
    }
}
