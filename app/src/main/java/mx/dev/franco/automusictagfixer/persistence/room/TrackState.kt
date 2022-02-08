package mx.dev.franco.automusictagfixer.persistence.room

object TrackState {
    //Status of correction of items
    const val NO_TAGS_SEARCHED_YET = 0
    const val ALL_TAGS_FOUND = 1
    const val ALL_TAGS_NOT_FOUND = 2
    const val NO_TAGS_FOUND = -1
    const val TAGS_EDITED_BY_USER = 3
    const val FILE_IN_SD_WITHOUT_PERMISSION = 6
    const val COULD_NOT_CREATE_TEMP_FILE = 7
    const val COULD_NOT_CREATE_AUDIOFILE = 8
    const val COULD_RESTORE_FILE_TO_ITS_LOCATION = 9
    const val COULD_NOT_APPLIED_CHANGES = 10
    const val FILE_ERROR_READ = 4
    const val TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE = 5
}