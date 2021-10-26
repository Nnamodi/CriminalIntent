package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"
private const val SUBMIT = "submit list"

class CrimeListFragment : Fragment() {
    /**
     * Required interface for hosting activities
     */
    interface Callbacks { // This defines work that the fragment needs to be done by it's boss, it's hosting activity.
        fun onCrimeSelected(crimeId: UUID)
    }
    private var callbacks: Callbacks? = null
    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())
    private val crimeListViewModel: CriminalListViewModel by lazy {
        ViewModelProvider(this).get(CriminalListViewModel::class.java)
    }

    override fun onAttach(context: Context) { // This implements Callbacks.
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe( // This is used to register an observer on the LiveData instance and it ties the observer's life to that of the fragment or activity.
            viewLifecycleOwner,
            { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got ${crimes.size} crimes.")
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onDetach() { // This also implements Callback.
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(crimes: List<Crime>) {
        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
        adapter?.submitList(crimes as MutableList<Crime>?)
    }

    private inner class CrimeHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var crime: Crime
        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private var solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private var contactPoliceButton: Button = itemView.findViewById(R.id.contact_police_button)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            val dateTimeFormat = SimpleDateFormat("EEEE, MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = dateTimeFormat.format(this.crime.date)
            solvedImageView.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
            contactPoliceButton.visibility = if (crime.requiresPolice) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>): ListAdapter<Crime, CrimeHolder>(DiffCallBack()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        override fun getItemCount() = crimes.size

        override fun submitList(crimes: MutableList<Crime>?) {
            if (crimes != null) {
                Log.d(SUBMIT, "Crimes submitted, crime is ${crimes.size} in number and $crimes")
            }
            adapter?.crimes
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id === newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}