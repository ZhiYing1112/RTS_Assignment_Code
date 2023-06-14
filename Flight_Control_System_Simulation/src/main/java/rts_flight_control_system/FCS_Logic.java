/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rts_flight_control_system;

import actuator_systems.Engine;
import actuator_systems.Landing_Gear;
import actuator_systems.Oxygen_Masks;
import actuator_systems.Tail_Flaps;
import actuator_systems.Wing_Flaps;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sensor_systems.Altitude_Sensor;
import sensor_systems.Cabin_Pressure_Sensor;

/**
 *
 * @author YEE ZHI YING
 */
public class FCS_Logic implements Runnable {

    SecureRandom secureRand = new SecureRandom();
    Random rand = new Random();

    //To receive from altitude sensor
    String exchangeNameFromAltitudeSensor = "altitudeSensorToFCS";
    String routingKeyFromAltitudeSensor = "altitude_to_controller";

//**Altitude Landing
    String routingKeyFromAltitudeSensorLanding = "altitude_to_controller_landing";

    //To receive from weather sensor
    String exchangeNameFromWeatherSensor = "weatherSensorToFCS";
    String routingKeyFromWeatherSensor = "weather_sensor_to_controller";

    //To receive from speed sensor
    String exchangeNameFromSpeedSensor = "speedIndicatorToFCS";
    String routingKeyFromSpeedSensor = "speed_to_controller";

//**Speed Landing
    String routingKeyFromSpeedSensorLanding = "speed_to_controller_landing";

    //To receive from cabin pressure sensor
    String exchangeNameFromCabinPressure = "cabinPressureSensorToFCS";
    String routingKeyFromCabinPressure = "cabin_pressure_to_controller";

//**Cabin Pressure Landing
    String routingKeyFromCabinPressureLanding = "cabin_pressure_to_controller";

    //To send to wing flap 
    String exchangeNameToWingFlapActuator = "FCSToWingFlapActuator";
    String routingKeyToWingFlapActuator = "controller_to_wingflap";

    //To send to tail flap 
    String exchangeNameToTailFlapActuator = "FCSToTailFlapActuator";
    String routingKeyToTailFlapActuator = "controller_to_tailflap";

    //To send to engine 
    String exchangeNameToEngine = "FCSToEngine";
    String routingKeyToEngine = "controller_to_engine";

    //To send to oxygen masks
    String exchangeNameToOxygenMasks = "FCSToOxygenMasks";
    String routingKeyToOxygenMasks = "controller_to_oxygen_masks";

    //To send to landing gear
    String exchangeNameToLandingGear = "FCSToLandingGear";
    String routingKeyToLandingGear = "controller_to_landing_gear";

    //To send new altitude to altitude sensor
    String exchangeNameToAltitudeSensor = "altitudeSensorFromFCS";
    String routingKeyToAltitudeSensor = "controller_to_altitude";

    //To send new plane speed to speed sensor
    String exchangeNameToSpeedSensor = "speedIndicatorFromFCS";
    String routingKeyToSpeedSensor = "controller_to_speed";

    //To send new cabin pressure to cabin pressure sensor
    String exchangeNameToCabinPressure = "cabinPressureSensorFromFCS";
    String routingKeyToCabinPressure = "controller_to_cabin_pressure";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel altitude_ch, weather_ch, speed_ch, cabinpressure_ch;
    String altitude_qName, weather_qName, speed_qName, cabinpressure_qName;

    Wing_Flaps wingflap;
    Tail_Flaps tailflap;
    Engine engine;
    Landing_Gear landingGear;
    Oxygen_Masks oxygenMasks;

    //global sensor data
    String weatherCon = "";
    int altitude = 0;
    int planeSpeed = 0; // km/h
    int cabinPressure = 0;

    int altitudeDiff_W, altitudeDiff_T = 0;
    int newAltitude = 0;
    int planeSpeedDiff = 0; // rpm - rotations per minute
    int newPlaneSpeed = 0;
    int newCabinPressure = 0;

