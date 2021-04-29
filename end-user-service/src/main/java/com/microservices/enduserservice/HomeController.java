package com.microservices.enduserservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HomeController {

    @Autowired
    RestTemplate restTemplate;

    @PostMapping("/student")
    public ResponseEntity<String> student(@RequestBody Student student) throws JsonProcessingException {
        String result = restTemplate.postForObject("http://data-aggregation-service/calculateGrades", student, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GradesResult gradesResult = objectMapper.readValue(result, GradesResult.class);
        return ResponseEntity.status(HttpStatus.OK).body(String.format("Sent the Student to the Data Aggregation Service: %s \nAnd got back:\n %s", student.toString(), gradesResult.toString()));
    }
}
