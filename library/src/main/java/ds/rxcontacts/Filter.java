package ds.rxcontacts;

import android.provider.ContactsContract.Contacts;

public enum Filter {
    HAS_IMAGE(Contacts.PHOTO_THUMBNAIL_URI, "NOT NULL"),
    HAS_PHONE(Contacts.HAS_PHONE_NUMBER, ">0");
    //HAS_EMAILS(null, null);

    private String field;
    private String condition;

    Filter(String field, String condition) {
        this.field = field;
        this.condition = condition;
    }

    @Override
    public String toString() {
        return field + " " + condition;
    }
}