    boolean cabinPressureLoss = false;
    boolean landingStatus = false;

    long endTime, startTime;

    public FCS_Logic(Connection con) throws IOException, TimeoutException {
        this.con = con;
        con = cf.newConnection();

        //Create channel for all sensors
        altitude_ch = con.createChannel();
        weather_ch = con.createChannel();
        speed_ch = con.createChannel();
        cabinpressure_ch = con.createChannel();

        //Declare "direct" as exchange type
        altitude_ch.exchangeDeclare(exchangeNameFromAltitudeSensor, "direct");
        weather_ch.exchangeDeclare(exchangeNameFromWeatherSensor, "direct");
        speed_ch.exchangeDeclare(exchangeNameFromSpeedSensor, "direct");
        cabinpressure_ch.exchangeDeclare(exchangeNameFromCabinPressure, "direct");

        //Get channels queue name from all sensors
        altitude_qName = altitude_ch.queueDeclare().getQueue();
        weather_qName = weather_ch.queueDeclare().getQueue();
        speed_qName = speed_ch.queueDeclare().getQueue();
        cabinpressure_qName = cabinpressure_ch.queueDeclare().getQueue();

        altitude_ch.queueBind(altitude_qName, exchangeNameFromAltitudeSensor, routingKeyFromAltitudeSensor);
        weather_ch.queueBind(weather_qName, exchangeNameFromWeatherSensor, routingKeyFromWeatherSensor);
        speed_ch.queueBind(speed_qName, exchangeNameFromSpeedSensor, routingKeyFromSpeedSensor);
        cabinpressure_ch.queueBind(cabinpressure_qName, exchangeNameFromCabinPressure, routingKeyFromCabinPressure);

        //Initiating a wingflap object with constructor - to set and get degree
        wingflap = new Wing_Flaps(10, con);
        tailflap = new Tail_Flaps(0, con);
        engine = new Engine(con);
        landingGear = new Landing_Gear(con);
        oxygenMasks = new Oxygen_Masks(con);

    }

