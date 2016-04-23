package ds.rxcontacts;

import android.content.Context;

import rx.Observable;
import rx.Subscriber;

import static rx.Observable.*;

public class RxContacts /*extends Observable*/ {

    private static RxContacts instance;

    public static RxContacts getInstance(Context ctx) {
        if (instance == null)
            instance = new RxContacts(ctx);

        return instance;
    }

    ContactsHelper helper;

    private Observable<Contact> profileObservable;
    private Observable<Contact> contactsObservable;
    private Observable<Contact> contactsNoEmailsObservable;

    private RxContacts(Context ctx/*, OnSubscribe s*/) {
        //super(s);
        helper = new ContactsHelper(ctx);
    }

    /**
     * Same as getAll() but produce extra query to fetch emails
     * @return
     */
    public Observable<Contact> getAllWithEmails() {
        if (contactsObservable == null)
            contactsObservable = defer(() -> from(helper.filter(null, true, true))).cache();

        return contactsObservable;
    }

    public Observable<Contact> getAll() {
        if (contactsNoEmailsObservable == null)
            contactsNoEmailsObservable = defer(() -> from(helper.filter(null, true, false))).cache();

        return contactsNoEmailsObservable;
    }

    public Observable<Contact> getAllWithEmailsRx() {
        return Observable.create((Subscriber<? super Contact> subscriber) -> {
            helper.emit(null, true, true, subscriber);
        }).onBackpressureBuffer();
    }

    public Observable<Contact> get(String filter) {
        return defer(() -> from(helper.filter(filter, true, true)));
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
}
