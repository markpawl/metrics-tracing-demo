package com.example.demo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/")
public class ServiceAPI {

	public static long instanceId = new Random().nextInt();
	public static int count = 0;

	@Autowired
	MeterRegistry registry;

	@GetMapping
	public String healthCheck() {
		count += 1;
		Date date = new Date();
		String dateformat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.FULL)
				.format(date);
		String output = "<h3>The metrics-tracing-demo app is up and running!</h3>" + "<br/>Instance: " + instanceId + ", "
				+ "<br/>DateTime: " + dateformat + "<br/>CallCount: " + count;
		
		registry.counter("custom.metrics.reqcount", "value", "GET_ROOT" ).increment();
		// registry.gauge("custom.metrics.reqgauge", count);
		
		return output;
		
	}

}
