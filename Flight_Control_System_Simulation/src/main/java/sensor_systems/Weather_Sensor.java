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
public class Weather_Sensor implements Runnable {

    Random rand = new Random();
    //direct exhange from weather sensor to controller
    String exchangeNameToFCS = "weatherSensorToFCS";
    //routing key: send data to controller
    String routingKeyToFCS = "weather_sensor_to_controller";

    //direct exhange from sensor to altitude
    String exchangeNameFromFCS = "weatherSensorFromFCS";
    //routing key: receive data from controller
    String routingKeyFromFCS = "controller_to_weather_sensor";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;

    //Sample weather data
    String[] weather = {"sunny", "cloudy", "turbulence", "thunderstorm"};
    String currentWeather = "";
    int index = 0;
    int count = 0;

    public Weather_Sensor(Connection con) {
        this.con = con;
    }
    

    @Override
    public void run() {
        String weatherCon = getWeatherCondition();
        System.out.println("WEATHER SENSOR: Current weather is " + weatherCon.toUpperCase() + ".");
        sendMessage(weatherCon);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Weather_Sensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getWeatherCondition() {

        currentWeather = weather[index];
        count++;
        if (count == 3) {
            index = (index + 1) % weather.length;
            count = 0;
        }

        return currentWeather;

    }

    public void sendMessage(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToFCS, "direct");

            chan.basicPublish(exchangeNameToFCS, routingKeyToFCS, false, null, msg.getBytes("UTF-8"));

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Weather_Sensor.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }  

}


