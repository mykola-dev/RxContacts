package ds.rxcontacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;

import java.util.*;

import rx.Subscriber;

import static android.provider.ContactsContract.CommonDataKinds;
import static android.provider.ContactsContract.Contacts;

public class ContactsHelper {

    private static final String TAG = "Contacts";

    public static boolean DEBUG = false;

    private static final String[] EMAIL_PROJECTION = new String[] {CommonDataKinds.Email.DATA, CommonDataKinds.Email.CONTACT_ID};
    private static final String[] PHONE_PROJECTION = new String[] {CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.CONTACT_ID};

    private static final String[] CONTACTS_PROJECTION = new String[] {
            Contacts._ID,
            Contacts.DISPLAY_NAME,
            Contacts.PHOTO_THUMBNAIL_URI,
            Contacts.HAS_PHONE_NUMBER
    };

    Context context;
    ContentResolver resolver;

    public ContactsHelper(Context ctx) {
        context = ctx;
        resolver = context.getContentResolver();
    }

    /**
     * Fetch device owner contact based on Google Account or 'Me' contact from address book
     * @return
     */
    @Nullable
    public Contact getProfileContact() {
        String filter = getAccountEmail();
        if (filter == null) {
            filter = getProfileName();
        }
        if (filter == null)
            return null;

        List<Contact> contacts = filter(filter, true, true);
        if (!contacts.isEmpty())
            return contacts.get(0);
        else
            return null;

    }

    /**
     * @param query      leave it null if you want all contacts
     * @param withEmails emails fetching can decrease performance, so 'false' is recommended
     * @return list with contacts data
     */
    @NonNull
    public List<Contact> filter(String query, boolean withPhones, boolean withEmails) {
        List<Contact> result = new ArrayList<>();

        Cursor c = getContactsCursor(query);

        while (c.moveToNext()) {
            Contact contact = fetchContact(c, withPhones, withEmails);
            result.add(contact);
        }
        c.close();

        if (DEBUG)
            log(result);

        return result;
    }

    public void emit(String query, boolean withPhones, boolean withEmails, Subscriber<? super Contact> subscriber) {
        Cursor c = getContactsCursor(query);
        while (c.moveToNext()) {
            Contact contact = fetchContact(c, withPhones, withEmails);
            subscriber.onNext(contact);
            if (DEBUG)
                Log.i("emit", contact.toString());
        }
        c.close();

        subscriber.onCompleted();
    }

    @NonNull
    private Contact fetchContact(Cursor c, boolean withPhones, boolean withEmails) {
        String id = c.getString(c.getColumnIndex(Contacts._ID));
        Contact contact = new Contact(id);
        contact.name = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));

        // photo thumb
        String thumbUri = c.getString(c.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI));
        contact.photoUri = thumbUri;

        // get phone numbers
        if (Integer.parseInt(c.getString(c.getColumnIndex(Contacts.HAS_PHONE_NUMBER))) > 0) {
            List<String> phones = getPhones(id);
            contact.phones.addAll(phones);
        }

        // get emails
        if (withEmails) {
            List<String> emails = getEmails(id);
            contact.emails.addAll(emails);
        }
        return contact;
    }

    private Cursor getContactsCursor(String query) {
        Uri uri;
        if (query == null) {
            uri = Contacts.CONTENT_URI;
        } else {
            uri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, query);
        }

        Cursor c = resolver.query(
                uri,
                CONTACTS_PROJECTION,
                null,
                null,
                null
        );

        return c;
    }

    private static String cleanPhone(String phone) {
        return phone.replaceAll("-|\\s|\\(|\\)|\\+", "");
    }

    private static void log(List<Contact> contacts) {
        Log.v(TAG, "=== contacts ===");
        for (Contact c : contacts) {
            Log.v(TAG, c.toString());
        }

    }

    private List<String> getPhones(String contactId) {
        List<String> result = new ArrayList<>();
        Cursor phoneCursor = resolver.query(
                CommonDataKinds.Phone.CONTENT_URI,
                PHONE_PROJECTION,
                CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[] {contactId},
                null
        );
        while (phoneCursor.moveToNext()) {
            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
            result.add(cleanPhone(phoneNumber));
        }
        phoneCursor.close();

        return result;
    }

    private List<String> getEmails(String contactId) {
        List<String> result = new ArrayList<>();
        Set<String> emailSet = new HashSet<>();
        Cursor emailCursor = resolver.query(
                CommonDataKinds.Email.CONTENT_URI,
                EMAIL_PROJECTION,
                CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[] {contactId},
                null
        );
        while (emailCursor.moveToNext()) {
            String email = emailCursor.getString(emailCursor.getColumnIndex(CommonDataKinds.Email.DATA));
            emailSet.add(email);
        }
        emailCursor.close();
        result.addAll(emailSet);

        return result;
    }

    /**
     * Utility method. Should'n rely on it by 100%
     * @return
     */
    @Nullable
    private String getAccountEmail() {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        for (Account account : accounts) {
            Log.v(TAG, "account:" + account.name);
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                return account.name;
            }
        }

        return null;
    }

    private String getProfileName() {
        Cursor c = resolver.query(
                ContactsContract.Profile.CONTENT_URI,
                null,
                null,
                null,
                null);
        String name = null;
        if (c.moveToFirst()) {
            String id = c.getString(c.getColumnIndex(Contacts._ID));
            name = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));
        }
        c.close();
        return name;
    }

}
