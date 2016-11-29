package com.ifmo.necracker.warehouse_app

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.widget.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.IOException

class OrderActivity : AppCompatActivity() {


    private val restTemplate = com.ifmo.necracker.warehouse_app.restTemplate.restTemplate
    private var amountView: TextView? = null
    private var nameView: TextView? = null
    private var idView: TextView? = null
    private var orderCountView: EditText? = null
    private var orderButton: Button? = null
    private var user: User? = null
    private var item: Item? = null
    private var asyncTask: AsyncTask<Void, Void, Boolean>? = null
    private var swipeContainer: SwipeRefreshLayout? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)
        amountView = findViewById(R.id.amountView) as TextView
        idView = findViewById(R.id.idOrderView) as TextView
        nameView = findViewById(R.id.idNameView) as TextView
        orderButton = findViewById(R.id.buttonOrder) as Button
        orderCountView = findViewById(R.id.toOrderId) as EditText
        user = intent.getSerializableExtra("user") as User
        item = intent.getSerializableExtra("item") as Item
        swipeContainer = findViewById(R.id.swipeContainerOrder) as SwipeRefreshLayout
        swipeContainer!!.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            if (asyncTask == null) {
                asyncTask = UpdateItemTask()
                asyncTask!!.execute(null)
            }
        })
        nameView!!.text = item!!.name
        idView!!.text = "Id: " + item!!.id
        amountView!!.text = "Amount: " + item!!.quantity
        orderButton!!.setOnClickListener {
            val count = orderCountView!!.text.toString().toInt()
            if (count > item!!.quantity) {
                makeToast(this, "More then avaliable")
            } else if (asyncTask == null) {
                asyncTask = MakeOrder(item!!.id, count)
                asyncTask!!.execute(null)
            } else {
                makeToast(this, "Other operation in progress")
            }

        }
    }

    fun getContext(): Context {
        return this
    }

    inner class MakeOrder internal constructor(private val id: Int, private val amount: Int) : AsyncTask<Void, Void, Boolean>() {
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            val response: ResponseEntity<Long>
            var orderId: Long = 0
            try {
                response = restTemplate.getForEntity(serverAddress + "/new_order_number", Long::class.java)
                orderId = response.body
                val userId = user!!.id
                println(orderId.toString() + " " + userId + " " + id + " " + amount)
                val order = Order(orderId, userId.toString().toInt(), id.toString().toInt(), amount)
                restTemplate.postForEntity(serverAddress + "/book", order, Long::class.java)
            } catch (e: HttpStatusCodeException) {
                error = "Error during booking"
                return false
            } catch (e: RestClientException) {
                e.message
                error = "Unable to connect to server"
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            asyncTask = null
            if (!success!!) {
                makeToast(getContext(), error)
            } else {
                makeToast(getContext(), "Success!")
            }
        }

        override fun onCancelled() {
            asyncTask = null
        }
    }

    inner class UpdateItemTask internal constructor() : AsyncTask<Void, Void, Boolean>() {
        private var count: ResponseEntity<Int>? = null
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {

            try {
                count = restTemplate.getForEntity(serverAddress + "/goods/" + item!!.id, Int::class.java)
            } catch(e: HttpStatusCodeException) {
                error = "Error during getting orders"
                return false
            } catch (e: RestClientException) {
                error = "Unable to connect to server"
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {

            if (!success!!) {
                makeToast(getContext(), error)
            } else {
                println("Count: " + count!!.body)
                item!!.quantity = count!!.body
                amountView!!.text = "Amount: " + item!!.quantity
                swipeContainer!!.isRefreshing = false
            }
            asyncTask = null
        }

        override fun onCancelled() {
            swipeContainer!!.isRefreshing = false
            asyncTask = null
        }
    }
}
