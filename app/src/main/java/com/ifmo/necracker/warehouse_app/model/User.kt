package com.ifmo.necracker.warehouse_app.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Created by bigz on 15.11.16.
 */

data class User(val id: Int, @JsonProperty("login") var login: String, @JsonProperty("password") val password: String) :Serializable {

    override fun toString(): String {
        return String.format("User [id = %s, login = %s, password = %s]", id, login, password)
    }
}

