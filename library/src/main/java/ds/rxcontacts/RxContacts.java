package ds.rxcontacts;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import rx.Observable;
import rx.Subscriber;

public class RxContacts {

    private static RxContacts instance;

    boolean withPhones;
    boolean withEmails;
    private Sorter sorter;
    private Filter[] filter;

    public static RxContacts getInstance(Context ctx) {
        if (instance == null)
            instance = new RxContacts(ctx);

        instance.withPhones = false;
        instance.withEmails = false;
        instance.sorter = null;
        instance.filter = null;

        return instance;
    }

    private ContactsHelper helper;

    private Observable<Contact> profileObservable;
    private Observable<Contact> contactsObservable;

    private RxContacts(Context ctx) {
        helper = new ContactsHelper(ctx);
    }

    /**
     * Emmits device owner if available
     * @return
     */
    public Observable<Contact> getProfile() {
        if (profileObservable == null) {
            profileObservable = Observable.create(subscriber -> {
                Contact c = helper.getProfileContact();
                if (c != null)
                    subscriber.onNext(c);

                subscriber.onCompleted();
            });
            profileObservable = profileObservable.cache();
        }

        return profileObservable;
    }

    public ContactsHelper getContactsHelper() {
        return helper;
    }

    public Observable<Contact> getContacts() {
        if (contactsObservable == null)
            contactsObservable = Observable.create((Subscriber<? super Contact> subscriber) -> {
                emit(null, withPhones, withEmails,sorter,filter, subscriber);
            }).onBackpressureBuffer().serialize();

        return contactsObservable;
    }

    private void emit(String query, boolean withPhones, boolean withEmails, Sorter sorter, Filter[] filter, Subscriber<? super Contact> subscriber) {
        Cursor c = helper.getContactsCursor(query, sorter, filter);
        while (c.moveToNext()) {
            Contact contact = helper.fetchContact(c, withPhones, withEmails);
            if (!subscriber.isUnsubscribed())
                subscriber.onNext(contact);
            else
                break;

            if (ContactsHelper.DEBUG)
                Log.i("emit", contact.toString() + " is subscribed=" + !subscriber.isUnsubscribed());
        }
        c.close();

        subscriber.onCompleted();
    }

    public RxContacts withPhones() {
        withPhones = true;
        return this;
    }

    public RxContacts withEmails() {
        withEmails = true;
        return this;
    }

    public RxContacts sort(Sorter sorter) {
        this.sorter = sorter;
        return this;
    }

    public RxContacts filter(Filter... filter) {
        this.filter = filter;
        return this;
    }
}
