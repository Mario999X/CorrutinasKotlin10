package flow

import FileController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

// Generamos el productor
private data class Productor(private val id: String, private val totalKilos: Int) {
    private var litrosTotales = 0

    suspend fun produceLitros() = flow {
        for (i in 1..totalKilos) {
            litrosTotales++
            emit(litrosTotales)
            litrosTotales = 0
            delay(1000)
        }
    }
}

// Generamos al consumidor
data class Empaquetador(val id: String) {

    private val file = FileController.init()
    private var litrosParaBotella = 0
    private var contBotella = 0
    private var botellasParaCaja = mutableListOf<Botella>()

    suspend fun preparaLotes(deposito: Flow<Int>) {
        deposito.buffer(1).collect {
            litrosParaBotella++
            if (litrosParaBotella == 5) {
                contBotella++
                val botella = Botella(contBotella)
                println(botella)
                botellasParaCaja.add(botella)
                litrosParaBotella = 0
            }
            if (botellasParaCaja.size == 10) {
                val caja = Caja(botellasParaCaja)
                println(caja)
                file.appendText("$id -> Lote: $caja\n")
                botellasParaCaja.clear()
                delay(2000)
            }
        }
        println("Empaquetador $id termino su jornada")
    }
}

fun main(): Unit = runBlocking {

    FileController.resetFile()

    val productor = Productor("Productor-1", 300)
    val deposito = productor.produceLitros()
    val empaquetador = Empaquetador("Empaquetador-1")

    launch {
        empaquetador.preparaLotes(deposito)
    }
}
