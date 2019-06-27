package com.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jaegertracing.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/")
public class ServiceAPI {

	@Autowired
	MeterRegistry registry;

	// vars used by healthCheck endpoint
	public static long instanceId = new Random().nextInt();
	public static int count = 0;
	
	// vars used by 'random' endpoint
	OkHttpClient client = new OkHttpClient();
	public Tracer tracer = new Configuration("main-service").getTracer();
	
	/*
	 * returns a string containing current date/time, internal request counter and message
	 */
	@GetMapping("/")
	public String healthCheck() {
		// get response string
		String output = makeHealthCheckResponse();
		
		// increment Prometheus 'request-count' metric
		registry.counter("custom.metrics.request.count", "value", "HEALTHCHECK").increment();

		// return response
		return output;
	}

	private String makeHealthCheckResponse() {
		
		// increment internal counter
		count += 1;
		
		// get and format date
		Date date = new Date();
		String dateformat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.FULL).format(date);
		
		// assemble output and return
		String output = "<h3>The metrics-tracing-demo app is up and running!</h3>" + "<br/>Instance: " + instanceId
				+ ", " + "<br/>DateTime: " + dateformat + "<br/>CallCount: " + count;	
		return output;
	}
	
	/*
	 * returns a random number that is obtained from a back-end service
	 */
	@GetMapping("/random")
	public String randomNumber() throws IOException {
		
		// Start the Jaeger trace
		Span mainSpan = tracer.buildSpan("get-number").start();
		
		// Call the back-end api to get the data
		String randomInt = makeAPIRequest("http://localhost:8081/random", mainSpan);

		// Mark the trace as completed
		mainSpan.finish();
		
		// return data 
		return randomInt;
	}

    private String makeAPIRequest(String url, Span span) throws IOException {
    	
    	// build the back-end HTTP API request 
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // add a request header holding the current span
        tracer.inject( span.context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));

        // build the request
        Request request = requestBuilder.build();

        // execute the request and return the response body
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }	
	

	
}
