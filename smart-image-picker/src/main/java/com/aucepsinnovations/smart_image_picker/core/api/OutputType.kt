package com.aucepsinnovations.smart_image_picker.core.api

import android.os.Parcel
import android.os.Parcelable

enum class OutputType : Parcelable {
    URI,
    FILE;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OutputType> {
        override fun createFromParcel(parcel: Parcel): OutputType? {
            return OutputType.entries[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<out OutputType?>? {
            return arrayOfNulls(size = size)
        }

    }
}