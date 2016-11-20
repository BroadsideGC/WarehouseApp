package com.ifmo.necracker.warehouse_app

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class OrderActivity : AppCompatActivity() {


    val restTemplate = RestTemplate()
    var amountView: TextView? = null
    var nameView: TextView? = null
    var idView: TextView? = null
    var orderCountView: EditText? = null
    var orderButton: Button? = null
    var user: User? = null
    var item: Item? = null
    private var asyncTask: MakeOrder? = null
    private val serverAddress = "http://10.0.0.105:1487/mh/"
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
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter().apply { objectMapper = ObjectMapper().registerKotlinModule() })
        nameView!!.text = item!!.name
        idView!!.text = "Id: " + item!!.id
        amountView!!.text = "Amount: " + item!!.quantity
        orderButton!!.setOnClickListener {
            if (asyncTask == null) {
                asyncTask = MakeOrder(item!!.id, orderCountView!!.text.toString().toInt())
                asyncTask!!.execute(null)
            }

        }
    }


    fun makeToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.show()
    }

    inner class MakeOrder internal constructor(private val id: Int, private val amount: Int) : AsyncTask<Void, Void, Boolean>() {
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            var response = -1L
            var orderId: Long = 0
            try {
                orderId = restTemplate.getForObject(serverAddress + "new_order", Long::class.java)
                val userId = user!!.id
                println(orderId.toString() + " " + userId + " " + id + " " + amount)
                val order = Order(orderId, userId.toString().toInt(), id.toString().toInt(), amount)
                println(ObjectMapper().writeValueAsString(order))
                response = restTemplate.postForObject(serverAddress + "book", order, Long::class.java)
            } catch (e: RestClientException) {
                error = "Unable to connect to server"
                return false
            }
            if (response == -1L) {
                error = "Error during booking"
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            asyncTask = null
            if (!success!!) {
                makeToast(error)
            } else {
                makeToast("Success!")
            }
        }

        override fun onCancelled() {
            asyncTask = null
        }
    }
}
