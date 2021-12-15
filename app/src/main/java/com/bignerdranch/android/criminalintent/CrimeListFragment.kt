package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {
    /**
     * Required interface for hosting activities
     */
    interface Callbacks { // This defines work that the fragment needs to be done by it's boss, it's hosting activity.
        fun onCrimeSelected(crimeId: UUID)
    }
    private var callbacks: Callbacks? = null
    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addCrime: Button
    private var adapter: CrimeAdapter? = CrimeAdapter()
    private val crimeListViewModel: CriminalListViewModel by lazy {
        ViewModelProvider(this).get(CriminalListViewModel::class.java)
    }

    override fun onAttach(context: Context) { // This implements Callbacks.
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        emptyView = view.findViewById(R.id.no_crime) as TextView
        addCrime = view.findViewById(R.id.add_crime) as Button
        addCrime.setOnClickListener {
            val crime = Crime()
            crimeListViewModel.addCrime(crime)
            callbacks?.onCrimeSelected(crime.id)
        }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            R.id.delete_crimes -> {
                deleteCrimes()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun deleteCrimes() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton(R.string.yes) { _, _ ->
            crimeListViewModel.deleteCrimes()
            Toast.makeText(context, R.string.deleted, Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.setTitle(R.string.delete_everything)
        builder.setMessage(R.string.delete_everything_text)
        builder.create().show()
    }

    private fun updateUI(crimes: List<Crime>) {
        if (crimes.isEmpty()) {
            crimeRecyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            addCrime.visibility = View.VISIBLE
        } else {
            crimeRecyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            addCrime.visibility = View.GONE
        }
        (crimeRecyclerView.adapter as CrimeAdapter).submitList(crimes)
    }

    private inner class CrimeHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var crime: Crime
        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.crime_date_time)
        private var solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private var contactPoliceButton: Button = itemView.findViewById(R.id.contact_police_button)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            this.crime = crime
            val dateText = dateFormat.format(this.crime.date)
            val timeText = timeFormat.format(this.crime.time)
            titleTextView.text = this.crime.title
            dateTimeTextView.text = getString(R.string.crime_date_and_crime_time, dateText, timeText)
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
            contactPoliceButton.setOnClickListener {
                Toast.makeText(context, R.string.police_button_text, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    private inner class CrimeAdapter : ListAdapter<Crime, CrimeHolder>(DiffCallBack()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id == newItem.id
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