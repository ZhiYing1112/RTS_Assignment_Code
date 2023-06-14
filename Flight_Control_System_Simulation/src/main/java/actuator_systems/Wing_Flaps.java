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

/**
 *
 * @author YEE ZHI YING
 */
public class Wing_Flaps {

    int degree_angle;
    //To receive from FCS
    String exchangeNameFromFCS = "FCSToWingFlapActuator";
    String routingKeyFromFCS = "controller_to_wingflap";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;
    
    public String wingflap_qName;

    public int getWingFlapAngle() {
        return degree_angle;
    }

    public void setWingFlapAngle(int degree_angle) {
        this.degree_angle = degree_angle;
    }

    public Wing_Flaps(int degree_angle, Connection con) throws IOException, TimeoutException {
        this.degree_angle = degree_angle;
        this.con = con;

        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(exchangeNameFromFCS, "direct");

        wingflap_qName = ch.queueDeclare().getQueue();

        ch.queueBind(wingflap_qName, exchangeNameFromFCS, routingKeyFromFCS);

        //Receive and Consume message from altitude sensor
        receiveMessageFromFCS(wingflap_qName);

    }

    public void receiveMessageFromFCS(String qName) throws IOException {
        ch.basicConsume(qName, true, (x, msg) -> {

            String wingflap_degree = new String(msg.getBody(), "UTF-8");
            String message = "WING FLAP: angle had adjusted to " + wingflap_degree + " degree.";
            System.out.println(message);
        }, x -> {
        });
    }

}
