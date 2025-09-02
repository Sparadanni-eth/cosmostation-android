package wannabit.io.cosmostaion.database.legacy

import android.content.Context
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import wannabit.io.cosmostaion.common.BaseConstant
import wannabit.io.cosmostaion.ui.main.CosmostationApp

class LegacyDatabase(
    context: Context,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
    passphrase: String
) : SQLiteOpenHelper(
    context.applicationContext, name, passphrase, factory, version, 0, DatabaseErrorHandler { }, object : SQLiteDatabaseHook {
        override fun preKey(connection: SQLiteConnection) = Unit
        override fun postKey(connection: SQLiteConnection) {
            connection.executeForLong("PRAGMA cipher_migrate;", null, null)
        }
    }, true
) {
    companion object {
        private var INSTANCE: LegacyDatabase? = null

        fun getInstance(passphrase: String): LegacyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LegacyDatabase(
                    CosmostationApp.instance, BaseConstant.DB_NAME, null, BaseConstant.DB_VERSION, passphrase
                ).also { INSTANCE = it }
            }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_PASSWORD + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [resource] TEXT, [spec] TEXT)")
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_ACCOUNT + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [nickName] TEXT, [isFavo] INTEGER DEFAULT 0, [address] TEXT, [baseChain] INTEGER, " + "[hasPrivateKey] INTEGER DEFAULT 0, [resource] TEXT, [spec] TEXT, [fromMnemonic] INTEGER DEFAULT 0, [path] TEXT, " + "[isValidator] INTEGER DEFAULT 0, [sequenceNumber] INTEGER, [accountNumber] INTEGER, [fetchTime] INTEGER, [msize] INTEGER, [importTime] INTEGER, [lastTotal] TEXT, [sortOrder] INTEGER, " + "[pushAlarm] INTEGER DEFAULT 0, [newBip] INTEGER DEFAULT 0, [customPath] INTEGER DEFAULT 0, [mnemonicId] INTEGER DEFAULT 0)")
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_BALANCE + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [accountId] INTEGER, [symbol] TEXT, [balance] TEXT, [fetchTime] INTEGER, [frozen] TEXT, [locked] TEXT)")
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_BONDING + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [accountId] INTEGER, [validatorAddress] TEXT, [shares] TEXT, [fetchTime] INTEGER)")
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_UNBONDING + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [accountId] INTEGER, [validatorAddress] TEXT, [creationHeight] TEXT, [completionTime] INTEGER, " + "[initialBalance] TEXT, [balance] TEXT, [fetchTime] INTEGER)")
        sqLiteDatabase.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_BALANCE + " ADD COLUMN " + "frozen" + " TEXT")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_BALANCE + " ADD COLUMN " + "locked" + " TEXT")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "lastTotal" + " TEXT")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "sortOrder" + " INTEGER")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "pushAlarm" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "newBip" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "customPath" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            2 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "lastTotal" + " TEXT")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "sortOrder" + " INTEGER")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "pushAlarm" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "newBip" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "customPath" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            3 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "pushAlarm" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "newBip" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "customPath" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            4 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "newBip" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "customPath" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            5 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "customPath" + "  INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }

            6 -> {
                try {
                    db.beginTransaction()
                    db.execSQL("ALTER TABLE " + BaseConstant.DB_TABLE_ACCOUNT + " ADD COLUMN " + "mnemonicId" + "  INTEGER DEFAULT 0")
                    db.execSQL("CREATE TABLE [" + BaseConstant.DB_TABLE_MNEMONIC + "] ([id] INTEGER PRIMARY KEY AUTOINCREMENT, [uuid] TEXT, [resource] TEXT, [spec] TEXT, [nickName] TEXT, [wordsCnt] INTEGER DEFAULT 0, [isFavo] INTEGER DEFAULT 0, [importTime] INTEGER)")
                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}