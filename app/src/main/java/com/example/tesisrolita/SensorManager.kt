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
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    //Variables adelantadas
    private lateinit var sensorManager: SensorManager

    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor
    private lateinit var gravitySensor: Sensor
    private lateinit var orientationSensor: Sensor

    val dataBuffer = StringBuffer()


    /**
     * Convierte los datos de los sensores y la ubicación GPS actuales a una cadena de texto formateada.
     *
     * Este método sobrescribe `toString` para proporcionar una representación textual de los últimos datos recopilados
     * por los sensores de acelerómetro, giroscopio, gravedad, magnetómetro (orientación) y la ubicación GPS. Cada valor
     * se separa por comas para facilitar su posterior análisis o almacenamiento, y se incluye una marca de tiempo precisa
     * para cada conjunto de datos recopilados.
     *
     * La marca de tiempo se genera al momento de la llamada a este método y se formatea según el estándar ISO 8601, con
     * precisión hasta milisegundos, lo que permite una correlación precisa de los datos en el tiempo.
     *
     * @return Una cadena de texto que contiene la marca de tiempo y los últimos valores de los sensores y la ubicación GPS,
     *         separados por comas, terminando con un salto de línea.
     */
    override fun toString(): String {

        // Obtención y formateo de la marca de tiempo actual.
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val timestamp = dateFormat.format(Date(currentTimeMillis))

        // Formateo y devolución de los datos de los sensores y la ubicación GPS con la marca de tiempo.
        return "$timestamp, $lastX, $lastY, $lastZ, $lastGyroX, $lastGyroY, $lastGyroZ, $lastGravityX, $lastGravityY, $lastGravityZ, $lastOrientationX, $lastOrientationY, $lastOrientationZ, $lastLatitude, $lastLongitude\n"
    }


    /**
     * Maneja los cambios en las lecturas de los sensores, actualizando las variables correspondientes con los últimos valores.
     *
     * Este método se invoca automáticamente cada vez que un sensor registrado reporta un nuevo evento. Dentro del método,
     * se utiliza una instrucción `when` para determinar el tipo de sensor que ha cambiado y, en consecuencia, actualizar
     * las variables que almacenan las últimas lecturas de ese sensor específico.
     *
     * - Para el **acelerómetro**, se actualizan `lastX`, `lastY` y `lastZ` con los valores de aceleración en los ejes X, Y y Z.
     * - Para el **giroscopio**, se actualizan `lastGyroX`, `lastGyroY` y `lastGyroZ` con los valores de rotación en los ejes X, Y y Z.
     * - Para el **sensor de gravedad**, se actualizan `lastGravityX`, `lastGravityY` y `lastGravityZ` con los valores de gravedad en los ejes X, Y y Z.
     * - Para el **campo magnético (orientación)**, se actualizan `lastOrientationX`, `lastOrientationY` y `lastOrientationZ` con los valores de orientación en los ejes X, Y y Z.
     *
     * @param event El [SensorEvent] que contiene información sobre el nuevo evento del sensor, incluidos el tipo de sensor y los nuevos valores.
     */
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


    /**
     * Actualiza las variables de última ubicación GPS con los valores de latitud y longitud proporcionados por un objeto [Location].
     *
     * Este método se utiliza para registrar la última ubicación conocida del dispositivo. Se espera que sea llamado cada vez
     * que se reciba una nueva actualización de ubicación a través de los servicios de localización del dispositivo. Almacenar
     * la última ubicación permite a la clase `DataSensorManager` incluir esta información en los registros de datos sensoriales,
     * proporcionando un contexto geográfico para las mediciones de los sensores.
     *
     * @param ubicacion Objeto [Location] que contiene la nueva ubicación actualizada. La latitud y longitud contenidas en este
     * objeto se utilizan para actualizar las variables `lastLatitude` y `lastLongitude`.
     */
    fun imprimirUbicacion(ubicacion: Location) {
        lastLatitude = ubicacion.latitude
        lastLongitude = ubicacion.longitude
    }


    /**
     * Inicializa los sensores necesarios y registra este `DataSensorManager` como su listener.
     *
     * Este método se encarga de preparar los sensores requeridos por la aplicación para empezar a recibir actualizaciones
     * de datos. Se deben invocar estos pasos al inicio de la actividad o servicio que haga uso de los datos de los sensores,
     * asegurando que todos los sensores estén listos y configurados antes de su uso.
     *
     * Se inicializa y registra un conjunto de sensores comúnmente utilizados en aplicaciones móviles, incluyendo:
     * - **Sensor de Gravedad**: Para medir la fuerza de gravedad que se aplica en cada eje del dispositivo.
     * - **Giroscopio**: Para medir la tasa de rotación alrededor de los ejes del dispositivo.
     * - **Campo Magnético (Orientación)**: Para detectar la orientación del dispositivo en relación al campo magnético de la Tierra.
     * - **Acelerómetro**: Para medir la aceleración aplicada al dispositivo, incluyendo la fuerza de gravedad.
     *
     * Se utiliza `SENSOR_DELAY_GAME` como tasa de entrega para las actualizaciones de los sensores, ofreciendo un buen
     * equilibrio entre la precisión de los datos y el consumo de recursos del sistema.
     *
     * @param mySensorManager Instancia de `SensorManager` que se utilizará para acceder a los servicios de sensores del dispositivo.
     */
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


    var writer: OutputStreamWriter? = null
    var currentHour = -1



    /**
     * Escribe los datos actuales de los sensores en un archivo, creando un nuevo archivo cada hora para organizar los datos.
     *
     * Este método genera una marca de tiempo y utiliza esta marca para crear un archivo único cada hora, lo que ayuda a evitar
     * la sobreescritura de datos y facilita la organización de los archivos generados. Los datos de los sensores se escriben
     * en el archivo en el formato definido por el método `toString` de esta clase.
     *
     * Se realiza un seguimiento de la hora actual para determinar si es necesario crear un nuevo archivo o continuar escribiendo
     * en el archivo actual. Si la hora ha cambiado desde la última escritura, se cierra el archivo anterior y se crea uno nuevo.
     * Los archivos se almacenan en el directorio de descargas público del dispositivo, asegurando que sean accesibles fácilmente.
     *
     * Se capturan y manejan las excepciones relacionadas con la E/S para garantizar que la aplicación no falle inesperadamente
     * debido a problemas al escribir en el archivo. Se utiliza un log para notificar el inicio de la operación de escritura.
     */
    fun writeSensorDataToFile() {

        //Imprimen un log
        Log.d("TAG", "writeSensorDataToFile()")

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH", Locale.getDefault())
        val timestamp = dateFormat.format(Date(currentTimeMillis))

        if (hour != currentHour) {
            // Cierra el writer anterior si está abierto
            try {
                writer?.close()
            } catch (e: IOException) {
                // Manejar excepción
            }

            currentHour = hour
            val path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            // Crea un nuevo archivo con un nombre que incluya la hora para evitar sobreescrituras
            val file = File(path, "SensorData_$timestamp.txt")

            try {
                val stream = FileOutputStream(file, true) // Abre en modo "append"
                writer = OutputStreamWriter(stream)
            } catch (e: IOException) {
                // Manejar la excepción y finalizar
            }
        }
        // Escribe los datos en el archivo abierto
        try {
            writer?.write(this.toString())
            writer?.flush()
        } catch (e: IOException) {
            // Manejar la excepción
        }

    }

}