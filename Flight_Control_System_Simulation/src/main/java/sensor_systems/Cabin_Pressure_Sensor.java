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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author YEE ZHI YING
 */
public class Cabin_Pressure_Sensor implements Runnable {

    Random rand = new Random();
    //direct exhange from cabin pressure sensor to controller
    String exchangeNameToFCS = "cabinPressureSensorToFCS";
    //routing key: send data to controller
    String routingKeyToFCS = "cabin_pressure_to_controller";


    //direct exhange from sensor to altitude
    String exchangeNameFromFCS = "cabinPressureSensorFromFCS";
    //routing key: receive data from controller
    String routingKeyFromFCS = "controller_to_cabin_pressure";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;

    int initialCabinPressure = 0;
    int newChangedCabinPressure = 0;
    boolean checkInitial = true;
    public boolean pressureLossStatus = false;

    public Cabin_Pressure_Sensor(Connection con) throws IOException, TimeoutException {
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

        if (checkInitial == true) {
            initialCabinPressure = getCabinPressure();
            System.out.println("CABIN PRESSURE SENSOR: cabin pressure of the plane is " + initialCabinPressure + ".");
            sendMessage(Integer.toString(initialCabinPressure));
            checkInitial = false;

        } else {

            System.out.println("CABIN PRESSURE SENSOR: cabin pressure of the plane is " + newChangedCabinPressure + ".");
            sendMessage(Integer.toString(newChangedCabinPressure));

        }
    }

    public int getCabinPressure() {
        int currentCabinPressure = 10;

        int change = rand.nextInt(5);

        if (rand.nextBoolean()) {
            change *= -1;
        }
        currentCabinPressure += change;
        return currentCabinPressure;
    }

//Send Message to Controller
    public void sendMessage(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToFCS, "direct");

            chan.basicPublish(exchangeNameToFCS, routingKeyToFCS, false, null, msg.getBytes("UTF-8"));

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Cabin_Pressure_Sensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//Receive Message from Controller    
    public void receiveMessage(String qName) throws IOException {
        chan.basicConsume(qName, true, (x, msg) -> {

            String newCP = new String(msg.getBody(), "UTF-8");
            newChangedCabinPressure = Integer.parseInt(newCP);

            System.out.println("CABIN PRESSURE SENSOR: (new) current cabin pressure is "
                    + newChangedCabinPressure + ".");
        }, x -> {
        });
    }

}
