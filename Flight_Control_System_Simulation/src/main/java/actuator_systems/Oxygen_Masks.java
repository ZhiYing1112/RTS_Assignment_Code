/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package actuator_systems;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author YEE ZHI YING
 */
public class Oxygen_Masks {

    boolean oxygenMasksReleasedStatus = false;
    //To receive from FCS
    String exchangeNameFromFCS = "FCSToOxygenMasks";
    String routingKeyFromFCS = "controller_to_oxygen_masks";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    public boolean isReleasedStatus() {
        return oxygenMasksReleasedStatus;
    }

    public void setReleasedStatus(boolean releasedStatus) {
        this.oxygenMasksReleasedStatus = releasedStatus;
    }

    public Oxygen_Masks(Connection con) throws IOException, TimeoutException {
        this.con = con;
        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(exchangeNameFromFCS, "direct");

        String oxygenMasks_qName = ch.queueDeclare().getQueue();

        ch.queueBind(oxygenMasks_qName, exchangeNameFromFCS, routingKeyFromFCS);

        //Receive and Consume message from altitude sensor
        receiveMessageFromFCS(oxygenMasks_qName);

    }

    private void receiveMessageFromFCS(String qName) throws IOException {
        ch.basicConsume(qName, true, (x, msg) -> {

            String instruction = new String(msg.getBody(), "UTF-8");
            System.out.println("OXYGEN MASKS: received instruction: " + instruction + ".");
            try {
                for (int i = 3; i >= 1; i--) {

                    System.out.println("OXYGEN MASKS: (Sudden cabin lost) releasing oxygen masks in " + i + "...");

                    Thread.sleep(100);

                }
                System.out.println("OXYGEN MASKS: oxygen masks released, release status: " + Boolean.toString(isReleasedStatus()).toUpperCase());
            } catch (InterruptedException ex) {
                Logger.getLogger(Oxygen_Masks.class.getName()).log(Level.SEVERE, null, ex);
            }

        }, x -> {
        });

    }

}
