package com.info.findblood;

public class Donors {
    private String id;
    private String nameSurname;
    private String phone;
    private String bloodGroup;

    public Donors() {
    }

    public Donors(String id, String nameSurname, String phone, String bloodGroup) {
        this.id = id;
        this.nameSurname = nameSurname;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
    }

    public String getId() {
        return id;
    }

    public String getNameSurname() {
        return nameSurname;
    }


    public String getPhone() {
        return phone;
    }


    public String getBloodGroup() {
        return bloodGroup;
    }

}