    @Override
    public void run() {
        try {

            if (cabinPressureLoss == false) {
                //For receiving all sensor data
                receiveSensorData();

                Thread.sleep(1000);
                //Based on weather to adjust wing flap, tail flap, 
                weatherLogicToAdjustActuator();
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Weather adjustment - altitude, speed
    private void weatherLogicToAdjustActuator() throws InterruptedException, IOException, TimeoutException {

        if (altitude != 0 && planeSpeed != 0 && !weatherCon.isEmpty() && cabinPressure != 0) {

            if (altitude >= 8000 && planeSpeed >= 900 && "thunderstorm".equals(weatherCon)) {

                int pressureDiff = -15;
                newCabinPressure = cabinPressure + pressureDiff;
                cabinPressureLoss = true;
                triggerCabinLoss();

            } else if ("turbulence".equals(weatherCon) || "thunderstorm".equals(weatherCon)) {
                System.out.println("FLIGHT CONTROLLER: received altitude from altitude sensor: " + altitude);
                System.out.println("FLIGHT CONTROLLER: received plane speed from speed sensor: " + planeSpeed);
                System.out.println("FLIGHT CONTROLLER: received weather condition from weather sensor: " + weatherCon);
                System.out.println("FLIGHT CONTROLLER: received cabin pressure from pressure sensor: " + cabinPressure);

                altLogicToAdjustBadWeatCon();

            } else if ("sunny".equals(weatherCon) || "cloudy".equals(weatherCon)) {

                System.out.println("FLIGHT CONTROLLER: received altitude from altitude sensor: " + altitude);
                System.out.println("FLIGHT CONTROLLER: received plane speed from speed sensor: " + planeSpeed);
                System.out.println("FLIGHT CONTROLLER: received weather condition from weather sensor: " + weatherCon);
                System.out.println("FLIGHT CONTROLLER: received cabin pressure from pressure sensor: " + cabinPressure);

                altLogicToAdjustForNormalFlying();
            }

            System.out.println("");
        }
    }

    //Good weather - sunny, cloudy 
    private void altLogicToAdjustForNormalFlying() throws InterruptedException {
        if (altitude <= 4800) {

            adjustFlightParameters(50, 30, 60);

        } else if (altitude > 4800 && altitude <= 5000) {

            adjustFlightParameters(40, 25, 65);

        } else if (altitude > 5000 && altitude <= 5200) {

            adjustFlightParameters(30, 20, 70);

        } else if (altitude > 5200 && altitude <= 5400) {

            adjustFlightParameters(20, 15, 75);

        } else if (altitude > 5200 && altitude <= 5500) {

            adjustFlightParameters(15, 10, 80);

        } else if (altitude > 5500) {

            adjustFlightParameters(10, 5, 90);
        }

    }

    //Set adjustment for actuator - wing flap, tail flap, engine
    private void adjustFlightParameters(int wingFlapDegree, int tailFlapDegree, int engineSpeed) throws InterruptedException {

        System.out.println("------ MAKING ADJUSTMENTS FOR NOMRAL FLYING IN GOOD WEATHER -------");

        Thread.sleep(200);

        wingflap.setWingFlapAngle(wingFlapDegree);
        tailflap.setTailFlapAngle(tailFlapDegree);
        engine.setSpeed(engineSpeed);

        System.out.println("FLIGHT CONTROLLER: adjust wing flaps to " + wingflap.getWingFlapAngle() + " degree.");
        System.out.println("FLIGHT CONTROLLER: adjust tail flaps to " + tailflap.getTailFlapAngle() + " degree.");
        System.out.println("FLIGHT CONTROLLER: adjust engine speed to " + engine.getSpeed() + " rpm.");

        sendMessageToWingFlap(Integer.toString(wingFlapDegree));
        sendMessageToTailFlap(Integer.toString(tailFlapDegree));
        sendMessageToEngine(Integer.toString(engineSpeed));
        System.out.println("FLIGHT CONTROLLER: current adjusted cabin pressure: " + monitoringCabinPressure());

        changeInAltitudeFromWingAndTailFlap();
        changeInSpeedFromEngine();
        sendNewAltitude(Integer.toString(newAltitude));
        sendNewPlaneSpeed(Integer.toString(newPlaneSpeed));
        sendNewCabinPressure(Integer.toString(monitoringCabinPressure()));

    }

    //Change altitude for good weather
    private void changeInAltitudeFromWingAndTailFlap() throws InterruptedException {
        Thread.sleep(500);

        int wingFlapAngle = wingflap.getWingFlapAngle();
        int tailFlapAngle = tailflap.getTailFlapAngle();

        switch (wingFlapAngle) {
            case 50:
            case 40: {
                altitudeDiff_W = ThreadLocalRandom.current().nextInt(51) + 50;
                break;
            }

            case 30:
            case 20: {
                altitudeDiff_W = ThreadLocalRandom.current().nextInt(51) + 100;
                break;
            }

            case 15:
            case 10: {
                altitudeDiff_W = ThreadLocalRandom.current().nextInt(51) + 150;
                break;
            }
            default:
                altitudeDiff_W = ThreadLocalRandom.current().nextInt(51) + 30;
                break;
        }

        switch (tailFlapAngle) {
            case 30:
            case 25:
            case 20: {
                altitudeDiff_T = ThreadLocalRandom.current().nextInt(51) + 30;
                break;
            }
            case 15: {
                altitudeDiff_T = ThreadLocalRandom.current().nextInt(51) + 40;
                break;
            }
            case 10:
            case 5: {
                altitudeDiff_T = ThreadLocalRandom.current().nextInt(51) + 60;
                break;
            }
            default:
                altitudeDiff_T = ThreadLocalRandom.current().nextInt(51) + 20;
                break;
        }

        if (altitude >= 6000) {
            newAltitude = altitude - (altitudeDiff_W + altitudeDiff_T) - 400;
        } else {
            newAltitude = altitude + altitudeDiff_W + altitudeDiff_T;
        }
        
        System.out.println("FLIGHT CONTROLLER: wing flap adjusted successfully, "
                + "altitude is now at " + newAltitude + " sea level.");

    }

    //Change speed for good weather
    private void changeInSpeedFromEngine() throws InterruptedException {

        Thread.sleep(500);

        int engineSpeed = engine.getSpeed();

        switch (engineSpeed) {
            case 60:
            case 65: {
                planeSpeedDiff = ThreadLocalRandom.current().nextInt(51) + 70;
                break;
            }
            case 70: {
                planeSpeedDiff = ThreadLocalRandom.current().nextInt(51) + 80;
                break;
            }
            case 75:
            case 80: {
                planeSpeedDiff = ThreadLocalRandom.current().nextInt(51) + 90;
                break;
            }
            default:
                planeSpeedDiff = ThreadLocalRandom.current().nextInt(51) + 50;
                break;
        }

        if (planeSpeed >= 600) {
            newPlaneSpeed = planeSpeed - planeSpeedDiff - 30;
        } else {
            newPlaneSpeed = planeSpeed + planeSpeedDiff;
        }

        System.out.println("FLIGHT CONTROLLER: after successfully adjusted the "
                + "engine speed, plane is currently flying at " + newPlaneSpeed + " km/h.");
    }

    //Bad weather - turbulence, thunderstorm
    private void altLogicToAdjustBadWeatCon() throws InterruptedException {

        if (planeSpeed <= 450) {

            adjustFlightParameters(60, 40);

        } else if (planeSpeed > 450 && planeSpeed <= 500) {

            adjustFlightParameters(55, 45);

        } else if (planeSpeed > 500 && planeSpeed <= 530) {

            adjustFlightParameters(50, 50);

        } else if (planeSpeed > 530 && planeSpeed <= 570) {

            adjustFlightParameters(45, 55);

        } else if (planeSpeed > 570 && planeSpeed <= 600) {

            adjustFlightParameters(40, 60);

        } else if (planeSpeed > 600) {

            adjustFlightParameters(35, 65);

        }
    }

    //Set adjustment for actuator - tail flap, engine
    private void adjustFlightParameters(int tailFlapDegree, int engineSpeed) throws InterruptedException {

        System.out.println("------ MAKING ADJUSTMENTS FOR FLYING IN BAD WEATHER CONDITION -------");

        Thread.sleep(200);

        tailflap.setTailFlapAngle(tailFlapDegree);
        engine.setSpeed(engineSpeed);

        System.out.println("FLIGHT CONTROLLER: adjust tail flaps to " + tailFlapDegree + " degree.");
        System.out.println("FLIGHT CONTROLLER: adjust engine speed to " + engineSpeed + " rpm.");

        sendMessageToTailFlap(Integer.toString(tailFlapDegree));
        sendMessageToEngine(Integer.toString(engineSpeed));
        System.out.println("FLIGHT CONTROLLER: current adjusted cabin pressure: " + monitoringCabinPressure());

        changeInAltitudeFromTailAndEngine();
        sendNewAltitude(Integer.toString(newAltitude));
        sendNewPlaneSpeed(Integer.toString(newPlaneSpeed));
        sendNewCabinPressure(Integer.toString(monitoringCabinPressure()));
    }

    //Change altitude and speed for good weather
    private void changeInAltitudeFromTailAndEngine() throws InterruptedException {
        Thread.sleep(500);

        int tailFlapAngle = tailflap.getTailFlapAngle();
        int engineSpeed = engine.getSpeed();

        switch (tailFlapAngle) {

            case 60:
            case 55: {
                altitudeDiff_T = -(ThreadLocalRandom.current().nextInt(51) - 100);
                break;
            }
            case 50:
            case 45: {
                altitudeDiff_T = -(ThreadLocalRandom.current().nextInt(51) - 400);
                break;
            }
            case 40:
            case 35: {
                altitudeDiff_T = -(ThreadLocalRandom.current().nextInt(51) - 700);
                break;
            }
            default:
                altitudeDiff_T = ThreadLocalRandom.current().nextInt(51) - 80;
                break;
        }

        switch (engineSpeed) {

            case 40:
            case 45: {
                planeSpeedDiff = -(ThreadLocalRandom.current().nextInt(51) - 80);
                break;
            }
            case 50:
            case 55: {
                planeSpeedDiff = -(ThreadLocalRandom.current().nextInt(51) - 90);
                break;
            }
            case 60:
            case 65: {
                planeSpeedDiff = -(ThreadLocalRandom.current().nextInt(51) - 150);
                break;
            }
            default:
                planeSpeedDiff = -(ThreadLocalRandom.current().nextInt(51) - 70);
                break;
        }

        if (altitude >= 7000) {
            newAltitude = altitude + altitudeDiff_T - 50;
        } else {
            newAltitude = altitude + altitudeDiff_T;
        }

        if (planeSpeed >= 700) {
            newPlaneSpeed = planeSpeed + planeSpeedDiff - 50;
        } else {
            newPlaneSpeed = planeSpeed + planeSpeedDiff + 50;
        }

        System.out.println("FLIGHT CONTROLLER: tail flap adjusted successfully, "
                + "altitude is now at " + newAltitude + " sea level.");
        System.out.println("FLIGHT CONTROLLER: engine speed adjusted successfully, "
                + "plane is currently flying at " + newPlaneSpeed + " km/h.");
    }

    //Monitor cabin pressure - normal
    private int monitoringCabinPressure() throws InterruptedException {
        Thread.sleep(500);

        if (cabinPressure < 5) {
            System.out.println("FLIGHT CONTROLLER: adjusting cabin pressure loss to normal...");
            newCabinPressure = cabinPressure + 3;
        } else if (cabinPressure >= 5 && cabinPressure <= 7) {
            System.out.println("FLIGHT CONTROLLER: low in cabin pressure: " + cabinPressure);
            newCabinPressure = cabinPressure++;

        } else if (cabinPressure >= 8 && cabinPressure <= 10) {
            System.out.println("FLIGHT CONTROLLER: cabin pressure is at safe threshold, maintain cabin pressure.");
            newCabinPressure = cabinPressure;

        } else if (cabinPressure >= 11 && cabinPressure <= 13) {
            System.out.println("FLIGHT CONTROLLER: there's a slight increase in cabin pressure, continue monitoring.");
            newCabinPressure = cabinPressure--;

        } else if (cabinPressure >= 14) {
            System.out.println("FLIGHT CONTROLLER: high in cabin pressure: " + cabinPressure);
            newCabinPressure = cabinPressure - 2;
        }
        return newCabinPressure;
    }

    private void triggerCabinLoss() throws IOException, TimeoutException, InterruptedException {

        System.out.println("------------- *** EMERGENCY *** ------------");
        System.out.println("FLIGHT CONTROLLER: sudden cabin pressure loss detected !!!");
        System.out.println("FLIGHT CONTROLLER: current cabin pressure is far below safety threshold: " + newCabinPressure);
        System.out.println("------------- *** EMERGENCY *** ------------");

        sendNewCabinPressure(Integer.toString(newCabinPressure));
        System.out.println("FLIGHT CONTROLLER: request oxygen mask and emergency landing immediately !!!");

        triggerOxygenMasks();
        Thread.sleep(500);

        adjustForLanding();
        triggerLanding();
    }

    private void triggerOxygenMasks() throws InterruptedException {
        //trigger oxygen masks
        oxygenMasks.setReleasedStatus(true);
        String instruction = "release oxygen masks";
        System.out.println("FLIGHT CONTROLLER: " + instruction + ".");
        sendMessageToOxygenMasks(instruction);

    }

    private void adjustForLanding() throws InterruptedException {
        Thread.sleep(500);
        int wingFlapDegree = 50;
        int tailFlapDegree = 50;
        int engineSpeed = 60;

        wingflap.setWingFlapAngle(wingFlapDegree);
        tailflap.setTailFlapAngle(tailFlapDegree);
        engine.setSpeed(engineSpeed);

        System.out.println("FLIGHT CONTROLLER: adjust wing flaps to " + wingflap.getWingFlapAngle() + " degree.");
        System.out.println("FLIGHT CONTROLLER: adjust tail flaps to " + tailflap.getTailFlapAngle() + " degree.");
        System.out.println("FLIGHT CONTROLLER: adjust engine speed to " + engine.getSpeed() + " rpm.");

        sendMessageToWingFlap(Integer.toString(wingFlapDegree));
        sendMessageToTailFlap(Integer.toString(tailFlapDegree));
        sendMessageToEngine(Integer.toString(engineSpeed));

        altitudeDiff_W = -ThreadLocalRandom.current().nextInt(51) - 200;
        altitudeDiff_T = -ThreadLocalRandom.current().nextInt(51) - 100;
        planeSpeedDiff = ThreadLocalRandom.current().nextInt(51) - 200;
        newAltitude = altitude + altitudeDiff_W + altitudeDiff_T;
        newPlaneSpeed = planeSpeed + planeSpeedDiff;
        sendNewAltitude(Integer.toString(newAltitude));
        sendNewPlaneSpeed(Integer.toString(newPlaneSpeed));
        System.out.println("FLIGHT CONTROLLER: Successfully adjusted to handle emergency landing mode.");
        System.out.println("FLIGHT CONTROLLER: Proceed with landing procedure. ");
    }

    private void triggerLanding() throws InterruptedException {

        if (oxygenMasks.isReleasedStatus() == true) {

            int landingAltitude;
            int landingSpeed;

            while (newAltitude > 0 && newPlaneSpeed > 0) {
                newAltitude = Math.max(2500, newAltitude - (rand.nextInt(50) + 500));
                System.out.println("ALTITUDE SENSOR: latest adjusted altitude above sea level is " + newAltitude);
                sendNewAltitude(Integer.toString(newAltitude));

                Thread.sleep(500);
                newPlaneSpeed = Math.max(350, newPlaneSpeed - (rand.nextInt(50) + 80));
                System.out.println("SPEED SENSOR: latest adjusted plane speed is " + newPlaneSpeed);
                sendNewPlaneSpeed(Integer.toString(newPlaneSpeed));

                if (newAltitude <= 2500 && newPlaneSpeed <= 350) {
                    landingGear.setLandingGearReleasedStatus(true);
                    String instruction = "release landing gear";
                    System.out.println("FLIGHT CONTROLLER: " + instruction + ".");
                    sendMessageToLandingGear(instruction);
                    break;
                }
            }

            landingAltitude = newAltitude;
            landingSpeed = newPlaneSpeed;
            while (landingAltitude > 0 && landingSpeed > 0) {
                landingAltitude = Math.max(0, landingAltitude - (rand.nextInt(50) + 400));
                System.out.println("ALTITUDE SENSOR: latest adjusted altitude above sea level is " + landingAltitude);
                sendNewAltitude(Integer.toString(landingAltitude));

                Thread.sleep(500);
                landingSpeed = Math.max(0, landingSpeed - (rand.nextInt(50) + 60));
                System.out.println("SPEED SENSOR: latest adjusted plane speed is " + landingSpeed);
                sendNewPlaneSpeed(Integer.toString(landingSpeed));

                incPressuretoSafetyLevel(newCabinPressure);

                if (landingAltitude <= 0 || landingSpeed <= 0) {
                    break;
                }
            }

            if (landingAltitude <= 0 || landingSpeed <= 0) {
                landingStatus = true;
                System.out.println("FLIGHT CONTROLLER: The plane has landed.");
                System.out.println("FLIGHT CONTROLLER: Emergency landing successful! ");

                endTime = new Date().getTime();
                calculateExecution();

                System.exit(0);
            }
        }

    }

    public void incPressuretoSafetyLevel(int finalPressure) throws InterruptedException {

        while (finalPressure < 10) {
            finalPressure += (rand.nextInt(5));

            System.out.println("FLIGHT CONTROLLER: adjusting cabin pressure to " + finalPressure + ".");
            newCabinPressure = finalPressure;
            sendNewCabinPressure(Integer.toString(newCabinPressure));

            if (finalPressure >= 10) {
                System.out.println("CABIN PRESSURE SENSOR: (sudden loss) cabin pressure back to normal " + finalPressure + ".");
            }

        }
    }

    //Method to receive and consume all sensor data
    public void receiveSensorData() throws IOException {

        altitude_ch.basicConsume(altitude_qName, true, (x, msg) -> {

            String altitudeReading = new String(msg.getBody(), "UTF-8");
            altitude = Integer.parseInt(altitudeReading);

        }, x -> {
        });

        speed_ch.basicConsume(speed_qName, true, (y, msg) -> {

            String speedReading = new String(msg.getBody(), "UTF-8");
            planeSpeed = Integer.parseInt(speedReading);

        }, y -> {
        });

        cabinpressure_ch.basicConsume(cabinpressure_qName, true, (z, msg) -> {

            String cpresureReading = new String(msg.getBody(), "UTF-8");
            cabinPressure = Integer.parseInt(cpresureReading);

        }, z -> {
        });

        weather_ch.basicConsume(weather_qName, true, (w, msg) -> {

            String weather = new String(msg.getBody(), "UTF-8");
            weatherCon = weather;

        }, w -> {
        });

    }



    //Method to send message to wing flaps
    public void sendMessageToWingFlap(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToWingFlapActuator, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToWingFlapActuator, routingKeyToWingFlapActuator, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Method to send instruction to tail flaps
    public void sendMessageToTailFlap(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToTailFlapActuator, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToTailFlapActuator, routingKeyToTailFlapActuator, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Method to send instruction to engine
    public void sendMessageToEngine(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToEngine, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToEngine, routingKeyToEngine, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMessageToOxygenMasks(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToOxygenMasks, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToOxygenMasks, routingKeyToOxygenMasks, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMessageToLandingGear(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToLandingGear, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToLandingGear, routingKeyToLandingGear, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendNewAltitude(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToAltitudeSensor, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToAltitudeSensor, routingKeyToAltitudeSensor, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendNewPlaneSpeed(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToSpeedSensor, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToSpeedSensor, routingKeyToSpeedSensor, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendNewCabinPressure(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(exchangeNameToCabinPressure, "direct"); //TOPIC EXCHANGE

            chan.basicPublish(exchangeNameToCabinPressure, routingKeyToCabinPressure, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FCS_Logic.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void calculateExecution() {
        Altitude_Sensor altSensor = new Altitude_Sensor();
        startTime = altSensor.arrListStartTime.get(0);

        long totalRunTime = endTime - startTime;
        long runTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(totalRunTime);

        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);

        System.out.println("\n\n");
        System.out.println("Macro-Benchmarking: ");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Start Time: " + startDate);
        System.out.println("End Time: " + endDate);
        System.out.println("Total execution time in millieseconds: " + totalRunTime + "ms");
        System.out.println("Total execution time: " + runTimeInSeconds + "s");

    }

}
