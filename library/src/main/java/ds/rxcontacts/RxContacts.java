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

        // reset conditions on each instance request
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
     * Emits device owner if available
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

    /**
     * Use it if you need low level access to the library methods
     * @return
     */
    public ContactsHelper getContactsHelper() {
        return helper;
    }

    /**
     * Run ContentResolver query and emit results to the Observable
     * @return
     */
    public Observable<Contact> getContacts() {
        if (contactsObservable == null)
            contactsObservable = Observable.create((Subscriber<? super Contact> subscriber) -> {
                emit(null, withPhones, withEmails, sorter, filter, subscriber);
            }).onBackpressureBuffer().serialize();

        return contactsObservable;
    }

    /**
     * Experimental!
     * Faster query. Additional conditions doesn't work (withEmails, withPhotos, filters, sorters). Use Rx filters instead
     * @return
     */
    public Observable<Contact> getContactsFast() {
        return Observable.create((Subscriber<? super Contact> subscriber) -> {
            emitFast(subscriber);
        }).onBackpressureBuffer().serialize();

    }

    /**
     * Run extra query on Phones table if needed
     * @return
     */
    public RxContacts withPhones() {
        withPhones = true;
        return this;
    }

    /**
     * Run extra query on Emails table for each contact
     * @return
     */
    public RxContacts withEmails() {
        withEmails = true;
        return this;
    }

    /**
     * Sort emitted contacts. This sort runs on sqlite query.
     * @param sorter
     * @return
     */
    public RxContacts sort(Sorter sorter) {
        this.sorter = sorter;
        return this;
    }

    /**
     * Filter contacts with specific conditions
     * @param filter
     * @return
     */
    public RxContacts filter(Filter... filter) {
        this.filter = filter;
        return this;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    private void emitFast(Subscriber<? super Contact> subscriber) {
        Cursor c = helper.getFastContactsCursor();
        if (c.getCount() != 0) {
            c.moveToNext();
            Contact contact;
            while ((contact = helper.fetchContactFast(c)) != null) {

                if (!subscriber.isUnsubscribed())
                    subscriber.onNext(contact);
                else
                    break;

                if (ContactsHelper.DEBUG)
                    Log.i("emit fast", contact.toString() + " is subscribed=" + !subscriber.isUnsubscribed());
            }
        }
        c.close();

        subscriber.onCompleted();
    }
}
