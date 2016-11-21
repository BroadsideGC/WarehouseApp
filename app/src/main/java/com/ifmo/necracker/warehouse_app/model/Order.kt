package com.ifmo.necracker.warehouse_app.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


/**
 * Created by bigz on 15.11.16.
 */
data class Order(@JsonProperty("id")
                 val id: Long, @JsonProperty("user")
                 var userId: Int, @JsonProperty("code")
                 var uniqueCode: Int,
                 @JsonProperty("amount")
                 var amount: Int, @JsonProperty("type")
                 var type: Order.RequestType, @JsonProperty("status")
                 val status: Order.RequestStatus) :Serializable {

    constructor(id: Long, userId: Int, uniqueCode: Int, amount: Int) : this(id, userId, uniqueCode, amount, RequestType.BOOKED, RequestStatus.IN_PROGRESS) {
    }

    /*override fun toString(): String {
        return String.format("Order[user = %d, order = %d, product = %d, amount = %d, type = %s, status = %s]",
                userId, id, uniqueCode, amount, type.toString(), status.toString())
    }*/

    enum class RequestType private constructor(private val text: String) {
        BOOKED("booked"),
        PAID("paid"),
        CANCELED("canceled");

        override fun toString(): String {
            return text
        }

        companion object {

            fun getRequestTypeFromString(type: String): RequestType? {
                when (type) {
                    "booked" -> return BOOKED
                    "paid" -> return PAID
                    "canceled" -> return CANCELED
                    else -> return null
                }
            }
        }
    }

    enum class RequestStatus private constructor(private val text: String) {
        DONE("done"),
        IN_PROGRESS("in progress"),
        CANCELED("canceled");

        override fun toString(): String {
            return text
        }

        companion object {

            fun getRequestStatusFromString(type: String): RequestStatus? {
                when (type) {
                    "done" -> return DONE
                    "in progress" -> return IN_PROGRESS
                    "canceled" -> return CANCELED
                    else -> return null
                }
            }
        }
    }
}