package com.example.tesisrolita


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class MainActivity : AppCompatActivity() {

    val dataSensorManager = DataSensorManager()

    //Relacionado a la solicitud de permisos al usuario
    private val CODIGO_PERMISO_SEGUNDO_PLANO = 100
    private var isPermisos = false

    //Relacionado al servicio de localización
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    //Estado conectado
    private var connetStatus = false

    // Ejecucion del Runable
    private val handler = Handler(Looper.getMainLooper())

    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            if (connetStatus) {
                // Aquí va el código que quieres ejecutar de forma periódica
                dataSensorManager.writeSensorDataToFile()
            }
            // Repite cada 1000 ms (1 segundo), independientemente del valor de shouldRun
            handler.postDelayed(this, 200)
        }
    }

    private val handlerUpdate = Handler(Looper.getMainLooper())

    private val runnableCodeUpdate: Runnable = object : Runnable {
        override fun run() {
            if (connetStatus) {
                // Aquí va el código que quieres ejecutar de forma periódica
                dataSensorManager.updateFirestore()
            }
            // Repite cada 1000 ms (1 segundo), independientemente del valor de shouldRun
            handlerUpdate.postDelayed(this, 5000)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startForegroundService(Intent(this, LocationService::class.java))


        //Verificamos si hay permisos
        verificarPermisos()


        dataSensorManager.inicializarSensor(getSystemService(Context.SENSOR_SERVICE) as SensorManager)


        // Para iniciar el servicio
        //startForegroundService(Intent(this, SensorService::class.java))


        //Botones
        val bttn = findViewById<Button>(R.id.button)
        bttn.setOnClickListener {

            connetStatus = !connetStatus
            if (connetStatus)
            {
                bttn.text = "Desconectar"
                startForegroundService(Intent(this, LocationService::class.java))
                handler.post(runnableCode) // Iniciar el Runnable
                handlerUpdate.post(runnableCodeUpdate) // Iniciar el Runnable

            }
            else
            {
                bttn.text = "Conectar"
                stopService(Intent(this, LocationService::class.java))
                handler.removeCallbacks(runnableCode) // Detener el Runnable
                handlerUpdate.removeCallbacks(runnableCodeUpdate) // Detener el Runnable
            }

        }

    }



    //Si se acctializa el valor del sensor actualizamos las variables



    override fun onResume() {
        super.onResume()
        // Registrar nuevamente el listener cuando la actividad se reanuda
        // sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        // handler.post(runnableCode) // Iniciar el Runnable
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

    /**
 * Checks if all the given permissions have been granted.
 *
 * @param permisos the permissions to check
 * @return `true` if all the permissions have been granted, `false` otherwise
 */
private fun tienePermisos(permisos: Array<String>): Boolean {
    return permisos.all { permission ->
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

    private fun onPermisosConcedidos() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    dataSensorManager.imprimirUbicacion(it)
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
                        dataSensorManager.imprimirUbicacion(location)
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
