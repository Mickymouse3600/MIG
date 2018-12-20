package co.example.mickymouse.mig;

import com.google.firebase.database.Exclude;

public class Upload {
    private String mName;
    private String mImageUrl;
    private String mDescription;
    private String mContact;
    private String mKey;



    public Upload(String name, String imageUrl, String description,String contact ) {

        if (name.trim().equals("")) {
            name = "No Name";
        }

        mName = name;
        mImageUrl = imageUrl;
        mDescription=description;
        mContact=contact;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getImageUrl() { return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public void setContact(String contact) {
        this.mContact = contact;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getContact() {
        return mContact;
    }


    @Exclude
    public String getKey() {
            return mKey; }

    @Exclude
    public void setKey(String key) {
            mKey = key; }
    }
