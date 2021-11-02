package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_TIME = "time"
private const val TIME_KEY = "timeKey"

class TimePickerFragment : DialogFragment() {

    interface Callbacks {
        fun onTimeSelected(time: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeListener = TimePickerDialog.OnTimeSetListener { _: TimePicker, hourOfDay: Int, minute: Int ->
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val second = calendar.get(Calendar.SECOND)
            val resultTime: Date = GregorianCalendar(year, month, day, hourOfDay, minute, second).time
            val result = Bundle().apply {
                putSerializable(TIME_KEY, resultTime)
            }
            parentFragmentManager.setFragmentResult(REQUEST_TIME, result)
        }
        val time = arguments?.getSerializable(ARG_TIME) as Date
        val clock = Calendar.getInstance()
        clock.time = time
        val initialHour = clock.get(Calendar.HOUR)
        val initialMinute = clock.get(Calendar.MINUTE)
        val is24HourView = true

        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinute,
            is24HourView
        )
    }

    companion object {
        fun newInstance(time: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, time)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }

        fun newTime(result: Bundle): Date {
            return result.getSerializable(TIME_KEY) as Date
        }
    }
}