/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package benchmarking;

import actuator_systems.Tail_Flaps;
import actuator_systems.Wing_Flaps;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openjdk.jmh.annotations.*;
import rts_flight_control_system.FCS_Logic;
import sensor_systems.Altitude_Sensor;

/**
 *
 * @author YEE ZHI YING
 */
@State(Scope.Benchmark)
public class AltitudeSensor_BenchMark {

    public Connection benchMarkConnection;
    public int benchMarkDegreeWing = 10;
    public int benchMarkDegreeTail = 0;

    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 2, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1)
    @Fork(1)

//Step 1: Receive sensor reading from all sensors
    public void controllerReceiveFromAllSensor() throws IOException, TimeoutException, InterruptedException {

        FCS_Logic controller = new FCS_Logic(benchMarkConnection);
        controller.receiveSensorData();
    }

//Step 2: Wing flap receive instruction from controller
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 2, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1)
    @Fork(1)
    public void wingFlapReceiveFromController() throws IOException, TimeoutException, InterruptedException {

        Wing_Flaps wiflap = new Wing_Flaps(benchMarkDegreeWing, benchMarkConnection);
        wiflap.receiveMessageFromFCS(wiflap.wingflap_qName);

    }


    //Step 3: Tail flap receive instruction from controller
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 2, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1)
    @Fork(1)
    
    public void tailFlapReceiveFromController() throws IOException, TimeoutException, InterruptedException {

        Tail_Flaps taflap = new Tail_Flaps(benchMarkDegreeTail, benchMarkConnection);
        taflap.receiveMessageFromFCS(taflap.tailflap_qName);

    }

//Step 4: Altitude receive new data reading from controller after adjust wing flaps & tail flaps
    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 2, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1)
    @Fork(1)
    public void altSensorReceiveNewChangeFromController() throws IOException, TimeoutException, InterruptedException {
        Altitude_Sensor altSen = new Altitude_Sensor(benchMarkConnection);
        altSen.receiveMessage(altSen.alt_qName);
    }

}
