/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package rts_flight_control_system;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import sensor_systems.*;
import actuator_systems.*;

/**
 *
 * @author YEE ZHI YING
 */
public class FCS_Main {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection con = null;
        con = cf.newConnection();

        ScheduledExecutorService sensor = Executors.newScheduledThreadPool(1);
        sensor.scheduleAtFixedRate(new FCS_Logic(con), 0, 2, TimeUnit.SECONDS);

        sensor.scheduleAtFixedRate(new Altitude_Sensor(con), 0, 2, TimeUnit.SECONDS);
        sensor.scheduleAtFixedRate(new Speed_Indicator(con), 0, 2, TimeUnit.SECONDS);
        sensor.scheduleAtFixedRate(new Weather_Sensor(con), 0, 2, TimeUnit.SECONDS);
        sensor.scheduleAtFixedRate(new Cabin_Pressure_Sensor(con), 0, 2, TimeUnit.SECONDS);

    }

}



