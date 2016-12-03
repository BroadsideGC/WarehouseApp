package com.ifmo.necracker.warehouse_app.model

import android.os.Parcel
import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Created by bigz on 15.11.16.
 */
data class Item(@JsonProperty("code") val id: Int, @JsonProperty("count") var quantity: Int, @JsonProperty("name") val name: String) : Serializable {


}