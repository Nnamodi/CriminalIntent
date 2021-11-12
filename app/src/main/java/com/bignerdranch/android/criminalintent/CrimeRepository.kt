package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.bignerdranch.android.criminalintent.database.CrimeDatabase
import com.bignerdranch.android.criminalintent.database.migration_3_4
import java.util.*
import java.util.concurrent.Executors

// CrimeRepository is a SINGLETON, meaning that there will only be one instance of it in the app process.
// It exists as long as the app stays in memory, but it's not a long term data storage.

private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context) {
    private val database : CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_3_4)
        .build()
    private val crimeDao = database.crimeDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    fun updateCrime(crime: Crime) {
        executor.execute {
            crimeDao.updateCrime(crime)
        }
    }

    fun addCrime(crime: Crime) {
        executor.execute {
            crimeDao.addCrime(crime)
        }
    }

    companion object {
        private var INSTANCE: CrimeRepository? = null
        fun initialize(context: Context) { // This makes CrimeRepository a singleton as it initializes a new instance of the repository.
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository { // This too but it accesses the repository.
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}