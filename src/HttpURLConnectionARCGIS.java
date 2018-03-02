/**
 * Created by Samira Pouyanfar (spouy001@cs.fiu.edu) on 2/8/18.
 */
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;


import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileWriter;

import java.util.List;
import java.util.Arrays;


import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Demonstrates usage of the ArcGIS REST API with HTTPS.
 * Creates an output with ground elevation according to a given policy file.
 */
public class HttpURLConnectionARCGIS {

    //data to create the REST URL based on server, port and Albers Conical Equal Area [Florida Geographic Data Library] information
    private String URL_BASE = "https://";
    private String servicemap = "/arcgis/rest/services/flidar_mosaic_ft_w_data/MapServer/";
    private String host = ""; //add your host
    private String port = "";  // add your port number
    private String command = "identify";
    private String GeoType = "esriGeometryPoint";
    private String spatialReference = "4326";
    private String layer = "0";
    private String format = "=pjson";
    //Florida Map Extent
    private String mapExtend = "-87.6256%2C24.3959%2C-79.8198%2C31.0035";
    private String firstURLpart = servicemap + command;
    private String lastURLpart = "&geometryType=" + GeoType + "&sr=" + spatialReference + "&layers=" +
            layer + "&layerDefs=&time=&layerTimeOptions=" + "&tolerance=2" + "&mapExtent=" + mapExtend+"&imageDisplay=600%2C550%2C96"+
            "&returnGeometry=false&maxAllowableOffset=&geometryPrecision=&dynamicLayers=&returnZ=false&returnM=false&gdbVersion=&returnUnformattedValues=false&returnFieldName=false&datumTransformations=&layerParameterValues=&mapRangeValues=&layerRangeValues=&f" +
            format;

    private   void GetElevation(String input, String output) throws NoSuchAlgorithmException, KeyManagementException {
        String line = null;
        int counter = 0;
        String elevation = null;
        List<String> currnetRow = new ArrayList<>();
        try {
            FileWriter writer = new FileWriter(output);
            BufferedReader policyFile = new BufferedReader(new FileReader(input));
            while ((line = policyFile.readLine()) != null) {

                currnetRow = Arrays.asList(line.split(","));
                //skip first row (column name)
                if (counter != 0) {
                    //get second and third columns as lat, long
                    elevation = Connection(generateURL(currnetRow.get(1), currnetRow.get(2)));
                    //save elevation in third column
                    currnetRow.set(3, elevation);
                    System.out.println(elevation);
                    String collect = currnetRow.stream().collect(Collectors.joining(","));
                    writer.write(collect);
                    writer.write("\n");

                }
                else {
                    writer.write(line);
                    writer.write("\n");
                }
                counter++;
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(counter);
    }


    private  URL generateURL(String latitude, String longitude) throws MalformedURLException {
        URL url = null;
        StringBuffer params = new StringBuffer();

        params.append(firstURLpart);
        //add long and lat to the url
        params.append("?geometry=" + longitude + "%2C" + latitude);
        params.append(lastURLpart);

        url = new URL(URL_BASE + host + ':' + port  + params);

        return url;
    }

    private  String Connection(URL url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        String raster = null;
        String method = "POST";
        String userName = "";
        String password = "";
        String authentication = userName + ':' + password;
        // open HTTPS connection
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);


        HttpURLConnection connection = null;
        connection = (HttpsURLConnection) url.openConnection();
        //((HttpsURLConnection) connection).setHostnameVerifier(new MyHostnameVerifier());
        //connection.setRequestProperty("Content-Type", "text/plain; charset=\"utf8\"");
        connection.setRequestMethod(method);
        //BASE64Encoder encoder = new BASE64Encoder();
        //String encoded = encoder.encode((authentication).getBytes("UTF-8"));
        //connection.setRequestProperty("Authorization", "Basic " + encoded);
        // execute HTTPS request
        int returnCode = connection.getResponseCode();
        InputStream connectionIn = null;
        if (returnCode == 200)
            connectionIn = connection.getInputStream();
        else
            connectionIn = connection.getErrorStream();
        // print resulting stream
        BufferedReader buffer = new BufferedReader(new InputStreamReader(connectionIn));
        String inputLine;
        while ((inputLine = buffer.readLine()) != null) {
            //sb.append(inputLine);
            //Get raster pixel value
            if (inputLine.contains("Pixel Value")) {
                raster = inputLine.substring(20);
                raster = raster.substring(0, raster.length() - 2);
                //System.out.println(raster);
                break;
            }
        }
        buffer.close();
        return raster;
    }

    private TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }
    };


    public static void main(String[] args) throws Exception {
        Instant start = Instant.now();
        // input format: ID,lat,long,elevation,...
        String orgPolicyPath = "data/test.csv";
        // output format: ID,lat,long,elevation,...
        String finalElevationPath = "data/test_elv.csv";
        HttpURLConnectionARCGIS arcgisRest = new HttpURLConnectionARCGIS();
        arcgisRest.GetElevation(orgPolicyPath, finalElevationPath);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: "+ timeElapsed.toMillis() +" milliseconds");
    }
}
