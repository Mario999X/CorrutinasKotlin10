package flowStateFlow

import FileController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.system.exitProcess

// Generamos el producto basico
private data class Botella(
    var id: Int, private val caducidad: String = LocalDate.now().plusYears(1).toString()
)

// Generamos el producto complejo
private data class Caja(
    private val botellas: List<Botella>,
    private val id: String = UUID.randomUUID().toString(),
    private val fecha: String = LocalDateTime.now().toString()
)

// Productor
private data class Productor(private val id: String, private val totalKilos: Int, private val deposito: Deposito) {
    private var litrosTotales = 0
    private var contadorId = 1

    suspend fun produceLitros() {
        for (i in 1..totalKilos) {
            litrosTotales++
            if (litrosTotales == 5) {
                litrosTotales = 0
                val botella = Botella(contadorId)
                contadorId++
                deposito.addBotella(botella)
                delay(1000)
            }
        }
    }
}

// Consumidor
private data class Empaquetador(private val id: String, private val deposito: Deposito) {
    private var file = FileController.init()
    private var botellas = mutableListOf<Botella>()
    private var contadorCaja = 1

    suspend fun preparaCajas() {
        while (contadorCaja < 3) {
            if (deposito.botellasDisponibles > 0) {

                val botella = deposito.getBotella()
                botellas.add(botella)

                if (botellas.size == 10) {
                    val caja = Caja(botellas)
                    println(caja)
                    contadorCaja++
                    file.appendText("$id -> Lote: $caja\n")

                    botellas.clear()
                    delay(2000)
                }
            }
            delay(1000) // Espera necesaria para que funcione
        }
        println("Empaquetador $id termino su jornada")
    }
}

// Monitor
private class Deposito() {
    private val botellas = MutableStateFlow<List<Botella>>(listOf())

    val botellasDisponibles get() = botellas.value.size

    private var contadorSalida = 0

    // Agregamos botellas
    fun addBotella(b: Botella) {
        println("Deposito recibe $b")
        botellas.value += b
        contadorSalida++
        if (contadorSalida == 31) {
            exitProcess(1)
        }
    }

    // Retiramos botellas
    fun getBotella(): Botella {
        val botella = botellas.value.first()
        botellas.value = botellas.value.drop(1)
        println("$botella retirada del deposito")
        return botella
    }

}

fun main(): Unit = runBlocking {

    FileController.resetFile()

    val deposito = Deposito()

    val productor = Productor("Productor-1", 300, deposito)
    val empaquetador = Empaquetador("Empaquetador-1", deposito)

    launch {
        productor.produceLitros()
    }

    launch {
        empaquetador.preparaCajas()
    }
}