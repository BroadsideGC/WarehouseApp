package com.ifmo.necracker.warehouse_app

import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.Request
import com.ifmo.necracker.warehouse_app.model.TaskStatus
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException

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
        item = intent.getSerializableExtra("item") as Item
        if (savedInstanceState != null) {
            asyncTask = lastCustomNonConfigurationInstance?.let { it as ListActivity.GetAllItemsTask }
            asyncTask?.let {
                if (asyncTask is MakeOrder) {
                    (asyncTask as MakeOrder).activity = this
                    if ((asyncTask as MakeOrder).status == TaskStatus.PROCESSING) {
                        progressView!!.visibility = View.VISIBLE
                    }
                } else {
                    (asyncTask as UpdateItemTask).activity = this
                    if ((asyncTask as UpdateItemTask).status == TaskStatus.PROCESSING) {
                        progressView!!.visibility = View.VISIBLE
                    }
                }
            }
        }


        swipeContainer = findViewById(R.id.swipeContainerOrder) as SwipeRefreshLayout
        swipeContainer!!.setOnRefreshListener({
            if (asyncTask == null) {
                asyncTask = UpdateItemTask(this)
                asyncTask!!.execute(null)
            }
        })
        nameView!!.text = item!!.name
        idView!!.text = String.format(getString(R.string.string_id), item!!.id)
        amountView!!.text = String.format("Availabale " + getString(R.string.string_amount), item!!.quantity)
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
        return asyncTask
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        intent.putExtra("item", item)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        item = intent.getSerializableExtra("item") as Item
        asyncTask?.let {
            if (asyncTask is MakeOrder) {
                (asyncTask as MakeOrder).activity = this
                if ((asyncTask as MakeOrder).status == TaskStatus.PROCESSING) {
                    progressView!!.visibility = View.VISIBLE
                }
            } else {
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
            val orderId: Long
            try {
                response = restTemplate.getForEntity("$serverAddress/new_order_number", Long::class.java)
                orderId = response.body
                val userId = user!!.id
                val order = Request(orderId, userId.toString().toInt(), id.toString().toInt(), amount)
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
            status = TaskStatus.PROCESSING
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    count = restTemplate.getForEntity("$serverAddress/goods/${item!!.id}", Int::class.java)
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
                activity.item!!.quantity = count!!.body
                activity.amountView!!.text = String.format("Availabale " + getString(R.string.string_amount), item!!.quantity)
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
