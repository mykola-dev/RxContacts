package ds.rxcontacts.demo;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.bumptech.glide.Glide;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import ds.rxcontacts.*;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity {

    @Bind(R.id.recycler) RecyclerView recyclerView;
    @Bind(R.id.progress) View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ContactsHelper.DEBUG = true;

        RxPermissions.getInstance(this)
                     .request(Manifest.permission.READ_CONTACTS)
                     .filter(it -> it)
                     .flatMap(it -> RxContacts.getInstance(this)
                                              .getProfile())
                     .subscribe(it -> {
                         Log.v("owner", it.toString());
                         initList();
                     });


    }

    private void logTime(String message, long millis) {
        Log.v("#", message + " " + millis);
    }

    private void initList() {
        long timestamp = System.currentTimeMillis();
        final ContactsAdapter adapter = new ContactsAdapter(this, null);
        recyclerView.setAdapter(adapter);
        progress.setVisibility(View.GONE);
        RxContacts.getInstance(this)
                  .withPhones()
                  .withEmails()
                  .sort(Sorter.HAS_IMAGE)
                  //.filter(Filter.HAS_PHONE)
                  .getContacts()
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .compose(bindToLifecycle())
                  .subscribe(it -> {
                      if (ContactsHelper.DEBUG)
                          Log.v("contact", it.toString());
                      adapter.add(it);
                  }, Throwable::printStackTrace, () -> {
                      Toast.makeText(this, String.format("time=%sms items=%s", System.currentTimeMillis() - timestamp, recyclerView.getAdapter().getItemCount()),
                              Toast.LENGTH_SHORT).show();
                  });

    }

    private void initListFast() {
        long timestamp = System.currentTimeMillis();
        final ContactsAdapter adapter = new ContactsAdapter(this, null);
        recyclerView.setAdapter(adapter);
        progress.setVisibility(View.VISIBLE);
        RxContacts.getInstance(this)
                  .getContactsFast()
                  //.filter(it -> !it.phones.isEmpty()/* && it.photoUri != null*/)
                  .toList()    // aggregate to list
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .compose(bindToLifecycle())
                  .subscribe(it -> {
                      progress.setVisibility(View.GONE);
                      adapter.addAll(it);
                  }, Throwable::printStackTrace, () -> {
                      Toast.makeText(this, String.format("time=%sms items=%s", System.currentTimeMillis() - timestamp, recyclerView.getAdapter().getItemCount()),
                              Toast.LENGTH_SHORT).show();
                  });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fill:
                initList();
                break;
            case R.id.fill_fast:
                initListFast();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactHolder> {

        private Context context;
        List<Contact> contacts;

        public ContactsAdapter(Context context, List<Contact> contacts) {
            this.context = context;
            this.contacts = contacts != null ? contacts : new ArrayList<>();
        }

        public void add(Contact c) {
            contacts.add(c);
            notifyItemChanged(contacts.size() - 1);
        }

        public void addAll(List<Contact> newContacts) {
            int from = contacts.size();
            contacts.addAll(newContacts);
            notifyItemRangeChanged(from, this.contacts.size());
        }

        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            return new ContactHolder(v);
        }

        @Override
        public void onBindViewHolder(ContactHolder h, int position) {
            Contact c = contacts.get(position);
            h.name.setText(c.name);
            h.emails.setText(c.emails != null ? TextUtils.join(", ", c.emails) : "");
            h.phones.setText(c.phones != null ? TextUtils.join(", ", c.phones) : "");
            setVisibility(h.phones);
            setVisibility(h.emails);
            Glide.with(context).load(c.photoUri).into(h.image);

        }

        private void setVisibility(TextView textView) {
            textView.setVisibility(TextUtils.isEmpty(textView.getText()) ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        public static class ContactHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.name) TextView name;
            @Bind(R.id.image) ImageView image;
            @Bind(R.id.phones) TextView phones;
            @Bind(R.id.emails) TextView emails;

            public ContactHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }
    }


}
