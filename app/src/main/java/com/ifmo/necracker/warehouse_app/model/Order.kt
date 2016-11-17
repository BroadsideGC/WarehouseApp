package com.ifmo.necracker.warehouse_app.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Date

/**
 * Created by bigz on 15.11.16.
 */
class Order(@JsonProperty("id") val id:Long, @JsonProperty("user") val user_id: Int, @JsonProperty("code") val goods_id: Int, @JsonProperty("amount") val quantity: Int, @JsonProperty("type") val type: Int, @JsonProperty("date") var date: Date, @JsonProperty("attempts") var attemts_count: Int,@JsonProperty("status") val status: Int){

}