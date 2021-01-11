package com.company.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Mailbox {
    private String user;
    private String pass;
    private ArrayList<Integer> msgToDelete;
    private ArrayList<Message> messages;


    public Mailbox() {
        user = null;
        pass = null;
        msgToDelete = null;
        messages = null;
    }

    //size in octets
    public long size(){
        long size = 0;
        for(Message msg : messages)
        {
            ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(msg.body);
            size = size + byteBuffer.position();

            for(String header : msg.getHeaders())
            {
                byteBuffer = StandardCharsets.UTF_8.encode(header);
                size = size + byteBuffer.position();
            }
        }
        return size;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public ArrayList<Integer> getMsgToDelete() {
        return msgToDelete;
    }

    public void setMsgToDelete(ArrayList<Integer> msgToDelete) {
        this.msgToDelete = msgToDelete;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }
}
