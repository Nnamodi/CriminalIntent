package com.bignerdranch.android.criminalintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
const val REQUEST_DATE = "requestDate"
private const val TAG ="CrimeFragment"
private const val DIALOG_TIME = "DialogTime"
const val REQUEST_TIME = "requestTime"
private const val DATE_FORMAT = "EEE, MMM dd, yyyy"
private const val TIME_FORMAT = "hh:mm:ss a"
private const val REQUEST_CONTACT = 1
private const val CALL_CONTACT = 2
private const val REQUEST_PHOTO = 3

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, FragmentResultListener {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private var viewHeight = 0
    private var viewWidth = 0
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callSuspectButton: ImageButton
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var requiresPoliceCheckBox: CheckBox
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        timeButton = view.findViewById(R.id.crime_time) as Button
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callSuspectButton = view.findViewById(R.id.call_suspect) as ImageButton
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        requiresPoliceCheckBox = view.findViewById(R.id.requires_police) as CheckBox
        photoView.viewTreeObserver.addOnGlobalLayoutListener {
            viewHeight = photoView.height
            viewWidth = photoView.width
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile)
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                // This space intentionally left blank.
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too.
            }
        }
        titleField.addTextChangedListener(titleWatcher)
        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
        requiresPoliceCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.requiresPolice = isChecked
            }
        }
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_DATE)
            }
            childFragmentManager.setFragmentResultListener(REQUEST_DATE, viewLifecycleOwner, this)
        }
        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.time).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_TIME)
            }
            childFragmentManager.setFragmentResultListener(REQUEST_TIME, viewLifecycleOwner) { _, bundle ->
                val result = bundle.getSerializable(TIME_KEY) as Date
                crime.time = result
                updateUI()
            }
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
        suspectButton.apply {
            setOnClickListener {
                val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                // Disable the button if no activity match the Intent given
                val packageManager: PackageManager = requireActivity().packageManager
                val resolvedActivity: ResolveInfo? =
                    packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)

                if (resolvedActivity != null) {
                    startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                } else {
                    Toast.makeText(context, R.string.no_contact_app, Toast.LENGTH_LONG).show()
                }
            }
        }
        callSuspectButton.setOnClickListener {
            // Based on a challenge
            if (crime.suspect.isNotEmpty()) {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
                )
                Log.i("Phone number", "Phone number is ${crime.number}, suspect is ${crime.suspect}.")
            } else {
                Toast.makeText(context, R.string.no_suspect, Toast.LENGTH_SHORT).show()
            }
        }
        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName,
                    photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                if (resolvedActivity != null) {
                    startActivityForResult(captureImage, REQUEST_PHOTO)
                } else {
                    Toast.makeText(context, R.string.no_camera_app, Toast.LENGTH_LONG).show()
                }
            }
        }
        photoView.setOnClickListener {
            ZoomedInDialogFragment.zoomedPic(photoFile).apply {
                show(this@CrimeFragment.childFragmentManager, "image")
            }
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        Log.d(TAG, "Received both requestKey: $requestKey and result: $result.")
        crime.date = DatePickerFragment.newDate(result)
        updateUI()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDestroy() {
        Toast.makeText(context, R.string.crime_saved, Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        val dateTimeFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        titleField.setText(crime.title)
        dateButton.text = dateTimeFormat.format(this.crime.date)
        solvedCheckBox.isChecked = crime.isSolved
        requiresPoliceCheckBox.isChecked = crime.requiresPolice
        timeButton.text = timeFormat.format(this.crime.time)
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = getString(R.string.suspect, crime.suspect)
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, viewWidth, viewHeight)
            photoView.setImageBitmap(bitmap)
            Log.i("fulls","pic is $photoFile")
        } else {
            photoView.setImageDrawable(null)
            Log.i("null", "no pics")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Specify which fields you want your query to return values for
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
                // Perform your query - the contactUri is like a "where" clause here
                val cursor = contactUri?.let {
                    requireActivity().contentResolver.query(it, queryFields, null, null, null)
                }
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }
                    // Pull out the first column of the first row of data -
                    // that is your suspect's name
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
            requestCode == CALL_CONTACT && data!= null -> {
                val phoneURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val queryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val phoneWhereClause = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                val phoneQueryParameters = listOf( crime.suspect )
                val phoneCursor = requireActivity()
                    .contentResolver
                    .query(phoneURI, queryFields, phoneWhereClause, phoneQueryParameters.toTypedArray(), null)
                phoneCursor?.use {
                    if (it.count == 0) {
                        return
                    }
                    it.moveToFirst()
                    val number = it.getString(0)//it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    crime.number = number
                    crimeDetailViewModel.saveCrime(crime)
                }
            }
            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val time = DateFormat.format(TIME_FORMAT, crime.time).toString()
        val police = if (crime.requiresPolice) {
            getString(R.string.crime_report_police_required)
        } else {
            getString(R.string.crime_report_no_police)
        }
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, time, solvedString, police, suspect)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val callContactIntent =
                    Intent(Intent.ACTION_DIAL).apply {
                        val phone = crime.number
                        data = Uri.parse("tel: $phone")
                    }
                startActivityForResult(callContactIntent, CALL_CONTACT)
            } else {
                Log.i("Permission: ", "Denied")
            }
        }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}