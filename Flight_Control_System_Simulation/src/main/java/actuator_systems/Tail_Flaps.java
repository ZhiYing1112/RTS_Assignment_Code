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
public class Tail_Flaps {
    int degree_angle;
    

    //To receive from FCS
    String exchangeNameFromFCS = "FCSToTailFlapActuator";
    String routingKeyFromFCS = "controller_to_tailflap";
    
    
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;
    public String tailflap_qName;

    public int getTailFlapAngle() {
        return degree_angle;
    }

    public void setTailFlapAngle(int degree_angle) {
        this.degree_angle = degree_angle;
    }

    public Tail_Flaps(int degree_angle, Connection con) throws IOException, TimeoutException {
        this.degree_angle = degree_angle;
        this.con = con;
        
        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(exchangeNameFromFCS, "direct");

        tailflap_qName = ch.queueDeclare().getQueue();

        ch.queueBind(tailflap_qName, exchangeNameFromFCS, routingKeyFromFCS);
        
        receiveMessageFromFCS(tailflap_qName);
        
    }
    
    public void receiveMessageFromFCS(String qName) throws IOException {
        ch.basicConsume(qName, true, (x, msg) -> {

            String tailflap_degree = new String(msg.getBody(), "UTF-8");
            String message = "TAIL FLAP: angle had adjusted to " + tailflap_degree + " degree.";
            System.out.println(message);

        }, x -> {
        });
    }
    
}
