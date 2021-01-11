package com.company.protocol;

import com.company.server.POP3Server;
import com.company.server.Session;
import com.company.utils.DatabaseHandler;
import com.company.utils.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class CommandHandler {

    private final String[] commands = {"USER", "PASS", "STAT", "LIST", "RETR", "DELE", "NOOP", "LAST", "RSET", "QUIT", "TOP"};
    private final ArrayList<String> commandsArray;
    private final POP3Server parentServer;
    private final POP3Responses responses;
    private final DatabaseHandler databaseHandler;

    public CommandHandler() {
        this.parentServer = null;
        this.databaseHandler = new DatabaseHandler();
        this.responses = new POP3Responses();
        this.commandsArray = new ArrayList<>(Arrays.asList(commands));
    }

    public CommandHandler(POP3Server server) {
        this.databaseHandler = new DatabaseHandler();
        this.parentServer = server;
        responses = new POP3Responses();
        commandsArray = new ArrayList<>(Arrays.asList(commands));
    }

    public String getCommandName(String command) {
        if (command != null) {
            if (command.substring(0, 3).toUpperCase().equals("TOP"))
                return command.substring(0, 3).toUpperCase();
            if (command.length() == 4)
                return command.toUpperCase();
            else {
                String commandName = command.substring(0, 4);
                return commandName.toUpperCase();
            }
        }
        return null;
    }

    public Boolean checkCommandExists(String commandName) {
        return commandsArray.contains(commandName);
    }

    public void greetings(Session parentSession) throws IOException {
        parentSession.sendResponse(responses.serverGreetings(parentServer.getHostName()));
    }

    public void handleCommand(Session session, String commandLine) throws IOException {
        String commandName = this.getCommandName(commandLine);
        String commandMsg = "";
        if (commandLine.length() > 4)
            commandMsg = commandLine.substring(4);
        Session parentSession = session;
        if (!commandsArray.contains(commandName))
            parentSession.sendResponse(responses.error(null));
        else
            switch (commandName) {
                case "USER":
                    handleUSER(parentSession, commandMsg);
                    break;
                case "PASS":
                    handlePASS(parentSession, commandMsg);
                    break;
                case "STAT":
                    handleSTAT(parentSession);
                    break;
                case "LIST":
                    handleLIST(parentSession, commandMsg);
                    break;
                case "RETR":
                    handleRETR(parentSession, commandMsg);
                    break;
                case "DELE":
                    handleDELE(parentSession, commandMsg);
                    break;
                case "TOP":
                    handleTOP(parentSession, commandMsg);
                    break;
                case "NOOP":
                    parentSession.sendResponse(responses.success(null));
                    break;
                case "LAST":
                    handleLAST(parentSession);
                    break;
                case "RSET":
                    handleRSET(parentSession);
                    break;
                case "QUIT":
                    handleQUIT(parentSession);
                    break;
                default:
                    break;
            }
    }

    private void handleTOP(Session parentSession, String commandMsg) throws IOException {
        String[] args = commandMsg.split(" ");
        if (args.length == 2) {
            parentSession.sendResponse(responses.success(null));
            int id = Integer.parseInt(args[0]);
            int lines = Integer.parseInt(args[1]);
            Message message = parentSession.getMailbox().getMessages().get(id - 1);
            topMessage(parentSession, message, lines);

        } else
            parentSession.sendResponse(responses.error(null));
    }

    private void handleLAST(Session parentSession) {
    }

    private void handleQUIT(Session parentSession) throws IOException {
        parentSession.sendResponse(responses.success(null));
        parentSession.setActualState(Session.State.UPDATE);
        databaseHandler.deleteMessages(parentSession.getMailbox().getMsgToDelete());
        parentSession.quit();
    }

    private void handleRSET(Session parentSession) throws IOException {
        parentSession.getMailbox().setMsgToDelete(null);
        parentSession.sendResponse(responses.success(null));
    }

    private void handleDELE(Session parentSession, String commandMsg) throws IOException {
        String id = commandMsg.replaceAll("\\s+", "");
        try {
            int id_num = Integer.parseInt(id);
            int database_id = parentSession.getMailbox().getMessages().get(id_num).getDatabase_id();
            if (parentSession.getMailbox().getMsgToDelete().contains(database_id))
                parentSession.sendResponse(responses.error("message already marked to delete"));
            else {
                parentSession.getMailbox().getMsgToDelete().add(database_id);
                parentSession.sendResponse(responses.success(null));
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            parentSession.sendResponse(responses.error("bad syntax"));
        }
    }

    private void handleRETR(Session parentSession, String commandMsg) throws IOException {
        String id = commandMsg.replaceAll("\\s+", "");
        if (id.equals("")) {
            for (Message msg : parentSession.getMailbox().getMessages()) {
                sendMessage(parentSession, msg);
            }
        } else {
            try {
                int id_num = Integer.parseInt(id);
                Message message = parentSession.getMailbox().getMessages().get(id_num - 1);
                long octets = message.octets();
                parentSession.sendResponse(responses.success(Long.toString(octets)));
                sendMessage(parentSession, message);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                parentSession.sendResponse(responses.error("bad syntax"));
            }
        }

    }

    private void topMessage(Session parentSession, Message message, int lines) throws IOException {
        for (int i = 0; i < message.getHeaders().size(); i++) {
            parentSession.sendResponse(message.getHeaders().get(i) + "\r\n");
        }
        String parsedLine = "";
        parentSession.sendResponse("\r\n");
        String[] bodylist = message.getBody().split("\r\n");
        int n = 0;
        if (lines > bodylist.length)
            n = bodylist.length;
        else
            n = lines;
        for (int i = 0; i < n; i++) {
            if (!bodylist[i].equals(""))
                if (bodylist[i].charAt(0) == '.') {
                    parsedLine = ".";
                    parsedLine = parsedLine.concat(bodylist[i]);
                } else {
                    parsedLine = bodylist[i];
                }
            else
                parsedLine = bodylist[i];
            parsedLine = parsedLine.concat("\r\n");
            parentSession.sendResponse(parsedLine);
        }
            parentSession.sendResponse("\r\n");
            parentSession.sendResponse(".\r\n");
    }

    private void sendMessage(Session parentSession, Message message) throws IOException {
        for (int i = 0; i < message.getHeaders().size(); i++) {
            parentSession.sendResponse(message.getHeaders().get(i) + "\r\n");
        }
        String parsedLine = "";
        parentSession.sendResponse("\r\n");
        String[] bodylist = message.getBody().split("\r\n");
        if (bodylist.length > 0)
            for (String line : bodylist) {
                if (!line.equals(""))
                    if (line.charAt(0) == '.') {
                        parsedLine = ".";
                        parsedLine = parsedLine.concat(line);
                    } else {
                        parsedLine = line;
                    }
                else
                    parsedLine = line;
                parsedLine = parsedLine.concat("\r\n");
                parentSession.sendResponse(parsedLine);
            }
        parentSession.sendResponse("\r\n");
        parentSession.sendResponse(".\r\n");
    }

    private void handleLIST(Session parentSession, String commandMsg) throws IOException {
        String id = commandMsg.replaceAll("\\s+", "");

        if (id.equals("")) {
            handleSTAT(parentSession);
            for (int i = 1; i < parentSession.getMailbox().getMessages().size() + 1; i++) {
                long bytes = parentSession.getMailbox().getMessages().get(i - 1).size();
                parentSession.sendResponse(i + " " + bytes + "\r\n");
            }
        } else {
            try {
                int id_num = Integer.parseInt(id);
                long bytes = parentSession.getMailbox().getMessages().get(id_num - 1).size();
                parentSession.sendResponse(responses.success(Long.toString(bytes)));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                parentSession.sendResponse(responses.error("bad syntax"));
            }
        }
    }

    private void handleSTAT(Session parentSession) throws IOException {
        if (parentSession.getActualState() != Session.State.TRANSACTION) {
            parentSession.sendResponse(responses.badSequence());
        } else {
            int num_of_msg = parentSession.getMailbox().getMessages().size();
            long size_of_mailbox = parentSession.getMailbox().size();
            parentSession.sendResponse(responses.stat(num_of_msg, size_of_mailbox));
        }
    }

    private void handlePASS(Session parentSession, String commandMsg) throws IOException {
        if (parentSession.getActualState() == Session.State.AUTHORIZATION && parentSession.getMailbox().getUser() != null) {
            String pass = commandMsg.replaceAll("\\s+", "");
            if (databaseHandler.checkPass(parentSession.getMailbox().getUser(), pass)) {
                parentSession.setActualState(Session.State.TRANSACTION);
                parentSession.getMailbox().setPass(pass);
                parentSession.getMailbox().setMessages(databaseHandler.getMessages(parentSession.getMailbox().getUser(), pass));
                parentSession.sendResponse(responses.success(null));
            } else {
                parentSession.sendResponse(responses.error("Incorrect password"));
            }
        } else {
            parentSession.sendResponse(responses.badSequence());
        }
    }

    private void handleUSER(Session parentSession, String commandMsg) throws IOException {
        if (parentSession.getActualState() == Session.State.AUTHORIZATION && parentSession.getMailbox().getPass() == null) {
            String username = commandMsg.replaceAll("\\s+", "");
            if (DatabaseHandler.checkUser(username)) {
                parentSession.getMailbox().setUser(username);
                parentSession.sendResponse(responses.success(null));
            } else {
                parentSession.sendResponse(responses.error("User dont exists"));
            }
        } else {
            parentSession.sendResponse(responses.badSequence());
        }
    }
}
