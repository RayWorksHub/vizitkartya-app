package hu.rayworks.vizit.nfc

class Type4TagApduProcessor(private val ndefMessage: ByteArray) {
    private var selectedFile = SelectedFile.NONE

    init {
        require(ndefMessage.size <= MAX_NDEF_SIZE) {
            "The NDEF message is larger than the configured Type 4 tag capacity."
        }
    }

    fun process(command: ByteArray): ByteArray {
        if (isSelectApplication(command)) {
            selectedFile = SelectedFile.NONE
            return STATUS_OK
        }

        if (isSelectFile(command)) {
            return when (selectedFileId(command)) {
                CAPABILITY_CONTAINER_FILE_ID -> {
                    selectedFile = SelectedFile.CAPABILITY_CONTAINER
                    STATUS_OK
                }

                NDEF_FILE_ID -> {
                    selectedFile = SelectedFile.NDEF
                    STATUS_OK
                }

                else -> STATUS_FILE_NOT_FOUND
            }
        }

        if (isReadBinary(command)) {
            val file = when (selectedFile) {
                SelectedFile.CAPABILITY_CONTAINER -> CAPABILITY_CONTAINER
                SelectedFile.NDEF -> ndefFile()
                SelectedFile.NONE -> return STATUS_COMMAND_NOT_ALLOWED
            }
            return readBinary(command, file)
        }

        return STATUS_INSTRUCTION_NOT_SUPPORTED
    }

    private fun ndefFile(): ByteArray = byteArrayOf(
        ((ndefMessage.size shr 8) and 0xFF).toByte(),
        (ndefMessage.size and 0xFF).toByte(),
    ) + ndefMessage

    private fun readBinary(command: ByteArray, file: ByteArray): ByteArray {
        if (command.size < 5) return STATUS_WRONG_LENGTH
        val offset = ((command[2].toInt() and 0xFF) shl 8) or (command[3].toInt() and 0xFF)
        if (offset > file.size) return STATUS_WRONG_PARAMETERS
        val requestedLength = (command[4].toInt() and 0xFF).let { if (it == 0) 256 else it }
        val end = minOf(offset + requestedLength, file.size)
        return file.copyOfRange(offset, end) + STATUS_OK
    }

    private fun isSelectFile(command: ByteArray): Boolean =
        command.size >= 7 &&
            command[0] == 0x00.toByte() &&
            command[1] == 0xA4.toByte() &&
            command[2] == 0x00.toByte() &&
            (command[4].toInt() and 0xFF) == 2

    private fun selectedFileId(command: ByteArray): Int =
        ((command[5].toInt() and 0xFF) shl 8) or (command[6].toInt() and 0xFF)

    private fun isReadBinary(command: ByteArray): Boolean =
        command.size >= 5 &&
            command[0] == 0x00.toByte() &&
            command[1] == 0xB0.toByte()

    private enum class SelectedFile {
        NONE,
        CAPABILITY_CONTAINER,
        NDEF,
    }

    companion object {
        private const val CAPABILITY_CONTAINER_FILE_ID = 0xE103
        private const val NDEF_FILE_ID = 0xE104
        private const val MAX_NDEF_SIZE = 0x7FFF

        private val NDEF_APPLICATION_AID = byteArrayOf(
            0xD2.toByte(),
            0x76,
            0x00,
            0x00,
            0x85.toByte(),
            0x01,
            0x01,
        )

        private val CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0xFF.toByte(),
            0x00, 0xFF.toByte(),
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x7F, 0xFF.toByte(),
            0x00,
            0xFF.toByte(),
        )

        val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00)
        val STATUS_SECURITY_NOT_SATISFIED = byteArrayOf(0x69, 0x85.toByte())
        private val STATUS_COMMAND_NOT_ALLOWED = byteArrayOf(0x69, 0x86.toByte())
        private val STATUS_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        private val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val STATUS_WRONG_PARAMETERS = byteArrayOf(0x6B, 0x00)
        private val STATUS_INSTRUCTION_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)

        fun isSelectApplication(command: ByteArray): Boolean {
            if (command.size < 5 + NDEF_APPLICATION_AID.size) return false
            if (
                command[0] != 0x00.toByte() ||
                command[1] != 0xA4.toByte() ||
                command[2] != 0x04.toByte()
            ) {
                return false
            }
            val dataLength = command[4].toInt() and 0xFF
            if (dataLength != NDEF_APPLICATION_AID.size || command.size < 5 + dataLength) return false
            return command.copyOfRange(5, 5 + dataLength).contentEquals(NDEF_APPLICATION_AID)
        }
    }
}
