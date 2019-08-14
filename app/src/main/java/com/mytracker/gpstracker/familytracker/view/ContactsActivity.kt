package com.mytracker.gpstracker.familytracker.view

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mytracker.gpstracker.familytracker.BuildConfig
import com.mytracker.gpstracker.familytracker.R
import com.mytracker.gpstracker.familytracker.model.repository.Contacts
import com.mytracker.gpstracker.familytracker.model.repository.ContactsAdapter
import timber.log.Timber
import java.util.jar.Manifest

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Contacts"
        setSupportActionBar(toolbar)

        if(BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        adapter = ContactsAdapter(this)
        recyclerView = findViewById(R.id.contacts_rv)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        //check for contacts permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_CONTACTS), 1001)
        } else {
            getContacts()
        }
    }

    private fun getContacts() {
        Timber.d("getContacts: ")
        val contacts: HashMap<String, String> = intent.getSerializableExtra("contacts") as HashMap<String, String>
        for (contact in contacts) {
            adapter.addContact(contact.value, contact.key)
        }
//        progress.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1001 && permissions.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getContacts()
        }
    }
}
