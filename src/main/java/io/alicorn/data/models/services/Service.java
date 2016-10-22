package io.alicorn.data.models.services;

import io.alicorn.data.models.ContactInfo;

import java.util.UUID;

public abstract class Service {
    enum ServiceType {
        Shelter,
        Food,
        Health,
        Education,
        Employment,
        Prevention
    }

    private String uuid;
    private ServiceType serviceType;
    ContactInfo contactInfo;

    protected Service(ServiceType serviceType) {
        this.serviceType = serviceType;
    }


    public String getUuid() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = serviceType.toString() + UUID.randomUUID().toString();
        }
        return this.uuid;
    }

//    abstract ContactInfo getContactInfo();
//    abstract void setContactInfo(ContactInfo contactInfo);
}
