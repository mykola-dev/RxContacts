package ds.rxcontacts.demo

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions.RxPermissions
import com.trello.rxlifecycle.components.support.RxAppCompatActivity
import ds.rxcontacts.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_contact.view.*
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.*


class MainActivity : RxAppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
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
		recycler.adapter = adapter
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
				.subscribe ({
					            adapter.add(it)
				            }, {
					            it.printStackTrace()
				            }, {
					            Toast.makeText(this, "time=${System.currentTimeMillis() - timestamp}ms items=${recycler.adapter.itemCount}", Toast.LENGTH_SHORT).show()
				            })

	}

	private fun initListFast() {
		val timestamp = System.currentTimeMillis()
		val adapter = ContactsAdapter(this, null)
		recycler.adapter = adapter
		//progress.visibility = View.VISIBLE
		progress.visibility = View.GONE
		RxContacts
				.getInstance(this)
				.contactsFast
				.filter { !it.phones.isEmpty()/* && it.photoUri != null*/ }
				//.toList()    // aggregate to list
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(bindToLifecycle<Contact>())
				.subscribe({
					           adapter.add(it)
				           }, { it.printStackTrace() }, {
					           Toast.makeText(this, "time=${System.currentTimeMillis() - timestamp}ms items=${recycler.adapter.itemCount}", Toast.LENGTH_SHORT).show()
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
		val gray = ColorDrawable(Color.LTGRAY)

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
			val v = h.itemView;
			v.name.text = c.name
			v.emails.text = TextUtils.join(", ", c.emails)
			v.phones.text = TextUtils.join(", ", c.phones)
			setVisibility(v.phones)
			setVisibility(v.emails)
			Glide
					.with(context)
					.load(c.photoUri)
					.fallback(gray)
					.into(v.image)

		}

		private fun setVisibility(textView: TextView) {
			textView.visibility = if (TextUtils.isEmpty(textView.text)) View.GONE else View.VISIBLE
		}

		override fun getItemCount(): Int {
			return contacts.size
		}

		class ContactHolder(view: View) : RecyclerView.ViewHolder(view)
	}


}
