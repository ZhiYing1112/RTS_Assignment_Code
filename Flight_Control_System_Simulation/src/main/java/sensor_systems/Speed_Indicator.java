/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sensor_systems;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author YEE ZHI YING
 */
public class Speed_Indicator implements Runnable {

    Random rand = new Random();
    //direct exhange from speed indicator to controller
    String exchangeNameToFCS = "speedIndicatorToFCS";
    //routing key: send data to controller
    String routingKeyToFCS = "speed_to_controller";

    //direct exhange from sensor to altitude
    String exchangeNameFromFCS = "speedIndicatorFromFCS";
    //routing key: receive data from controller
    String routingKeyFromFCS = "controller_to_speed";


    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;

    int initialSpeed = 0;
    int newChangedSpeed = 0;
    boolean checkInitialSpeed = true;

    public Speed_Indicator(Connection con) throws IOException, TimeoutException {
        this.con = con;

        con = cf.newConnection();

        chan = con.createChannel();
        chan.exchangeDeclare(exchangeNameFromFCS, "direct");

        String qName = chan.queueDeclare().getQueue();

        chan.queueBind(qName, exchangeNameFromFCS, routingKeyFromFCS);

        receiveMessage(qName);

    }

    @Override
    public void run() {
        try {

            if (checkInitialSpeed) {

                initialSpeed = getSpeed();
                System.out.println("SPEED INDICATOR: plane is currently flying at " + initialSpeed + " km/h.");
                sendMessage(Integer.toString(initialSpeed));
                checkInitialSpeed = false;

            } else {

                System.out.println("SPEED INDICATOR: plane is currently flying at " + newChangedSpeed + " km/h.");
                sendMessage(Integer.toString(newChangedSpeed));
            }

            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Speed_Indicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getSpeed() {
        int currentSpeed = 350;

        int speedChange = rand.nextInt(100);

        if (rand.nextBoolean()) {
            speedChange *= -1;
        }

        currentSpeed += speedChange;

        return currentSpeed;
    }


//Send Message to Controller
    public void sendMessage(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToFCS, "direct");

            chan.basicPublish(exchangeNameToFCS, routingKeyToFCS, false, null, msg.getBytes("UTF-8"));

            //System.out.println("Speed data sent to flight controller.");
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Speed_Indicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


//Receive Message from Controller    
    public void receiveMessage(String qName) throws IOException {
        chan.basicConsume(qName, true, (x, msg) -> {

            String newSpd = new String(msg.getBody(), "UTF-8");
            newChangedSpeed = Integer.parseInt(newSpd);
            System.out.println("SPEED INDICATOR: (new) current speed is " + newChangedSpeed + " km/h.");

        }, x -> {
        });
    }

}
