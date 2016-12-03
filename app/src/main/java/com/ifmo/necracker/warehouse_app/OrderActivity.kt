package com.ifmo.necracker.warehouse_app

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.widget.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.TaskStatus
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
    private var progressView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)
        amountView = findViewById(R.id.amountView) as TextView
        idView = findViewById(R.id.idOrderView) as TextView
        nameView = findViewById(R.id.idNameView) as TextView
        orderButton = findViewById(R.id.buttonOrder) as Button
        orderCountView = findViewById(R.id.toOrderId) as EditText
        progressView = findViewById(R.id.order_progress) as View
        user = intent.getSerializableExtra("user") as User
        if (savedInstanceState != null) {
            println("cool")
            asyncTask = lastCustomNonConfigurationInstance?.let { it as ListActivity.GetAllItemsTask }
            println("get")
            asyncTask?.let {
                println(asyncTask!!.status)
                if (asyncTask is MakeOrder) {
                    (asyncTask as MakeOrder).activity = this
                    if ((asyncTask as MakeOrder).status == TaskStatus.PROCESSING) {
                        progressView!!.visibility = View.VISIBLE
                    }
                }else {
                    (asyncTask as UpdateItemTask).activity = this
                    if ((asyncTask as UpdateItemTask).status == TaskStatus.PROCESSING) {
                        progressView!!.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            item = intent.getSerializableExtra("item") as Item
        }
        swipeContainer = findViewById(R.id.swipeContainerOrder) as SwipeRefreshLayout
        swipeContainer!!.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            if (asyncTask == null) {
                asyncTask = UpdateItemTask(this)
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
                asyncTask = MakeOrder(item!!.id, count, this)
                progressView!!.visibility = View.VISIBLE
                asyncTask!!.execute(null)
            } else {
                makeToast(this, "Other operation in progress")
            }

        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        println(asyncTask?.let { "have" })
        return asyncTask
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState?.putSerializable("item", item)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        item = savedInstanceState?.getSerializable("item") as Item
        asyncTask?.let {
            if (asyncTask is MakeOrder) {
                (asyncTask as MakeOrder).activity = this
                if ((asyncTask as MakeOrder).status == TaskStatus.PROCESSING) {
                    progressView!!.visibility = View.VISIBLE
                }
            }else {
                (asyncTask as UpdateItemTask).activity = this
                if ((asyncTask as UpdateItemTask).status == TaskStatus.PROCESSING) {
                    progressView!!.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class MakeOrder internal constructor(private val id: Int, private val amount: Int, var activity: OrderActivity) : AsyncTask<Void, Void, Boolean>() {
        private var error = ""
        var status = TaskStatus.NONE

        override fun doInBackground(vararg params: Void): Boolean? {
            val response: ResponseEntity<Long>
            status = TaskStatus.PROCESSING
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
            status = TaskStatus.READY
            activity.progressView!!.visibility = View.GONE
            asyncTask = null
            if (!success!!) {
                makeToast(activity.applicationContext, error)
            } else {
                makeToast(activity.applicationContext, "Success!")
            }
        }

        override fun onCancelled() {
            status = TaskStatus.READY
            activity.progressView!!.visibility = View.GONE
            activity.asyncTask = null
        }
    }

    inner class UpdateItemTask internal constructor(var activity: OrderActivity) : AsyncTask<Void, Void, Boolean>() {
        private var count: ResponseEntity<Int>? = null
        private var error = ""
        var status = TaskStatus.NONE
        override fun doInBackground(vararg params: Void): Boolean? {
            status=TaskStatus.PROCESSING
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    count = restTemplate.getForEntity(serverAddress + "/goods/" + item!!.id, Int::class.java)
                    break
                } catch(e: HttpStatusCodeException) {
                    error = "Error during getting orders"
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
            status = TaskStatus.READY
            activity.progressView!!.visibility = View.GONE
            if (!success!!) {
                makeToast(activity.applicationContext, error)
            } else {
                println("Count: " + count!!.body)
                activity.item!!.quantity = count!!.body
                activity.amountView!!.text = "Amount: " + item!!.quantity
                activity.swipeContainer!!.isRefreshing = false
            }
            activity.asyncTask = null
        }

        override fun onCancelled() {
            status = TaskStatus.READY
            activity.progressView!!.visibility = View.GONE
            activity.swipeContainer!!.isRefreshing = false
            activity.asyncTask = null
        }
    }
}
