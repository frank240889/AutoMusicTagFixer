package mx.dev.franco.automusictagfixer.filemanager

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by franco on 3/10/17.
 */
object ImageFileSaver {
    private const val SLASH = "/"
    private const val AUTO_MUSIC_TAG_FIXER_FOLDER_NAME = "Covers"
    private const val EXTENSION = "jpg"
    private const val DOT = "."
    const val NULL_DATA = "null data"
    const val NO_EXTERNAL_STORAGE_WRITABLE = "no external storage writable"
    const val INPUT_OUTPUT_ERROR = "i/o onIdentificationError"
    const val GENERIC_NAME = "Unknown_cover"

    /**
     *
     * @param data Image data
     * @param imageName The title of song if exist
     * @param artist The artist of song if exist
     * @param album The album of song if exist
     * @return string absolute path where image was saved or
     * any other string representing the onIdentificationError.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveImageFile(data: ByteArray?, imageName: String?): String {

        //No data to write
        var imageName = imageName
        if (data == null) return NULL_DATA

        //External storage es not writable
        if (!isExternalStorageWritable) {
            return NO_EXTERNAL_STORAGE_WRITABLE
        }


        //Retrieve folder app, and if doesn't exist create it before
        val pathToFile = albumStorageDir
        if (imageName == null || imageName.isEmpty()) imageName = GENERIC_NAME

        //File object representing the new image file
        val imageFileCreated = createFile(pathToFile, imageName)

        //Stream to write
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(imageFileCreated)
            fos.write(data)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            fos?.close()
            return INPUT_OUTPUT_ERROR
        }
        return imageFileCreated.absolutePath
    }

    /**
     * Generates filename, appending the
     * current data and time to avoid repeat
     * file names
     * @param pathToFile Absolute path where will be saved the image
     * @param imageFile The title of song if exist
     * @param artist The artist of song if exist
     * @param album The album of song if exist
     * @return File representing the image
     */
    private fun createFile(pathToFile: File, imageFile: String?): File {
        //Get and format date
        val date = Date()
        val now: DateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val newFileName = imageFile + "_" + now.format(date)
        return File(pathToFile.absolutePath + SLASH + newFileName + DOT + EXTENSION)
    }

    /**
     * Checks if external storage is available for read and write
     */
    private val isExternalStorageWritable: Boolean
        private get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /**
     * Checks if external storage is available for reading at least
     */
    private val isExternalStorageReadable: Boolean
        private get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    /**
     * Get the directory for the user's public pictures directory.
     * @return File representing the absolute path where images
     * are going to be saved
     */
    private val albumStorageDir: File
        private get() = createFolderIfNotExist()

    private fun createFolderIfNotExist(): File {
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), AUTO_MUSIC_TAG_FIXER_FOLDER_NAME
        )
        if (!file.exists()) file.mkdirs()
        return file
    }
}