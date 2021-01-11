package com.company.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;

public class Message {
    int id;
    int database_id;
    String body;
    Date date;
    Boolean seen;
    String sender;
    String subject;
    LinkedList<String> headers;

    public long size(){
        long size = 0;
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(body);
        size = size + byteBuffer.position();
        for(String header: headers)
        {
            byteBuffer = StandardCharsets.UTF_8.encode(header);
            size = size + byteBuffer.position();
        }
        return size;
    }

    public int getDatabase_id() {
        return database_id;
    }

    public void setDatabase_id(int database_id) {
        this.database_id = database_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LinkedList<String> getHeaders() {
        return headers;
    }

    public void setHeaders(LinkedList<String> headers) {
        this.headers = headers;
    }

    public long octets() {
        long size = 0;
        for(int i = 0; i < headers.size(); i ++){
            size = size + headers.get(i).getBytes().length;
        }
        size = size + body.getBytes().length;
        return size;
    }
}
