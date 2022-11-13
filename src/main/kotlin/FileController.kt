import java.io.File
import java.nio.file.Paths

object FileController {
    private val userDir = System.getProperty("user.dir")
    private val pathFile = Paths.get(userDir + File.separator + "data").toString()
    private val file = File(pathFile + File.separator + "produccion.txt")

    fun init(): File {
        return file
    }

    fun resetFile() {
        file.writeText("")
    }
}