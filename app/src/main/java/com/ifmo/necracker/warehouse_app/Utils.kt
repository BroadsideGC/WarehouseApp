package com.ifmo.necracker.warehouse_app

import android.content.Context
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

/**
 * Created by bigz on 22.11.16.
 */

val serverAddress = "http://35.165.160.141:3029/mh"

fun makeToast(context: Context, text: String) {
    val toast = Toast.makeText(context, text, Toast.LENGTH_LONG)
    toast.show()
}

val restTemplate = CustomRestTemplate()
val TIMEOUT = 3000
val MAX_ATTEMPTS_COUNT  = 4
class CustomRestTemplate()  {
    val restTemplate : RestTemplate
    init {
        restTemplate = RestTemplate()
        val rf = SimpleClientHttpRequestFactory()
        rf.setConnectTimeout(TIMEOUT)
        rf.setReadTimeout(TIMEOUT)
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter().apply { objectMapper = ObjectMapper().registerKotlinModule() })
        restTemplate.requestFactory = rf

    }
}