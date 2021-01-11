package com.company.server;

import com.company.protocol.POP3Responses;
import com.company.utils.CRLFTerminatedReader;
import com.company.utils.Mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;

public class Session implements Runnable{

    private final POP3Server parentServer;
    private final ServerThread parentServerThread;

    private Socket socket;
    private InputStream input;
    private PrintWriter writer;
    private CRLFTerminatedReader reader;
    private POP3Responses responses;
    private Mailbox mailbox;

    private volatile boolean quitting = false;

    public enum State {
        IDLE,
        AUTHORIZATION,
        TRANSACTION,
        UPDATE
    }


    private State actualState = State.IDLE;

    public Session(POP3Server parentServer, ServerThread serverThread, Socket socket) throws IOException {
        this.parentServer = parentServer;
        this.parentServerThread = serverThread;
        this.setSocket(socket);
    }

    private void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.input = this.socket.getInputStream();
        this.reader = new CRLFTerminatedReader(this.input);
        this.writer = new PrintWriter(this.socket.getOutputStream());
        this.socket.setSoTimeout(this.parentServer.getTimeout());
    }

    public void run() {
        final String originalName = Thread.currentThread().getName();
        Thread.currentThread().setName(Session.class.getName()+"-"+socket.getInetAddress()+":"+socket.getPort());
        try {
            runCommandLoop();
        }
        catch (IOException e) {
            if(!this.quitting){
                try {
                    this.sendResponse(responses.ClosingTransmission(this.parentServer.getHostName()));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        catch (Throwable e) {
            try{
                this.sendResponse("421 4.3.0 Mail system failure, closing transmission channel");
            }
            catch (IOException ioException)
            {}

            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof Error)
                throw (Error) e;
            else
                throw new RuntimeException("Unexpected exception", e);
        }
        finally {
            this.closeConnection();
            this.parentServerThread.sessionEnded(this);
            Thread.currentThread().setName(originalName);
        }
    }

    private void closeConnection() {
        try {
            this.writer.close();
            this.input.close();
            if((this.socket != null) && this.socket.isBound() && !this.socket.isClosed())
                this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(String response) throws IOException
    {
        System.out.print(response);
        this.writer.print(response);
        this.writer.flush();
    }

    public void quit() {
        this.quitting = true;
        this.closeConnection();
    }

    private void runCommandLoop() throws IOException {
        if(this.parentServerThread.maxConnectionsAcquired())
        {
            this.sendResponse("421 Too many connections, try again later");
            return;
        }

        this.mailbox = new Mailbox();
        this.parentServer.getCommandHandler().greetings(this);
        this.actualState = State.AUTHORIZATION;

        while(!this.quitting) {
            try {
                String inputLine = null;
                try {
                    inputLine  = this.reader.readLine();
                    System.out.println("Client: "+inputLine);
                }catch (SocketException e) {
                    e.printStackTrace();
                    return;
                }

                if(inputLine.equals("") || inputLine == null) {
                    System.out.println("Input line is null");
                    return;
                }

                this.parentServer.getCommandHandler().handleCommand(this, inputLine);

            }
            catch (RejectedExecutionException e) {
                this.sendResponse(responses.ClosingTransmission(parentServer.getHostName()));
                return;
            }
            catch (CRLFTerminatedReader.TerminationException e) {
                this.sendResponse(responses.SyntaxError());
                return;
            }
            catch (CRLFTerminatedReader.MaxLineLengthException e) {
                this.sendResponse(responses.LengthError());
                return;
            }

        }

    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public void setMailbox(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

    public State getActualState() {
        return actualState;
    }

    public void setActualState(State actualState) {
        this.actualState = actualState;
    }
}
