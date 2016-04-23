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
import rx.Subscription;
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
                         initListRx();
                     });


    }

    private void logTime(String message, long millis) {
        Log.v("#", message + " " + millis);
    }

    private void initListRx() {
        long timestamp = System.currentTimeMillis();
        final ContactsAdapter adapter = new ContactsAdapter(this, null);
        recyclerView.setAdapter(adapter);
        progress.setVisibility(View.GONE);
        Subscription s = RxContacts.getInstance(this)
                                   .withPhones()
                                   .withEmails()
                                   .sort(Sorter.HAS_IMAGE)
                                   //.filter(Filter.HAS_EMAILS)
                                   .getContacts()
                                   .subscribeOn(Schedulers.io())
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .compose(bindToLifecycle())
                                   .subscribe(it -> {
                                       Log.v("contact", it.toString());
                                       adapter.add(it);
                                   }, Throwable::printStackTrace, () -> {
                                       Toast.makeText(this, "time=" + (System.currentTimeMillis() - timestamp) + "ms", Toast.LENGTH_SHORT).show();
                                   });

        // new Handler().postDelayed(() -> s.unsubscribe(), 100);

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
            h.emails.setText(TextUtils.join(", ", c.emails));
            h.phones.setText(TextUtils.join(", ", c.phones));
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
