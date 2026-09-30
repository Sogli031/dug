package com.example.debttracker.data.backup

/** Imena i ciscenje prethodnih kopija backup fajla ("<ime>_prev_<epochMillis>.json"). */
internal object BackupFilePolicy {
    const val MAX_PREV_COPIES = 5
    private const val SUFFIX = ".json"

    fun prevCopyName(mainFileName: String, nowMillis: Long): String =
        "${prevCopyPrefix(mainFileName)}$nowMillis$SUFFIX"

    /** Vreme iz imena prethodne kopije, ili null ako ime nije prethodna kopija ovog fajla. */
    fun prevCopyTimestamp(mainFileName: String, fileName: String): Long? {
        val prefix = prevCopyPrefix(mainFileName)
        if (!fileName.startsWith(prefix) || !fileName.endsWith(SUFFIX)) return null
        return fileName.substring(prefix.length, fileName.length - SUFFIX.length).toLongOrNull()
    }

    /**
     * Od svih fajlova (id, ime) vraca ID-jeve prethodnih kopija koje treba obrisati:
     * cuva se [keep] najnovijih. Fajlovi koji nisu prethodne kopije se nikad ne vracaju.
     */
    fun prevCopiesToDelete(
        mainFileName: String,
        files: List<Pair<String, String>>,
        keep: Int = MAX_PREV_COPIES
    ): List<String> =
        files.mapNotNull { (id, name) -> prevCopyTimestamp(mainFileName, name)?.let { id to it } }
            .sortedByDescending { it.second }
            .drop(keep)
            .map { it.first }

    private fun prevCopyPrefix(mainFileName: String): String =
        mainFileName.removeSuffix(SUFFIX) + "_prev_"
}
