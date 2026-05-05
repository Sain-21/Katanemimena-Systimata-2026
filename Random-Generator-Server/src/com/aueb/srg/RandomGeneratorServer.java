package com.aueb.srg;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class RandomGeneratorServer {
    private static final int PORT = 6000;
    //map pou krataei tin oura kai to secret gia kathe paixnidi
    private static HashMap<String , ArrayList<String[]>> queues = new HashMap<String , ArrayList<String[]>>();

    public static void main(String[] args) 
    {
        try 
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[SRG] Server started on port " + PORT);

            while (true) 
            {
                // apodoxi sindesis
                final Socket s = serverSocket.accept();

                // dimiourgia kai start tou thread
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        handle(s);
                    }
                });
                t.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void handle(Socket s)
    {
        try
        {
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            Object request = in.readObject();

            //master stelnei add_game
            if(request instanceof String[])
            {
                String[] reqArray = (String[]) request;
                if(reqArray[0].equals("ADD_GAME"))
                {
                    final String name = reqArray[1];
                    final String secret = reqArray[2];
                    synchronized(queues)
                    {
                        if(queues.containsKey(name))
                        {
                            out.writeObject("ALREADY_EXISTS");
                            return;
                        }
                    }
                    final ArrayList<String[]>queue = new ArrayList<String[]>();

                    synchronized(queues)
                    {
                        queues.put(name,queue);
                    }

                    //dimiourgia producer Thread gia auto to paixnidi
                    Thread producer = new Thread(new Runnable() {
                        public void run(){
                            Random r = new Random();
                            while(true)
                            {
                                try
                                {
                                    int num = r.nextInt(1000) + 1;
                                    String hash = hashData(num, secret);
                                    //kleidoma stin sugkekrimeni oura tou paixti
                                    synchronized(queue)
                                    {
                                        while(queue.size() >= 10)
                                        {
                                            queue.wait();
                                        }
                                        queue.add(new String[]{String.valueOf(num), hash});
                                        queue.notifyAll();//ksupname worker pou isos perimenei
                                    }
                                    Thread.sleep(100);
                                }
                                catch(Exception e)
                                {
                                    break;
                                }
                            }
                        }
                    });
                    producer.start();
                    out.writeObject("OK");
                }
            }
            //worker zhta arithmo stelnontas to onoma tou paixnidiou
            else if(request instanceof String)
            {
                String gameName = (String) request;
                ArrayList<String[]>queue = null;

                //kleidoma gia na paroume sosti oura
                synchronized(queues)
                {
                    queue = queues.get(gameName);
                }

                if(queue != null)
                {
                    //kleidoma stin oura tou sigkekrimenou paixnidiou gia afairesi
                    synchronized(queue)
                    {
                        //perimenoume an i oura einai adeia
                        while(queue.isEmpty())
                        {
                            queue.wait();
                        }
                        String[] result = queue.remove(0);
                        queue.notifyAll(); // ksupname ton producer gia na balei neo arithmo
                        out.writeObject(result);
                    }
                }
                else
                {
                    out.writeObject(new String[]{"-1" , "ERR"});
                }
            }
            s.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static String hashData(int num, String secret) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((num + secret).getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}