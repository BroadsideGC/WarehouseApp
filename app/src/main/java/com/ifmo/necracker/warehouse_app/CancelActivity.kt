package com.ifmo.necracker.warehouse_app

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

class CancelActivity : AppCompatActivity() {

    private var orderId: TextView? = null
    private var itemId: TextView? = null
    private var itemAmount: TextView? = null
    private var orderType: TextView? = null
    private var orderStatus: TextView? = null
    private var buyButton: Button? = null
    private var cancelButton: Button? = null
    private var user: User? = null
    private var order: Order? = null
    private var asyncTask: AsyncTask<Void, Void, Boolean>? = null
    private val restTemplate = com.ifmo.necracker.warehouse_app.restTemplate.restTemplate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel)
        buyButton = findViewById(R.id.buyButton) as Button
        cancelButton = findViewById(R.id.cancelButton) as Button

        orderId = findViewById(R.id.actionOrderId) as TextView
        itemId = findViewById(R.id.actionOrderItemId) as TextView
        itemAmount = findViewById(R.id.actionOrderAmount) as TextView
        orderStatus = findViewById(R.id.actionOrderStatus) as TextView
        orderType = findViewById(R.id.actionOrderType) as TextView

        user = intent.getSerializableExtra("user") as User
        order = intent.getSerializableExtra("order") as Order
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter().apply { objectMapper = ObjectMapper().registerKotlinModule() })
        orderId!!.text = "Order id: " + order!!.id.toString()
        itemId!!.text = "Item id: " + order!!.uniqueCode
        itemAmount!!.text = "Amount: " + order!!.amount
        orderType!!.text = "Type: " + order!!.type
        orderStatus!!.text = "Status: " + order!!.status
        if (order!!.type == Order.RequestType.BOOKED && order!!.status == Order.RequestStatus.DONE) {
            buyButton!!.setOnClickListener {
                if (asyncTask == null) {
                    asyncTask = BuyOrder(order!!.id)
                    asyncTask!!.execute(null)
                } else {
                    makeToast(this, "Other operation in progress")
                }
            }
        } else {
            buyButton!!.isEnabled = false
        }
        if (order!!.type == Order.RequestType.BOOKED) {
            cancelButton!!.setOnClickListener {
                if (asyncTask == null) {
                    asyncTask = CancelOrder(order!!.id)
                    asyncTask!!.execute(null)
                } else {
                    makeToast(this, "Other operation in progress")
                }
            }
        } else {
            cancelButton!!.isEnabled = false
        }
    }

    fun getContext(): Context {
        return this
    }

    inner class BuyOrder internal constructor(private val id: Long) : AsyncTask<Void, Void, Boolean>() {
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    restTemplate.put(serverAddress + "/payment/" + id, null)
                } catch (e: HttpStatusCodeException) {
                    error = "Error during buying"
                    return false
                } catch (e: RestClientException) {
                    if (attempt == MAX_ATTEMPTS_COUNT) {
                        error = "Unable to connect to server"
                        return false
                    }
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            asyncTask = null
            if (!success!!) {
                makeToast(getContext(), error)
            } else {
                makeToast(getContext(), "Success!")
                order!!.type == Order.RequestType.PAID
                order!!.status == Order.RequestStatus.DONE
                orderType!!.text = "Type: " + order!!.type
                orderStatus!!.text = "Status: " + order!!.status
                buyButton!!.isEnabled = false
                cancelButton!!.isEnabled = false
            }
        }

        override fun onCancelled() {
            asyncTask = null
        }
    }

    inner class CancelOrder internal constructor(private val id: Long) : AsyncTask<Void, Void, Boolean>() {
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    restTemplate.put(serverAddress + "/cancellation/" + id, null)
                    println("Ready")
                } catch (e: HttpStatusCodeException) {
                    error = "Error during cancellation"
                    return false
                } catch (e: RestClientException) {
                    if (attempt == MAX_ATTEMPTS_COUNT) {
                        error = "Unable to connect to server"
                        return false
                    }
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            asyncTask = null
            if (!success!!) {
                makeToast(getContext(), error)
            } else {
                makeToast(getContext(), "Success!")
                order!!.type == Order.RequestType.CANCELED
                order!!.status == Order.RequestStatus.CANCELED
                orderType!!.text = "Type: " + order!!.type
                orderStatus!!.text = "Status: " + order!!.status
                buyButton!!.isEnabled = false
                cancelButton!!.isEnabled = false
            }
        }

        override fun onCancelled() {
            asyncTask = null
        }
    }
}
