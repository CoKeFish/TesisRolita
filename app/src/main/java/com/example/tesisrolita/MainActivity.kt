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
            handler.postDelayed(this, 1000)
        }
    }


    /**
     * Inicializa la actividad principal, configura la vista de usuario, inicia servicios y establece manejadores de eventos.
     *
     * Este método configura el layout de la actividad principal, inicia el servicio `LocationService` en primer plano
     * para garantizar que la aplicación pueda seguir recibiendo actualizaciones de ubicación cuando esté en segundo plano,
     * verifica y solicita los permisos necesarios y configura el comportamiento del botón para alternar el estado de conexión.
     *
     * @param savedInstanceState Si la actividad está siendo reinicializada después de haber sido cerrada anteriormente,
     *                           este Bundle contiene los datos que más recientemente fueron suministrados en onSaveInstanceState(Bundle).
     *                           De lo contrario, es null.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Inicia el servicio 'LocationService' como un servicio en primer plano, lo que permite que la aplicación
        // reciba actualizaciones de ubicación incluso cuando se encuentra en segundo plano.
        startForegroundService(Intent(this, LocationService::class.java))


        // Verifica los permisos necesarios para la aplicación, como los permisos de ubicación.
        verificarPermisos()

        // Verifica los permisos necesarios para la aplicación, como los permisos de ubicación.
        dataSensorManager.inicializarSensor(getSystemService(Context.SENSOR_SERVICE) as SensorManager)


        // Encuentra el botón en el layout utilizando su ID y establece un manejador de clics.
        val bttn = findViewById<Button>(R.id.button)
        bttn.setOnClickListener {
            // Cambia el estado de conexión cada vez que se hace clic en el botón.
            connetStatus = !connetStatus
            if (connetStatus) {
                // Si el estado es 'conectado', cambia el texto del botón a 'Desconectar', inicia el servicio 'LocationService'
                // y comienza a ejecutar un bloque de código de manera periódica utilizando un 'Runnable'.
                bttn.text = getString(R.string.desconectar)
                startForegroundService(Intent(this, LocationService::class.java))
                handler.post(runnableCode) // Iniciar el Runnable

            } else {
                // Si el estado es 'desconectado', cambia el texto del botón a 'Conectar', detiene el servicio 'LocationService'
                // y detiene la ejecución periódica del 'Runnable'.
                bttn.text = getString(R.string.conectar)
                stopService(Intent(this, LocationService::class.java))
                handler.removeCallbacks(runnableCode) // Detener el Runnable
            }

        }

    }


    /**
     * Verifica si la aplicación tiene los permisos necesarios para acceder a la ubicación del dispositivo y, si no,
     * solicita dichos permisos al usuario.
     *
     * Esta función primero crea una lista de los permisos requeridos para acceder a la ubicación del dispositivo
     * en diferentes niveles de precisión (gruesa y fina). Si la versión del SDK es igual o superior a Android 10 (Q),
     * también se añade el permiso para acceder a la ubicación en segundo plano.
     *
     * Después de construir la lista de permisos necesarios, la función verifica si todos estos permisos han sido
     * concedidos previamente. Si todos los permisos han sido concedidos, se actualiza la variable de estado `isPermisos`
     * a `true` y se ejecuta la función `onPermisosConcedidos` para continuar con el flujo normal de la aplicación.
     *
     * En caso de que algún permiso no haya sido concedido, se inicia el proceso para solicitar los permisos faltantes
     * al usuario mediante la función `solicitarPermisos`.
     */
    private fun verificarPermisos() {
        val permisos = arrayListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Añade el permiso de ubicación en segundo plano para versiones Android 10 (Q) y superiores.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permisos.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        // Convierte la lista de permisos en un array para su uso en la solicitud de permisos.
        val permisosArray = permisos.toTypedArray()

        // Verifica si todos los permisos requeridos ya han sido concedidos.
        if (tienePermisos(permisosArray)) {
            isPermisos = true
            onPermisosConcedidos()
        } else {
            solicitarPermisos(permisosArray)
        }
    }



    /**
     * Evalúa si todos los permisos especificados en el array han sido concedidos.
     *
     * Esta función itera sobre el array de permisos proporcionado, comprobando uno por uno si han sido concedidos,
     * utilizando la función `checkSelfPermission` de `ContextCompat`.
     *
     * @param permisos Array de cadenas que contiene los permisos a verificar.
     * @return `true` si todos los permisos han sido concedidos, `false` de lo contrario.
     */
    private fun tienePermisos(permisos: Array<String>): Boolean {
        return permisos.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }



    /**
     * Se ejecuta cuando todos los permisos necesarios han sido concedidos. Esta función inicia los componentes
     * relacionados con la localización y comienza a recibir actualizaciones de la ubicación del dispositivo.
     *
     * Primero, se obtiene una instancia de `FusedLocationProviderClient` para interactuar con el servicio de ubicación.
     * Luego, se intenta obtener la última ubicación conocida del dispositivo. Si se obtiene una ubicación, se utiliza
     * `dataSensorManager` para manejar esta ubicación; si no se puede obtener, se muestra un mensaje al usuario.
     *
     * Además, se configura y solicita actualizaciones de ubicación regulares utilizando `LocationRequest`. Se establece
     * la precisión de la ubicación en alta, con un intervalo de solicitud de 1000 ms. Cuando se reciben actualizaciones
     * de ubicación, cada ubicación en el resultado se maneja utilizando `dataSensorManager`.
     *
     * En caso de que no se tengan los permisos necesarios para acceder a la ubicación (aunque se supone que esto se
     * maneja antes de llamar a esta función), se captura y se maneja una `SecurityException` sin realizar ninguna acción.
     */
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
                1000
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

    /**
     * Solicita al usuario que conceda los permisos especificados que aún no han sido otorgados.
     *
     * Esta función es esencial para asegurar que la aplicación tenga los permisos necesarios para operar correctamente,
     * especialmente para funciones críticas que dependen del acceso a recursos o información sensible del dispositivo,
     * como la ubicación en este contexto. La solicitud de permisos se realiza de forma asíncrona, y el resultado de la
     * solicitud será manejado por el sistema de Android y recibido en el método `onRequestPermissionsResult`.
     *
     * @param permisos Array de permisos que se solicitarán al usuario.
     */
    private fun solicitarPermisos(permisos: Array<String>) {
        requestPermissions(
            permisos,
            CODIGO_PERMISO_SEGUNDO_PLANO
        )
    }


    /**
     * Aquí se verifica si todos los permisos solicitados han sido concedidos. Si es así, se procede con las operaciones
     * que dependen de estos permisos; en este caso, se considera que los permisos de ubicación son esenciales para la
     * funcionalidad de la aplicación relacionada con la localización.
     *
     * @param requestCode Código de solicitud pasado en `requestPermissions`. Se utiliza para verificar que el resultado
     *                    corresponde a nuestra solicitud de permisos de ubicación en segundo plano.
     * @param permissions Array de permisos solicitados, no necesariamente todos concedidos.
     * @param grantResults Resultados de las solicitudes de permisos correspondientes a los permisos solicitados. Cada
     *                     elemento del array es `PackageManager.PERMISSION_GRANTED` o `PackageManager.PERMISSION_DENIED`.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verifica si el resultado corresponde a la solicitud de permisos en segundo plano.
        if (requestCode == CODIGO_PERMISO_SEGUNDO_PLANO) {

            // Verifica si todos los permisos han sido concedidos.
            val todosPermisosConcedidos = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            // Si todos los permisos han sido concedidos, actualiza el estado de permisos y procede con la lógica dependiente de permisos.
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


    /**
     * Método `onCreate` del servicio `LocationService`. Se invoca cuando el servicio se crea por primera vez.
     *
     * Este método configura el servicio para ejecutarse en primer plano, asegurando que continúe ejecutándose incluso
     * cuando la aplicación se encuentre en segundo plano. Se inicializa `fusedLocationClient` para obtener acceso a
     * las APIs de ubicación de Google Play Services. Se define y configura una solicitud de actualizaciones de ubicación
     * con alta precisión y un intervalo de actualización de 10000 ms (10 segundos). Cuando se reciben actualizaciones
     * de ubicación, se invoca el método `onLocationResult` donde se puede manejar cada nueva ubicación.
     *
     * Si no se tienen los permisos necesarios para acceder a la ubicación, se captura y maneja una `SecurityException`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()


        Log.d("TAG", "Si entra al servicio")

        // Inicia el servicio en primer plano con una notificación para asegurar que el servicio no sea detenido por el sistema.
        startForeground(1, createNotification())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura la solicitud de ubicación con alta precisión y una espera de ubicación precisa.
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        // Define cómo manejar las actualizaciones de ubicación.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for (location in p0.locations) {

                }
            }
        }

        // Solicita actualizaciones de ubicación con el request configurado y el callback definido.
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



    /**
     * Crea y configura una notificación para ejecutar el servicio de ubicación en primer plano.
     *
     * Este método es necesario para cumplir con las directrices de Android que requieren que cualquier servicio que se
     * ejecute en primer plano debe mostrar una notificación persistente. Esta notificación informa al usuario de que la
     * aplicación está realizando una tarea en segundo plano, en este caso, actualizando la ubicación.
     *
     * La notificación se configura con un ícono, un título y se establece en una categoría adecuada para servicios que se
     * ejecutan en segundo plano. Además, se crea un canal de notificación específico para versiones Android Oreo (API
     * nivel 26) y superiores, lo que es obligatorio para mostrar notificaciones en estas versiones.
     *
     * @return La notificación configurada para el servicio en primer plano.
     */
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

    /**
     * Se llama cuando el sistema elimina este servicio de la lista de tareas debido a que la tarea que lo alojaba ha sido
     * eliminada.
     *
     * Este método asegura que el servicio se detenga a sí mismo cuando ya no sea necesario, es una buena práctica
     * para liberar recursos y evitar fugas de memoria. Al llamar a `stopSelf()`, se solicita que el servicio se detenga,
     * lo que eventualmente resultará en una llamada a `onDestroy()` si el servicio se está ejecutando actualmente.
     *
     * @param rootIntent El intento que se había utilizado para lanzar el servicio, ahora eliminado.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }


}
