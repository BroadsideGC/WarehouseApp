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
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.TaskStatus
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import java.io.IOException


class ListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var listView: RecyclerView? = null
    private var adapter: ItemsViewer? = null
    private var asyncTask: GetAllItemsTask? = null
    private var restTemplate = com.ifmo.necracker.warehouse_app.restTemplate.restTemplate
    private var user: User? = null
    private val itemList = mutableListOf<Item>()
    private var swipeContainer: SwipeRefreshLayout? = null
    private var progressView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        swipeContainer = findViewById(R.id.swipeContainerList) as SwipeRefreshLayout
        swipeContainer!!.setOnRefreshListener({
            if (asyncTask == null) {
                asyncTask = GetAllItemsTask(this)
                asyncTask!!.execute(null)
            }
        })

        progressView = findViewById(R.id.items_progress)
        listView = findViewById(R.id.listView) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(this)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        user = intent.getSerializableExtra("user") as User
        adapter = ItemsViewer(getContext(), itemList)
        listView!!.adapter = adapter
        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        if (asyncTask == null) {
            asyncTask = GetAllItemsTask(this)
            asyncTask!!.execute(null)
            progressView!!.visibility = View.VISIBLE
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

        if (id == R.id.nav_checkout) {
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }


    inner class ItemsViewer(context: Context, private var items: List<Item>) : RecyclerView.Adapter<ItemsViewer.ViewHolder>() {
        private var li: LayoutInflater? = null

        init {
            li = LayoutInflater.from(context)
            this.items = items
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(li!!.inflate(R.layout.item_list, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemName.text = items[position].name
            holder.itemIdd.text = String.format(getString(R.string.string_id), items[position].id)
            holder.itemAmount.text = String.format(getString(R.string.string_amount), items[position].quantity)
        }

        override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

        override fun getItemCount(): Int = items.size


        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val itemName: TextView
            val itemIdd: TextView
            val itemAmount: TextView

            init {
                itemView.setOnClickListener(this)
                itemName = itemView.findViewById(R.id.itemName) as TextView
                itemIdd = itemView.findViewById(R.id.itemId) as TextView
                itemAmount = itemView.findViewById(R.id.itemAmount) as TextView
            }

            override fun onClick(v: View) {
                makeOrder(items[this.adapterPosition])
            }
        }
    }


    fun makeOrder(item: Item) {
        val intent = Intent(this, OrderActivity::class.java)
        intent.putExtra("item", item)
        intent.putExtra("user", user)
        startActivity(intent)
    }


    fun getContext(): Context {
        return this
    }

    inner class GetAllItemsTask internal constructor(var activity: ListActivity) : AsyncTask<Void, Void, Boolean>() {
        private var allGoods = listOf<Item>()
        private var error = ""
        var status = TaskStatus.NONE

        override fun doInBackground(vararg params: Void): Boolean? {
            status = TaskStatus.PROCESSING
            var response: ResponseEntity<JsonNode>?
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    response = restTemplate.getForEntity("$serverAddress/all_goods/", JsonNode::class.java)
                    try {
                        val mapper = ObjectMapper().registerKotlinModule()
                        allGoods = mapper.readValue(mapper.treeAsTokens(response.body), object : TypeReference<List<Item>>() {
                        })
                    } catch (ignored: IOException) {

                    }
                } catch (e: HttpStatusCodeException) {
                    error = "Error during getting items"
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
                activity.itemList.clear()
                activity.itemList.addAll(allGoods)
                activity.adapter!!.notifyDataSetChanged()
            }
            activity.swipeContainer!!.isRefreshing = false
            activity.asyncTask = null

        }

        override fun onCancelled() {
            status = TaskStatus.READY
            activity.progressView!!.visibility = View.GONE
            activity.swipeContainer!!.isRefreshing = false
            asyncTask = null
        }
    }

}
