package com.ifmo.necracker.warehouse_app

import android.app.ListActivity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
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






class ListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var listView: RecyclerView? = null
    private var adapter: CustomViewer? = null
    private var asyncTask: GetAllItemsTask? = GetAllItemsTask()
    private var restTemplate: RestTemplate = RestTemplate()
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.listView) as RecyclerView
        listView!!.setLayoutManager(LinearLayoutManager(this))
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter().apply { objectMapper = ObjectMapper().registerKotlinModule() })


        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        user = intent.getSerializableExtra("user") as User
        asyncTask!!.execute(null)
        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

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
                //menu for order
                makeOrder(Item(itemIdd.text.toString().split(" ").last().toInt(), itemAmount.text.toString().split(" ").last().toInt(), itemName.text.toString()))
            }
        }
    }


    fun makeOrder(item: Item) {
        val intent = Intent(this, OrderActivity::class.java)
        intent.putExtra("item", item)
        intent.putExtra("user", user)
        startActivity(intent)
    }

    fun makeToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.show()
    }

    fun getContext(): Context {
        return this
    }

    inner class GetAllItemsTask internal constructor() : AsyncTask<Void, Void, Boolean>() {
        private var allGoods = listOf<Item>()
        private var error = ""
        override fun doInBackground(vararg params: Void): Boolean? {
            var response :ResponseEntity<JsonNode>? = null
            try {
                response = restTemplate.getForEntity(serverAddress + "/all_goods/", JsonNode::class.java)
                try{
                    val mapper = ObjectMapper().registerKotlinModule()
                    allGoods = mapper.readValue(mapper.treeAsTokens(response.body), object : TypeReference<List<Item>>() {
                    })
                }catch (ignored: IOException){

                }
            } catch (e: HttpStatusCodeException){
                error = "Error during getting items"
                return false
            }
            catch (e: RestClientException) {
                error = "Unable to connect to server"
                return false
            }
            println(allGoods)
            return true
        }

        override fun onPostExecute(success: Boolean?) {

            adapter = CustomViewer(getContext(), allGoods)
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

}
