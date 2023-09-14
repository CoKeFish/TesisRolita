package com.example.tesisrolita


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity(), SensorEventListener {

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

    //Relacionado a la solicitud de permisos al usuario
    private val CODIGO_PERMISO_SEGUNDO_PLANO = 100
    private var isPermisos = false

    //Relacionado al servicio de localización
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    //Fucion que escribe en el archivo
    private fun writeSensorDataToFile() {

        //Tiempo
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val timestamp = dateFormat.format(Date(currentTimeMillis))

        //Guardar el archivo
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, "Texto.txt")

        //Chekear archivo y guardar datos
        var writer: OutputStreamWriter? = null
        try {
            val stream = FileOutputStream(file, true) // Abre en modo "append"
            writer = OutputStreamWriter(stream)
            writer.write("$timestamp, Accel: $lastX, $lastY, $lastZ, Gyro: $lastGyroX, $lastGyroY, $lastGyroZ, Gravity: $lastGravityX, $lastGravityY, $lastGravityZ, Orientation: $lastOrientationX, $lastOrientationY, $lastOrientationZ, GPS: $lastLatitude, $lastLongitude\n")
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


    // Ejecucion del Runable
    private val handler = Handler(Looper.getMainLooper())

    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            // Aquí va el código que quieres ejecutar de forma periódica
            writeSensorDataToFile()
            handler.postDelayed(this, 1000) // Repite cada 1000 ms (1 segundo)

            //Log.d("TAG", "Conchita")

        }
    }

    //Variables adelantadas acelerometro
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startForegroundService(Intent(this, LocationService::class.java))


        //Verificamos si hay permisos
        verificarPermisos()


        // Inicializar SensorManager y el sensor de tipo acelerómetro
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager


        // Para iniciar el servicio
        //startForegroundService(Intent(this, SensorService::class.java))

        // Inicializar el sensor
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) // Obsoleto, pero aún se usa
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Registrar el listener
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)


        //Botones
        findViewById<Button>(R.id.button).setOnClickListener {


            Log.d("TAG", "sssds.absolutePath")

            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, "Texto.txt")

            try {
                val stream = FileOutputStream(file)
                val writer = OutputStreamWriter(stream)
                writer.write("Un simple texto")
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                // Manejar la excepción
            }

        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Aquí puedes manejar cambios en la precisión del sensor, si es necesario
    }

    //Si se acctializa el valor del sensor actualizamos las variables
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


    override fun onResume() {
        super.onResume()
        // Registrar nuevamente el listener cuando la actividad se reanuda
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        handler.post(runnableCode) // Iniciar el Runnable
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el listener para ahorrar batería
        // sensorManager.unregisterListener(this)
        // handler.removeCallbacks(runnableCode) // Detener el Runnable
    }



    private fun verificarPermisos() {
        val permisos = arrayListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permisos.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val permisosArray = permisos.toTypedArray()
        if (tienePermisos(permisosArray)) {
            isPermisos = true
            onPermisosConcedidos()
        } else {
            solicitarPermisos(permisosArray)
        }
    }

    private fun tienePermisos(permisos: Array<String>): Boolean {
        return permisos.all {
            return ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun onPermisosConcedidos() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    imprimirUbicacion(it)
                } else {
                    Toast.makeText(this, "No se puede obtener la ubicacion", Toast.LENGTH_SHORT).show()
                }
            }

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000
            ).apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)

                    for (location in p0.locations) {
                        imprimirUbicacion(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {

        }
    }

    private fun solicitarPermisos(permisos: Array<String>) {
        requestPermissions(
            permisos,
            CODIGO_PERMISO_SEGUNDO_PLANO
        )
    }



    private fun imprimirUbicacion(ubicacion: Location) {
        lastLatitude = ubicacion.latitude
        lastLongitude = ubicacion.longitude
        Log.d("GPS", "LAT: ${ubicacion.latitude} - LON: ${ubicacion.longitude}")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == CODIGO_PERMISO_SEGUNDO_PLANO) {
            val todosPermisosConcedidos = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (grantResults.isNotEmpty() && todosPermisosConcedidos) {
                isPermisos = true
                onPermisosConcedidos()
            }
        }
    }

}

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()


        Log.d("TAG", "Si entra al servicio")


        startForeground(1, createNotification())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for (location in p0.locations) {
                    // Aquí puedes hacer algo con la nueva ubicación, como enviarla a tu actividad
                    Log.d("TAG", "${location.longitude}")

                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Manejar excepción
        }
    }

    // ... (otros métodos y configuraciones, como createNotification)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelID = "LocationChannel"
        val notificationChannel = NotificationChannel(
            channelID,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, channelID)
        return notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Actualizando ubicación en segundo plano")
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }


}
