package com.company.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class DatabaseHandler {

    private static final String url = "jdbc:postgresql://localhost:5432/postgres";
    private static final String user = "postgres";
    private static final String password = "password";

    private static final String FIND_MAILBOX_ID = "SELECT * FROM mailbox WHERE address = ?;";
    private static final String FIND_MAILBOX_ID_2 = "SELECT * FROM mailbox WHERE address = ? AND password = ?;";
    private static final String CHECK_PASS = "SELECT * FROM mailbox WHERE address = ? AND password = ?;";
    private static final String GET_MESSAGES = "SELECT * FROM message WHERE mailbox_id = ?;";
    private static final String GET_HEADERS = "SELECT * FROM header WHERE message_id = ?;";
    private static final String DELETE_MESSAGE = "DELETE FROM message WHERE id = ?;";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static long getUserId(String username, String password) {
        long id = 0;
        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(FIND_MAILBOX_ID_2);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next())
                id = resultSet.getLong("id");

            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return id;
    }

    public static boolean checkUser(String username) {
        int count = 0;
        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(FIND_MAILBOX_ID);
            preparedStatement.setString(1, username);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next())
                ++count;

            conn.close();
            return count > 0;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public boolean checkPass(String user, String pass) {
        int count = 0;
        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(CHECK_PASS);
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, pass);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next())
                ++count;

            conn.close();

            return count > 0;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public ArrayList<Message> getMessages(String username, String password) {
        ArrayList<Message> messages = new ArrayList<>();
        Message tempMsg;
        long mailbox_id = getUserId(username, password);

        if (mailbox_id == 0)
            return null;

        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(GET_MESSAGES);
            preparedStatement.setLong(1, mailbox_id);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            int i = 1;
            while (resultSet.next()) {
                tempMsg = new Message();
                tempMsg.setId(i);
                tempMsg.setDatabase_id(resultSet.getInt("id"));
                tempMsg.setBody(resultSet.getString("body"));
                tempMsg.setDate(resultSet.getTimestamp("date"));
                tempMsg.setSeen(resultSet.getBoolean("seen"));
                tempMsg.setSender(resultSet.getString("sender"));
                tempMsg.setSubject(resultSet.getString("subject"));

                messages.add(tempMsg);
                i++;
            }
            for (Message message : messages) {
                message.setHeaders(getHeaders(conn, message.getId()));
            }
            conn.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return messages;
    }

    private LinkedList<String> getHeaders(Connection conn, int id) {
        LinkedList<String> headers = new LinkedList<>();
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(GET_HEADERS);
            preparedStatement.setInt(1, id);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            String temp;
            while (resultSet.next()) {
                temp = resultSet.getString("content");
                headers.add(temp);
            }
            return headers;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return headers;
    }

    public void deleteMessages(ArrayList<Integer> msgToDelete) {

        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(DELETE_MESSAGE);
            if (msgToDelete != null)
                for (int i = 0; i < msgToDelete.size(); i++) {
                    preparedStatement.setInt(1, msgToDelete.get(i));
                    preparedStatement.executeUpdate();
                }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
}
