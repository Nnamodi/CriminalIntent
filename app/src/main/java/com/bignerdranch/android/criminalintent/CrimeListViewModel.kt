package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel

class CriminalListViewModel : ViewModel() {
    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()
}