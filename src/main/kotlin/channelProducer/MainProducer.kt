package channelProducer

import FileController
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.CoroutineContext

// Generamos el producto basico
private data class Botella(
    private val id: Int, private val caducidad: String = LocalDate.now().plusYears(1).toString()
)

// Generamos el producto complejo
private data class Caja(
    private val botellas: List<Botella>,
    private val id: String = UUID.randomUUID().toString(),
    private val fecha: String = LocalDateTime.now().toString()
)

// Productor
private data class Productor(private val id: String, private val totalKilos: Int) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.Default

    private var litrosTotales = 0
    private var contadorId = 1

    @OptIn(ExperimentalCoroutinesApi::class)
    fun produceBotellas() = produce {
        for (i in 0 until totalKilos) {
            litrosTotales++
            if (litrosTotales == 5) {
                litrosTotales = 0
                val botella = Botella(contadorId)
                println("Produce $botella")
                send(botella)
                contadorId++
                delay(1000)
            }
        }
        println("Se finalizo la produccion")
    }
}

// Consumidor
private data class Empaquetador(private val id: String) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.Default

    private var file = FileController.init()
    private var botellas = mutableListOf<Botella>()
    private var contadorCaja = 1

    suspend fun preparaCaja(canal: ReceiveChannel<Botella>) {
        for (b in canal) {
            botellas.add(b)
            if (botellas.size == 10) {
                val caja = Caja(botellas)
                println(caja)
                contadorCaja++
                file.appendText("$id -> Lote: $caja\n")

                botellas.clear()
                delay(2000)
            }
        }
        println("Finalizo la consumicion.")
    }
}


fun main(): Unit = runBlocking {
    FileController.resetFile()

    val productor = Productor("Productor-1", 300)

    val canal = productor.produceBotellas()

    val empaquetador = Empaquetador("Empaquetador-1")

    launch {
        empaquetador.preparaCaja(canal)
    }
}