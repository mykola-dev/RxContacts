package ds.rxcontacts;

import android.provider.ContactsContract.Contacts;

public enum Sorter {
    FIRST_NAME(Contacts.DISPLAY_NAME_PRIMARY + " asc"),
    LAST_NAME(Contacts.DISPLAY_NAME_ALTERNATIVE + " asc"),
    HAS_IMAGE(Contacts.PHOTO_THUMBNAIL_URI + " desc"),
    HAS_PHONE(Contacts.HAS_PHONE_NUMBER + " desc");

    public String raw;

    Sorter(String raw) {
        this.raw = raw;
    }

}
