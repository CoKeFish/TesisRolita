package com.example.tesisrolita

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Environment
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date

class DataSensorManager : SensorEventListener {
    //region variables sensores

    //Variables del acelerometro
    private var lastX: Float = 0.0f
    private var lastY: Float = 0.0f
    private var lastZ: Float = 0.0f

    //Variables del giroscopio
    private var lastGyroX: Float = 0.0f
    private var lastGyroY: Float = 0.0f
    private var lastGyroZ: Float = 0.0f

    //Variables de las componentes gravitacionales
    private var lastGravityX: Float = 0.0f
    private var lastGravityY: Float = 0.0f
    private var lastGravityZ: Float = 0.0f

    //Mognetometro, o Orientacion
    private var lastOrientationX: Float = 0.0f
    private var lastOrientationY: Float = 0.0f
    private var lastOrientationZ: Float = 0.0f

    //Posicion GPS
    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0

    //endregion


    //Constructor


    val db = Firebase.firestore

    //Variables adelantadas
    private lateinit var sensorManager: SensorManager

    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor
    private lateinit var gravitySensor: Sensor
    private lateinit var orientationSensor: Sensor

    val dataBuffer = StringBuffer()


    override fun toString(): String {

        //Tiempo
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val timestamp = dateFormat.format(Date(currentTimeMillis))

        return "$timestamp, $lastX, $lastY, $lastZ, $lastGyroX, $lastGyroY, $lastGyroZ, $lastGravityX, $lastGravityY, $lastGravityZ, $lastOrientationX, $lastOrientationY, $lastOrientationZ, $lastLatitude, $lastLongitude\n"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastX = event.values[0]
                lastY = event.values[1]
                lastZ = event.values[2]
            }

            Sensor.TYPE_GYROSCOPE -> {
                lastGyroX = event.values[0]
                lastGyroY = event.values[1]
                lastGyroZ = event.values[2]
            }

            Sensor.TYPE_GRAVITY -> {
                lastGravityX = event.values[0]
                lastGravityY = event.values[1]
                lastGravityZ = event.values[2]
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastOrientationX = event.values[0]
                lastOrientationY = event.values[1]
                lastOrientationZ = event.values[2]
            }
        }
    }

    fun imprimirUbicacion(ubicacion: Location) {
        lastLatitude = ubicacion.latitude
        lastLongitude = ubicacion.longitude
        Log.d("GPS", "LAT: ${ubicacion.latitude} - LON: ${ubicacion.longitude}")
    }

    fun inicializarSensor(mySensorManager: SensorManager) {

        // Inicializar SensorManager y el sensor de tipo acelerómetro
        sensorManager = mySensorManager

        // Inicializar el sensor
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)!!
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        orientationSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!! // Obsoleto, pero aún se usa
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        // Registrar el listener
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)


    }

    fun registerSensors() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME)

    }

    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Aquí puedes manejar cambios en la precisión del sensor, si es necesario
    }

    //Fucion que escribe en el archivo
    fun writeSensorDataToFile() {

        //Guardar el archivo
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, "Texto.txt")

        dataBuffer.append(toString())

        //Chekear archivo y guardar datos
        var writer: OutputStreamWriter? = null
        try {
            val stream = FileOutputStream(file, true) // Abre en modo "append"
            writer = OutputStreamWriter(stream)
            writer.write(this.toString())
            writer.flush()
        } catch (e: IOException) {
            // Manejar la excepción y finalizar
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                // Ignorar
            }
        }
    }

    fun updateFirestore() {

        // Create a new user with a first and last name
        val user = hashMapOf(
            "Timestamp" to System.currentTimeMillis(),
            "Datas" to dataBuffer.toString()

        )

        dataBuffer.setLength(0)

        val TAG = "null"
// Add a new document with a generated ID
        db.collection("users")
            .document("DataSensorManager")
            .set(user)
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }
}