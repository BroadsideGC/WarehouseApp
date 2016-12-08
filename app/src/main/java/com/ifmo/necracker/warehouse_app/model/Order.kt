package com.ifmo.necracker.warehouse_app.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


/**
 * Created by bigz on 15.11.16.
 */
data class Order(@JsonProperty("id")
                 val id: Long,
                 @JsonProperty("goods")
                 var name: String,
                 @JsonProperty("quantity")
                 var amount: Int, @JsonProperty("type")
                 var type: Request.RequestType, @JsonProperty("status")
                 var status: Request.RequestStatus) : Serializable {


}