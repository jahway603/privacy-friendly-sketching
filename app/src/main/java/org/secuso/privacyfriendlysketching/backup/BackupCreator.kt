package org.secuso.privacyfriendlysketching.backup

import android.content.Context
import android.preference.PreferenceManager
import android.util.JsonWriter
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import org.secuso.privacyfriendlybackup.api.backup.DatabaseUtil
import org.secuso.privacyfriendlybackup.api.backup.PreferenceUtil
import org.secuso.privacyfriendlybackup.api.pfa.IBackupCreator
import org.secuso.privacyfriendlysketching.database.SketchingRoomDB
import org.secuso.privacyfriendlysketching.helpers.EncryptionHelper
import java.io.OutputStream
import java.io.OutputStreamWriter

class BackupCreator : IBackupCreator {
    override fun writeBackup(context: Context, outputStream: OutputStream) : Boolean {
        Log.d(TAG, "createBackup() started")
        val outputStreamWriter = OutputStreamWriter(outputStream, Charsets.UTF_8)
        val writer = JsonWriter(outputStreamWriter)
        writer.setIndent("")

        try {
            writer.beginObject()

            Log.d(TAG, "Writing database")
            writer.name("database")

            SQLiteDatabase.loadLibs(context)

            if (context.getDatabasePath(SketchingRoomDB.DATABASENAME).exists()) {
                val database = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(SketchingRoomDB.DATABASENAME).path,
                    EncryptionHelper.loadPassPhrase(context),
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                    object : SQLiteDatabaseHook {
                        override fun preKey(database: SQLiteDatabase) {}
                        override fun postKey(database: SQLiteDatabase) {
                            database.rawExecSQL("PRAGMA cipher_compatibility = 3;")
                        }
                    })

                DatabaseUtil.writeDatabase(writer, database)
                database.close()
            } else {
                Log.d(TAG, "No database found")
                writer.beginObject()
                writer.endObject()
            }

            Log.d(TAG, "Writing preferences")
            writer.name("preferences")

            val pref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            PreferenceUtil.writePreferences(writer, pref, arrayOf(EncryptionHelper.PASSPHRASE_KEY_PREF_NAME))

            writer.endObject()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred", e)
            e.printStackTrace()
            return false
        }

        Log.d(TAG, "Backup created successfully")
        return true
    }

    companion object {
        const val TAG = "PFABackupCreator"
    }
}