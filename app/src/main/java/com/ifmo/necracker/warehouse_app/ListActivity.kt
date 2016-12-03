package com.ifmo.necracker.warehouse_app

import android.app.Activity
import android.app.ListActivity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager

import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ifmo.necracker.warehouse_app.model.Item
import com.ifmo.necracker.warehouse_app.model.User
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException

import org.springframework.web.client.RestTemplate
import java.io.IOException
import android.support.v7.widget.RecyclerView
import android.widget.OverScroller
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import com.ifmo.necracker.warehouse_app.model.TaskStatus


class ListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var listView: RecyclerView? = null
    private var adapter: CustomViewer? = null
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
        swipeContainer!!.setOnRefreshListener(OnRefreshListener {
            if (asyncTask == null) {
                asyncTask = GetAllItemsTask(this)
                asyncTask!!.execute(null)
            }
        })

        progressView =findViewById(R.id.items_progress)
        listView = findViewById(R.id.listView) as RecyclerView
        listView!!.setLayoutManager(LinearLayoutManager(this))

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        user = intent.getSerializableExtra("user") as User
        adapter = CustomViewer(getContext(), itemList)
        listView!!.setAdapter(adapter)
        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        if (asyncTask == null) {
            asyncTask = GetAllItemsTask(this)
            asyncTask!!.execute(null)
            progressView!!.visibility = View.VISIBLE
        }
        (navigationView.getHeaderView(0).findViewById(R.id.loginView) as TextView).text = "Login: " + user!!.login
        (navigationView.getHeaderView(0).findViewById(R.id.idView) as TextView).text = "Id: " + user!!.id
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

        if (id == R.id.nav_checkout) {
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }


    inner class CustomViewer(context: Context, private var items: List<Item>) : RecyclerView.Adapter<CustomViewer.ViewHolder>() {
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
            holder.itemName.text = items.get(position).name
            holder.itemIdd.text = "Id: " + items.get(position).id.toString()
            holder.itemAmount.text = "Amount: " + items.get(position).quantity.toString()
        }

        override fun getItemId(position: Int): Long {
            return items.get(position).hashCode().toLong()
        }

        override fun getItemCount(): Int {
            return items.size
        }


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

    inner class GetAllItemsTask internal constructor(var activity: com.ifmo.necracker.warehouse_app.ListActivity) : AsyncTask<Void, Void, Boolean>() {
        private var allGoods = listOf<Item>()
        private var error = ""
        var status = TaskStatus.NONE

        override fun doInBackground(vararg params: Void): Boolean? {
            status = TaskStatus.PROCESSING
            var response: ResponseEntity<JsonNode>? = null
            for (attempt in 1..MAX_ATTEMPTS_COUNT) {
                try {
                    response = restTemplate.getForEntity(serverAddress + "/all_goods/", JsonNode::class.java)
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
            println(allGoods)
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
                println(itemList.size)
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
