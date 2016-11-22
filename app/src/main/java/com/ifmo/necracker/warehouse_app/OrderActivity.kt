package com.ifmo.necracker.warehouse_app

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class OrderActivity : AppCompatActivity() {


    private val restTemplate = RestTemplate()
    private var amountView: TextView? = null
    private var nameView: TextView? = null
    private var idView: TextView? = null
    private var orderCountView: EditText? = null
    private var orderButton: Button? = null
    private var user: User? = null
    private var item: Item? = null
    private var asyncTask: MakeOrder? = null

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
            val count = orderCountView!!.text.toString().toInt()
            if (count > item!!.quantity) {
                makeToast("More then avaliable")
            } else if (asyncTask == null) {
                asyncTask = MakeOrder(item!!.id, count)
                asyncTask!!.execute(null)
            } else {
                makeToast("Other operation in progress")
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
                error = "Unable to connect to server"
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
