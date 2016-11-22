package com.ifmo.necracker.warehouse_app

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.core.type.TypeReference

import com.fasterxml.jackson.module.kotlin.registerKotlinModule

import com.ifmo.necracker.warehouse_app.model.Order
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

import org.springframework.web.client.HttpStatusCodeException


class CheckoutActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var listView: RecyclerView? = null
    private var adapter: OrdersViewer? = null
    private var asyncTask: GetAllOrdersTask? = GetAllOrdersTask()
    private var restTemplate: RestTemplate = RestTemplate()
    private var user: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter().apply { objectMapper = ObjectMapper().registerKotlinModule() })
        listView = findViewById(R.id.listViewCheckout) as RecyclerView
        listView!!.setLayoutManager(LinearLayoutManager(this))
        //adapter = OrdersViewer(getContext(), listOf<Order>())
        //listView!!.setAdapter(adapter)


        //adapter = CustomViewer(this, ArrayList<String>(Arrays.asList("s343", "s34s", "xvcfgWv")))
        //listView!!.setAdapter(adapter)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        user = intent.getSerializableExtra("user") as User
        println(user)

        (navigationView.getHeaderView(0).findViewById(R.id.loginView) as TextView).text = "Login: " + user!!.login
        (navigationView.getHeaderView(0).findViewById(R.id.idView) as TextView).text = "Id: " + user!!.id
        asyncTask!!.execute(null)

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

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
            // holder.itemName.text = items.get(position).
            holder.orderId.text = "OrderId: " + items[position].id.toString()
            holder.itemIdd.text = "Id: " + items.get(position).uniqueCode.toString()
            holder.itemAmount.text = "Amount: " + items.get(position).amount.toString()
            holder.itemType.text = "Type: " + items.get(position).type.toString()
            holder.itemStatus.text = "Status: " + items.get(position).status.toString()
        }

        override fun getItemId(position: Int): Long {
            return items.get(position).hashCode().toLong()
        }

        override fun getItemCount(): Int {
            return items.size
        }

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
                val statusString = itemStatus.text.split(" ")[1] + if (itemStatus.text.split(" ").size > 2) " " + itemStatus.text.split(" ")[2] else ""
                println(statusString)
                makeCancel(Order(orderId.text.split(" ").last().toLong(),
                        user!!.id, itemIdd.text.split(" ").last().toInt(),
                        itemAmount.text.split(" ").last().toInt(),
                        Order.RequestType.getRequestTypeFromString(itemType.text.split(" ").last())!!,
                        Order.RequestStatus.getRequestStatusFromString(statusString)!!))
            }
        }
    }

    fun makeToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.show()
    }

    fun getContext(): Context {
        return this
    }


    inner class GetAllOrdersTask internal constructor() : AsyncTask<Void, Void, Boolean>() {
        private var orders: List<Order> = listOf<Order>()
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            try {
                val mapper = ObjectMapper().registerKotlinModule()
                val requestsJson = restTemplate.getForEntity(serverAddress + "/all_user_orders/" + user!!.id, JsonNode::class.java)
                println(requestsJson)
                orders = mapper.readValue(mapper.treeAsTokens(requestsJson.body), object : TypeReference<List<Order>>() {

                })
                println(orders)
            } catch(e: HttpStatusCodeException) {
                error = "Error during getting orders"
                return false
            } catch (e: RestClientException) {
                error = "Unable to connect to server"
                return false
            } catch (e: IOException) {
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            adapter = OrdersViewer(getContext(), orders)
            listView!!.setAdapter(adapter)
            if (!success!!) {
                makeToast(error)
            }
            asyncTask = null
        }

        override fun onCancelled() {
            asyncTask = null
        }
    }

    fun makeCancel(order: Order) {
        val intent = Intent(this, CancelActivity::class.java)
        println(order)
        intent.putExtra("order", order)
        intent.putExtra("user", user)
        startActivity(intent)
    }
}
