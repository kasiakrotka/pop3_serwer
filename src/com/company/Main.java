package com.company;

import com.company.server.POP3Server;

public class Main {

    public static void main(String[] args) {

        POP3Server server = new POP3Server();
        server.start();
    }
}
