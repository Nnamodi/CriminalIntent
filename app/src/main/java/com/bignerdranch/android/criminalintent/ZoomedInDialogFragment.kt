package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

class ZoomedInDialogFragment: DialogFragment() {
    private lateinit var zoomedInImage: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.zoomed_in_image, container, false)
        zoomedInImage = view.findViewById(R.id.zoomed_in_photo) as ImageView
        val image = arguments?.getSerializable("picture") as File
        zoomedInImage.setImageBitmap(getScaledBitmap(image.path, requireActivity()))
        return view
    }

    companion object {
        fun zoomedPic(pic: File): ZoomedInDialogFragment {
            val image = Bundle().apply {
                putSerializable("picture", pic)
            }
            return ZoomedInDialogFragment().apply {
                arguments = image
            }
        }
    }
}

/**
    Based on a challenge
 */