package com.ifmo.necracker.warehouse_app

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import java.io.IOException


class CheckoutActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var listView: RecyclerView? = null
    private var adapter: OrdersViewer? = null
    private var asyncTask: GetAllOrdersTask? = null
    private var restTemplate = com.ifmo.necracker.warehouse_app.restTemplate.restTemplate
    private var user: User? = null
    private var swipeContainer: SwipeRefreshLayout? = null
    private val ordersList = mutableListOf<Order>()
    private var progressView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        progressView = findViewById(R.id.orders_progress) as View

        swipeContainer = findViewById(R.id.swipeContainerCheckout) as SwipeRefreshLayout
        swipeContainer!!.setOnRefreshListener({
            if (asyncTask == null) {
                asyncTask = GetAllOrdersTask()
                asyncTask!!.execute(null)
            }
        })

        listView = findViewById(R.id.listViewCheckout) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(this)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        user = intent.getSerializableExtra("user") as User

        adapter = OrdersViewer(this, ordersList)
        listView!!.adapter = adapter

        if (asyncTask == null) {
            asyncTask = GetAllOrdersTask()
            progressView!!.visibility = View.VISIBLE
            asyncTask!!.execute(null)
        }
        (navigationView.getHeaderView(0).findViewById(R.id.loginView) as TextView).text = String.format(getString(R.string.string_username), user!!.login)
        (navigationView.getHeaderView(0).findViewById(R.id.idView) as TextView).text = String.format(getString(R.string.string_id), user!!.id)

    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item)
    }

    @SuppressWarnings("StatementWithEmptyBody")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        if (id == R.id.nav_list) {
            val intent = Intent(this, com.ifmo.necracker.warehouse_app.ListActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    inner class OrdersViewer(context: Context, private var items: List<Order>) : RecyclerView.Adapter<OrdersViewer.OrderViewHolder>() {
        private var li: LayoutInflater? = null

        init {
            li = LayoutInflater.from(context)
            this.items = items
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            return OrderViewHolder(li!!.inflate(R.layout.order_list, parent, false))
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            holder.orderId.text = String.format("Order" + getString(R.string.string_id), items[position].id)
            holder.itemIdd.text = "Name: ${items[position].name}"
            holder.itemAmount.text = String.format(getString(R.string.string_amount), items[position].amount)
            holder.itemType.text = String.format(getString(R.string.string_type), items[position].type)
            holder.itemStatus.text = String.format(getString(R.string.string_status), items[position].status)
        }

        override fun getItemId(position: Int): Long {
            return items[position].hashCode().toLong()
        }

        override fun getItemCount(): Int = items.size

        inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val orderId: TextView
            val itemIdd: TextView
            val itemAmount: TextView
            val itemType: TextView
            val itemStatus: TextView

            init {
                itemView.setOnClickListener(this)
                orderId = itemView.findViewById(R.id.orderId) as TextView
                itemIdd = itemView.findViewById(R.id.orderItemId) as TextView
                itemType = itemView.findViewById(R.id.orderType) as TextView
                itemStatus = itemView.findViewById(R.id.orderStatus) as TextView
                itemAmount = itemView.findViewById(R.id.orderAmount) as TextView
            }

            override fun onClick(v: View) {
                makeCancel(items[this.adapterPosition])
            }
        }
    }

    fun getContext(): Context {
        return this
    }


    inner class GetAllOrdersTask internal constructor() : AsyncTask<Void, Void, Boolean>() {
        private var orders = listOf<Order>()
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    val mapper = ObjectMapper().registerKotlinModule()
                    val requestsJson = restTemplate.getForEntity("$serverAddress/all_user_orders/${user!!.id}", JsonNode::class.java)
                    orders = mapper.readValue(mapper.treeAsTokens(requestsJson.body), object : TypeReference<List<Order>>() {
                    })
                } catch(e: HttpStatusCodeException) {
                    error = "Error during getting orders"
                    return false
                } catch (e: RestClientException) {
                    if (attempt == MAX_ATTEMPTS_COUNT) {
                        error = "Unable to connect to server"
                        return false
                    }
                } catch (e: IOException) {
                    error = "Unable to parse response"
                    return false
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            progressView!!.visibility = View.GONE
            if (!success!!) {
                makeToast(getContext(), error)
            } else {
                ordersList.clear()
                ordersList.addAll(orders)
                adapter!!.notifyDataSetChanged()
                swipeContainer!!.isRefreshing = false
            }
            asyncTask = null
        }

        override fun onCancelled() {
            progressView!!.visibility = View.GONE
            swipeContainer!!.isRefreshing = false
            asyncTask = null
        }
    }

    fun makeCancel(order: Order) {
        val intent = Intent(this, CancelActivity::class.java)
        intent.putExtra("order", order)
        intent.putExtra("user", user)
        startActivity(intent)
    }
}
