package com.wipro.payroll.common;

import java.io.Serializable;

public class UserAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private AddressType addressType;
    private String streetLine1;
    private String streetLine2;
    private String city;
    private String state;
    private String postcode;
    private String country;

    public UserAddress() {}

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public AddressType getAddressType() {
        return addressType;
    }
    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
    public String getStreetLine1() {
        return streetLine1;
    }
    public void setStreetLine1(String streetLine1) {
        this.streetLine1 = streetLine1;
    }
    public String getStreetLine2() {
        return streetLine2;
    }
    public void setStreetLine2(String streetLine2) {
        this.streetLine2 = streetLine2;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getPostcode() {
        return postcode;
    }
    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
}
