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
public class Landing_Gear {

    boolean landingGearReleasedStatus = false;

    //To receive from FCS
    String exchangeNameFromFCS = "FCSToLandingGear";
    String routingKeyFromFCS = "controller_to_landing_gear";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    public boolean isLandingGearReleasedStatus() {
        return landingGearReleasedStatus;
    }

    public void setLandingGearReleasedStatus(boolean landingGearReleasedStatus) {
        this.landingGearReleasedStatus = landingGearReleasedStatus;
    }

    public Landing_Gear(Connection con) throws IOException, TimeoutException {
        this.con = con;
        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(exchangeNameFromFCS, "direct");

        String landingGear_qName = ch.queueDeclare().getQueue();

        ch.queueBind(landingGear_qName, exchangeNameFromFCS, routingKeyFromFCS);
        receiveMessageFromFCS(landingGear_qName);

    }

    private void receiveMessageFromFCS(String qName) throws IOException {
        ch.basicConsume(qName, true, (x, msg) -> {
            try {
                String instruction = new String(msg.getBody(), "UTF-8");

                System.out.println("LANDING GEAR: received instruction: " + instruction + ".");

                for (int i = 5; i >= 1; i--) {

                    System.out.println("LANDING GEAR: (Landing) releasing landing gear in " + i + "...");

                    Thread.sleep(200);

                }
                System.out.println("LANDING GEAR: landing gear released, release status: " + Boolean.toString(isLandingGearReleasedStatus()).toUpperCase());

            } catch (InterruptedException ex) {
                Logger.getLogger(Oxygen_Masks.class.getName()).log(Level.SEVERE, null, ex);
            }

        }, x -> {
        });
    }

}
