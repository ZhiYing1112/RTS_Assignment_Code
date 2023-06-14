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
public class Engine {
    int speed;
    
    
    //To receive from FCS
    String exchangeNameFromFCS = "FCSToEngine";
    String routingKeyFromFCS = "controller_to_engine";
    
    
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Engine(Connection con) throws IOException, TimeoutException {
        this.con = con;
        
        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(exchangeNameFromFCS, "direct");

        String altitude_qName = ch.queueDeclare().getQueue();

        ch.queueBind(altitude_qName, exchangeNameFromFCS, routingKeyFromFCS);
        
        //Receive and Consume message from altitude sensor
        receiveMessageFromFCS(altitude_qName);
    }
    
    private void receiveMessageFromFCS(String qName) throws IOException {
        ch.basicConsume(qName, true, (x, msg) -> {

            String engine_speed = new String(msg.getBody(), "UTF-8");
            String message = "ENGINE: engine speed had adjusted to " + engine_speed + " rpm.";
            System.out.println(message);

        }, x -> {
        });
    }
    
}
