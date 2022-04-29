package com.appinostudio.android.sendadssms.models;

public class User {
    private String email;
    private String mobile;
    private String f_name;
    private String l_name;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getF_name() {
        return f_name;
    }

    public void setF_name(String f_name) {
        this.f_name = f_name;
    }

    public String getL_name() {
        return l_name;
    }

    public void setL_name(String l_name) {
        this.l_name = l_name;
    }

    public String getByValue(String value)
    {
        switch (value)
        {
            case "email":
                return this.getEmail();
            case "mobile":
                return this.getMobile();
            case "f_name":
                return this.getF_name();
            case "l_name":
                return this.getL_name();
            default:
                return "";
        }
    }
}
