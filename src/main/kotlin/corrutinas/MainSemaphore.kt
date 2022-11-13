package corrutinas

import FileController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.system.exitProcess

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

// Generamos al productor
private data class Productor(private val id: String, private val totalKilos: Int, private val deposito: Deposito) {

    private var litrosTotales = 0

    suspend fun produceLitros() {
        for (i in 1..totalKilos) {
            litrosTotales++
            if (litrosTotales == 5) {
                deposito.put(litrosTotales)
                litrosTotales = 0
                delay(1000)
            }
        }
    }

}

// Generamos al consumidor
private data class Empaquetador(private val id: String, private val deposito: Deposito) {

    private val file = FileController.init()
    private val botellasParaCaja = mutableListOf<Botella>()
    private var cont = 0

    suspend fun produceCajas() {
        while (cont < 3) {
            val botella = deposito.get()
            botellasParaCaja.add(botella)
            println("Empaquetador: $id -> $botella")

            if (botellasParaCaja.size == 10) {
                val caja = Caja(botellasParaCaja)
                println("\tEmpaquetador: $id -> Lote: $caja")
                file.appendText("$id -> Lote: $caja\n")
                botellasParaCaja.clear()
                cont++
                delay(2000)
            }
        }
        println("Empaquetador: $id termino su jornada")
    }
}

// Generamos el monitor
private class Deposito() {
    private val semaforo = Semaphore(1)

    private var litrosAceite = 0

    private var contadorBotellas = 0

    private val botellasAceite = mutableListOf<Botella>()


    suspend fun put(litro: Int) {
        if (botellasAceite.size > 15) {
            exitProcess(1)
        }
        semaforo.withPermit {
            litrosAceite += litro
            //println("Litro introducido")

            if (litrosAceite == 5) {
                contadorBotellas++
                val botella = Botella(contadorBotellas)
                println("Botella introducida: $botella")
                botellasAceite.add(botella)
                litrosAceite = 0
            }
        }
    }

    suspend fun get(): Botella {
        while (botellasAceite.size == 0) {
            delay(1000)
        }
        semaforo.withPermit {
            val botella = botellasAceite.removeFirst()
            println("Sacando $botella")
            return botella
        }
    }


}

/*
* Mucho cuidado con los bucles en los metodos del consumidor y del monitor.
* */
fun main(): Unit = runBlocking {

    FileController.resetFile()

    val deposito = Deposito()
    val productor = Productor("Productor-1", 300, deposito)
    val empaquetador = Empaquetador("Empaquetador-1", deposito)

    launch {
        productor.produceLitros()
    }

    launch {
        empaquetador.produceCajas()
    }

}