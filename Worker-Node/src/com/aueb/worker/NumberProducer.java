package com.aueb.worker;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class NumberProducer implements Runnable
{
    private ArrayList<String[]> buffer;
    private Object lock;
    private final int MAX_SIZE = 10;

    public NumberProducer(ArrayList<String[]> buffer , Object lock)
    {
        this.buffer = buffer;
        this.lock = lock;
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                Socket s = new Socket("localhost" , 6000);
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                String[] data = (String[])ois.readObject();
                s.close();

                synchronized(lock)
                {
                    while(buffer.size() >= MAX_SIZE)
                    {
                        lock.wait();
                    }
                    buffer.add(data);
                    lock.notifyAll();
                }
                Thread.sleep(100);
            }
            catch(Exception e)
            {
                try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            }
        }
    }
}