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
import org.codehaus.jackson.JsonNode
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter

import org.springframework.web.client.RestTemplate
import java.util.*

class ListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var listView: RecyclerView? = null
    var adapter : CustomViewer? = null
    private var  asyncTask : GetAllItemsTask? = GetAllItemsTask()
    var restTemplate : RestTemplate = RestTemplate()
    private val serverAddress = "http://10.0.0.105:1487/mh/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.listView) as RecyclerView
        listView!!.setLayoutManager(LinearLayoutManager(this))
        restTemplate.messageConverters.add(MappingJacksonHttpMessageConverter())

        adapter = CustomViewer(this, ArrayList<String>(Arrays.asList("sds","sdfs","xvcv")))
        listView!!.setAdapter(adapter)
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        asyncTask!!.execute(null)
        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
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
        } else if (id == R.id.nav_checkout) {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }




    inner class CustomViewer(context: Context, private var items: ArrayList<String>) : RecyclerView.Adapter<CustomViewer.ViewHolder>() {
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
            holder.firstLine.setText(items.get(position))
        }

        override fun getItemId(position: Int): Long {
            return items.get(position).hashCode().toLong()
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val firstLine: TextView

            init {
                itemView.setOnClickListener(this)
                firstLine = itemView.findViewById(R.id.fist_line) as TextView
            }

            override fun onClick(v: View) {
                //menu for order
                makeOrder(firstLine.text.toString())
            }
        }
    }


    fun makeOrder(name: String){
        val intent = Intent(this, OrderActivity::class.java)
        intent.putExtra("name", name)
        startActivity(intent)
    }

    inner class GetAllItemsTask internal constructor() : AsyncTask<Void, Void, Boolean>() {
        var goods : JsonNode? = null
        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            goods = restTemplate.getForObject(serverAddress+"all_goods/", JsonNode::class.java)
            println(goods)
            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }


            // TODO: register the new account here.
            return true
        }

        override fun onPostExecute(success: Boolean?) {

        }

        override fun onCancelled() {

        }
    }

}
