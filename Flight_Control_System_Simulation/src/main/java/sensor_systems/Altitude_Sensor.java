/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sensor_systems;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author YEE ZHI YING
 */
public class Altitude_Sensor implements Runnable {

    Random rand = new Random();
    //direct exhange from altitude to controller
    String exchangeNameToFCS = "altitudeSensorToFCS";
    //routing key: send data to controller
    String routingKeyToFCS = "altitude_to_controller";

    //direct exhange from sensor to altitude
    String exchangeNameFromFCS = "altitudeSensorFromFCS";
    //routing key: receive data from controller
    String routingKeyFromFCS = "controller_to_altitude";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;
    public String alt_qName;

    int initialAltitude = 0;
    int newChangedAltitude = 0;
    boolean checkInitialAlt = true;

    public programStartTime startTime = new programStartTime();
    public static ArrayList<Long> arrListStartTime = new ArrayList<Long>();

    public Altitude_Sensor(Connection con) throws IOException, TimeoutException, InterruptedException {
        this.con = con;
        con = cf.newConnection();

        chan = con.createChannel();
        chan.exchangeDeclare(exchangeNameFromFCS, "direct");

        alt_qName = chan.queueDeclare().getQueue();

        chan.queueBind(alt_qName, exchangeNameFromFCS, routingKeyFromFCS);

        receiveMessage(alt_qName);
    }

//benchmark purpose
    public Altitude_Sensor(){
    }
    
    @Override
    public void run() {

        startTime.setStartTime(new Date().getTime());
        arrListStartTime.add(startTime.getStartTime());
        
        if (checkInitialAlt == true) {
            initialAltitude = getAltitude();
            System.out.println("ALTITUDE SENSOR: plane is currently at " + initialAltitude + " altitude.");
            sendMessage(Integer.toString(initialAltitude));
            checkInitialAlt = false;
        } else {
            System.out.println("ALTITUDE SENSOR: plane is currently at " + newChangedAltitude + " altitude.");
            sendMessage(Integer.toString(newChangedAltitude));
        }

    }

    public int getAltitude() {
        int currentAlt = 5000;

        int altitudeChange = rand.nextInt(300);

        if (rand.nextBoolean()) {
            altitudeChange *= -1;
        }

        currentAlt += altitudeChange;

        return currentAlt;
    }

//Send Message to Controller
    public void sendMessage(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToFCS, "direct");
            chan.basicPublish(exchangeNameToFCS, routingKeyToFCS, false, null, msg.getBytes("UTF-8"));

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Altitude_Sensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//Receive Message from Controller    
    public void receiveMessage(String qName) throws IOException, InterruptedException {
        chan.basicConsume(qName, true, (x, msg) -> {
            String newAlt = new String(msg.getBody(), "UTF-8");
            newChangedAltitude = Integer.parseInt(newAlt);
            System.out.println("ALTITUDE SENSOR: (new) altitude is now " + newChangedAltitude);
        }, x -> {
        });
        Thread.sleep(100);
    }

//benchmark purpose
    class programStartTime {

        long startTime;

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

    }

}
