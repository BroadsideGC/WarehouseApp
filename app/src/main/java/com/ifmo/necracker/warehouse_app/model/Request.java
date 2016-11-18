package com.ifmo.necracker.warehouse_app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by bigz on 18.11.16.
 */

public class Request {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("user")
    private int userId;
    @JsonProperty("code")
    private int uniqueCode;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("type")
    private RequestType type;
    @JsonProperty("status")
    private RequestStatus status;

    public Request(Long id, int userId, int uniqueCode, int amount)
    {
        this(id, userId, uniqueCode, amount, RequestType.BOOKED, RequestStatus.IN_PROGRESS);
    }

    @JsonCreator
    public Request(@JsonProperty("id") Long id, @JsonProperty("user") int userId, @JsonProperty("code") int uniqueCode,
                   @JsonProperty("amount")int amount, @JsonProperty("type") RequestType type, @JsonProperty("status") RequestStatus status)
    {
        this.id = id;
        this.userId = userId;
        this.uniqueCode = uniqueCode;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getUniqueCode() {
        return uniqueCode;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RequestType getType() {
        return type;
    }

    public RequestStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("Request [user = %d, order = %d, product = %d, amount = %d, type = %s, status = %s]",
                userId, id, uniqueCode, amount, type.toString(), status.toString());
    }

    public enum RequestType {
        BOOKED ("booked"),
        PAID("paid"),
        CANCELED("canceled");

        private String text;
        RequestType(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static RequestType getRequestTypeFromString(String type) {
            switch (type) {
                case "booked":
                    return BOOKED;
                case "paid":
                    return PAID;
                case "canceled":
                    return CANCELED;
                default:
                    return null;
            }
        }
    }

    public enum RequestStatus {
        COMPLETED ("complete"),
        IN_PROGRESS ("in progress"),
        CANCELED ("canceled");

        private String text;
        RequestStatus(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static RequestStatus getRequestStatusFromString(String type) {
            switch (type) {
                case "complete":
                    return COMPLETED;
                case "in progress":
                    return IN_PROGRESS;
                case "canceled":
                    return CANCELED;
                default:
                    return null;
            }
        }
    }
}
