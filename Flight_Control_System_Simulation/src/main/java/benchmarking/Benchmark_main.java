/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package benchmarking;

import java.io.IOException;

/**
 *
 * @author YEE ZHI YING
 */
public class Benchmark_main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here

        org.openjdk.jmh.Main.main(getParams());
    }

    public static String[] getParams() {

        return new String[]{"AltitudeSensor_BenchMark"};

    }
}
