package com.ifmo.necracker.warehouse_app.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Created by bigz on 15.11.16.
 */
data class Item(@JsonProperty("code") val id: Int,@JsonProperty("count") var quantity: Int) :Serializable {

}