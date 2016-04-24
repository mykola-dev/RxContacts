package ds.rxcontacts.demo

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.Bind
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions.RxPermissions
import com.trello.rxlifecycle.components.support.RxAppCompatActivity
import ds.rxcontacts.*
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.*

class MainActivity : RxAppCompatActivity() {

	@Bind(R.id.recycler) lateinit var recyclerView: RecyclerView
	@Bind(R.id.progress) lateinit var progress: View

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		ButterKnife.bind(this)
		ContactsHelper.DEBUG = true

		RxPermissions.getInstance(this)
				.request(Manifest.permission.READ_CONTACTS)
				.filter { it }
				.flatMap<Contact>(Func1 { RxContacts.getInstance(this).profile })
				.subscribe {
					Log.v("owner", it.toString())
					initList()
				}


	}

	private fun initList() {
		val timestamp = System.currentTimeMillis()
		val adapter = ContactsAdapter(this, null)
		recyclerView.adapter = adapter
		progress.visibility = View.GONE
		RxContacts
				.getInstance(this)
				.withPhones()
				.withEmails()
				.sort(Sorter.HAS_IMAGE)
				.filter(Filter.HAS_PHONE)
				.contacts
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(bindToLifecycle<Contact>())
				.subscribe {
					Toast.makeText(this, "time=${System.currentTimeMillis() - timestamp}ms items=${recyclerView.adapter.itemCount}", Toast.LENGTH_SHORT).show()
				}

	}

	private fun initListFast() {
		val timestamp = System.currentTimeMillis()
		val adapter = ContactsAdapter(this, null)
		recyclerView.adapter = adapter
		progress.visibility = View.VISIBLE
		RxContacts
				.getInstance(this)
				.contactsFast
				.filter { !it.phones.isEmpty() && it.photoUri != null }
				.toList()    // aggregate to list
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(bindToLifecycle<List<Contact>>())
				.subscribe({
					           progress.visibility = View.GONE
					           adapter.addAll(it)
				           }, { it.printStackTrace() }, {
					           Toast.makeText(this, "time=${System.currentTimeMillis() - timestamp}ms items=${recyclerView.adapter.itemCount}", Toast.LENGTH_SHORT).show()
				           })

	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.fill -> initList()
			R.id.fill_fast -> initListFast()
		}
		return super.onOptionsItemSelected(item)
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	class ContactsAdapter(private val context: Context, contacts: List<Contact>?) : RecyclerView.Adapter<ContactsAdapter.ContactHolder>() {
		internal var contacts: MutableList<Contact>

		init {
			if (contacts != null)
				this.contacts = ArrayList(contacts)
			else
				this.contacts = ArrayList()
		}

		fun add(c: Contact) {
			contacts.add(c)
			notifyItemChanged(contacts.size - 1)
		}

		fun addAll(newContacts: List<Contact>) {
			val from = contacts.size
			contacts.addAll(newContacts)
			notifyItemRangeChanged(from, this.contacts.size)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
			val v = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
			return ContactHolder(v)
		}

		override fun onBindViewHolder(h: ContactHolder, position: Int) {
			val c = contacts[position]
			h.name.text = c.name
			h.emails.text = TextUtils.join(", ", c.emails)
			h.phones.text = TextUtils.join(", ", c.phones)
			setVisibility(h.phones)
			setVisibility(h.emails)
			Glide.with(context).load(c.photoUri).into(h.image)

		}

		private fun setVisibility(textView: TextView) {
			textView.visibility = if (TextUtils.isEmpty(textView.text)) View.GONE else View.VISIBLE
		}

		override fun getItemCount(): Int {
			return contacts.size
		}

		class ContactHolder(view: View) : RecyclerView.ViewHolder(view) {

			@Bind(R.id.name) lateinit var name: TextView
			@Bind(R.id.image) lateinit var image: ImageView
			@Bind(R.id.phones) lateinit var phones: TextView
			@Bind(R.id.emails) lateinit var emails: TextView

			init {
				ButterKnife.bind(this, view)
			}
		}
	}


}
