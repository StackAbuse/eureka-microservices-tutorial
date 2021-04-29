### Introduction

In this guide, we'll be utilizing <a rel="nofollow nooopener" target="_blank" href="https://github.com/Netflix/eureka">*Netflix Eureka*</a>, a microservice discovery service to combine a Spring Boot microservice h a Flask microservice, bridging services written in totally different programming languages and frameworks.

We'll be building two services - *End-User Service*, which is a Spring Boot service oriented at the end-user, and collects data, and a *Data-Aggregation Service* which is a Python service, using Pandas to perform data aggregation, and returns a JSON response to the end-user.

### Netflix Eureka Serice Discovery

When switching from a monolith codebase to a microservice-oriented architecture - Netflix built a plethora of tools that helped them overhaul their entire architecture. One of the in-house solutions, which was subsequently released to the public is *Eureka*.

**Netflix Eureka** a *service discovery* tool (also known as a *lookup server* or *service registry*), that allows us to register multiple microservices, and handles request routing between them.

It's a central hub where each service is registered, and each of them communicates with the rest through the hub. Instead of sending REST calls via hostnames and ports - we delegate this to Eureka, and simply call the *name* of the service, as registered in the hub.

To achieve this, a typical architecture consists of a few elements:

![eureka microservice architecture](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-0.png)

You can spin off the Eureka Server in any language that has a Eureka wrapper, though, it's most naturally done in Java, through Spring Boot, since this is the original implementation of the tool, with official support.

Each Eureka Server can register *N* Eureka Clients, each of which is typically an individual project. These can also be done in any language or framework, so each microservice uses what's most suitable for their task.

We'll have two clients:

- **End-User Service** (Java-based Eureka Client)
- **Data-Aggregation Service** (Python-based Eureka Client)

Since Eureka is a Java-based project, originally meant for Spring Boot solutions - it doesn't have an *official* implementation for Python. However, we can use a community-driven Python wrapper for it:

- <a rel="nofollow noopener" target="_blank" href="https://github.com/Netflix/eureka">**Netflix Eureka**</a>
- <a rel="nofollow noppener" target="_blank" href="https://pypi.org/project/py-eureka-client/">**Python's Eureka Client**</a>

With that in mind, let's create an *Eureka Server* first.

### Creating a Eureka Server

We'll be using Spring Boot to create and maintain our Eureka Server. Let's start off by making a directory to house our three projects:

```console
$ mkdir eureka-microservices
$ cd eureka-microservices
$ mkdir eureka-server
$ cd eureka-server
```

The `eureka-server` directory will be the root directory of our Eureka Server. You can start a Spring Boot project here through the CLI:

```console
$ spring init -d=spring-cloud-starter-eureka-server
```

Alternatively, you can use <a rel="nofollow noopener" target="_blank" href="https://start.spring.io/">Spring Initializr</a> and include the *Eureka Server* dependency:

![spring initializr eureka server](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-1.png)

If you already have a project and just wish to include the new depdenency, if you're using Maven, add:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-eureka-server</artifactId>
    <version>${version}</version>
</dependency>
```

Or if you're using Gradle:

```gradle
implementation group: 'org.springframework.cloud', name: 'spring-cloud-starter-eureka-server', version: ${version}
```

Regardless of the initialization type - the Eureka Server requires a *single* anotation to be marked as a server.

In your `EndUserApplication` file class, which is our entry-point with the `@SpringBootApplication` annotation, we'll just add an `@EnableEurekaServer`:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

The default port for Eureka Servers is `8761`, and it's also recommended by the Spring Team. Though, for good measure, let's set it in the `application.properties` file as well:

```properties
server.port=8761
```

With that done, our server is ready to run. Running this project will start up the Eureka Server, available at `localhost:8761`:

![eureka server](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-2.png)

**Note:** Without registering any services, Eureka may incorrectly claim an *UNKNOWN* instance is up.

### Creating a Eureka Client - End User Service in Spring Boot

Now, with our server spun up and ready to register services, let's go ahead and make our *end user service* in Spring Boot. It'll have a single endpoint that accepts JSON data regarding a *Student*. This data is then sent as JSON to our Data Aggregation service that calculates general statistics of the grades.

In practice, this operation would be replaced with much more labor-intensive operations, which make sense to be done in dedicated data processing libraries and which justify the use of another service, rather than performing them on the same one.

That being said, let's go back and create a directory for our *End User Service*:

```console
$ cd..
$ mkdir end-user-service
$ cd end-user-service
```

Here, let's start a new project via the CLI, and include the `spring-cloud-starter-netflix-eureka-client` dependency. We'll also add the `web` dependency since this application will actually be facing the user:

```console
$ spring init -d=web, spring-cloud-starter-netflix-eureka-client
```

Alternatively, you can use <a rel="nofollow noopener" target="_blank" href="https://start.spring.io/">Spring Initializr</a> and include the *Eureka Discovery Client* dependency:

![spring initializr eureka client](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-3.png)

If you already have a project and just wish to include the new depdenency, if you're using Maven, add:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    <version>${version}</version>
</dependency>
```

Or if you're using Gradle:

```gradle
implementation group: 'org.springframework.cloud', name: 'spring-cloud-starter-netflix-eureka-client', version: ${version}
```

Regardless of the initialization type - to mark this application as a Eureka Client, we simply add the `@EnableEurekaClient` annotation to the main class:

```java
@SpringBootApplication
@EnableEurekaClient
public class EndUserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EndUserServiceApplication.class, args);
    }
    
    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Note:** Alternatively, you can use the `@EnableDiscoveryClient` annotation, which is a more wide-encompassing annotation. It can refer to Eureka, Consul *or* Zookeper, depending on which tool is being used.

We've also defined a `@Bean` here, so that we can `@Autowire` the `RestTemplate` later on in our controller. This `RestTemplate` will be used to send a `POST` request to the *Data Aggregation Service*. The `@LoadBalanced` annotation signifies that our `RestTeamplate` should use a `RibbonLoadBalancerClient` when sending requests.

Since this application is a Eureka Client, we'll want to give it a *name* for the registry. Other services will refer to this name when relying on it. The name is defined in the `application.properties` or `application.yml` file:

```properties
server.port = 8060
spring.application.name = end-user-service
eureka.client.serviceUrl.defaultZone = http://localhost:8761/eureka
```

```properties
server:
    port: 8060
spring:
    application:
        name: end-user-service
eureka:
    client:
      serviceUrl:
        defaultZone: http://localhost:8761/eureka/
```

Here, we've set the port for our application, which Eureka needs to know to route requests to it. We've also specified the name of the service, which will be referenced by other services.

Running this application will register the service to the Eureka Server:

```plaintext
INFO 3220 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8060 (http) with context path ''
INFO 3220 --- [           main] .s.c.n.e.s.EurekaAutoServiceRegistration : Updating port to 8060
INFO 3220 --- [nfoReplicator-0] com.netflix.discovery.DiscoveryClient    : DiscoveryClient_END-USER-SERVICE/DESKTOP-8HAKM3G:end-user-service:8060 - registration status: 204
INFO 3220 --- [           main] c.m.e.EndUserServiceApplication          : Started EndUserServiceApplication in 1.978 seconds (JVM running for 2.276)
INFO 3220 --- [tbeatExecutor-0] com.netflix.discovery.DiscoveryClient    : DiscoveryClient_END-USER-SERVICE/DESKTOP-8HAKM3G:end-user-service:8060 - Re-registering apps/END-USER-SERVICE
INFO 3220 --- [tbeatExecutor-0] com.netflix.discovery.DiscoveryClient    : DiscoveryClient_END-USER-SERVICE/DESKTOP-8HAKM3G:end-user-service:8060: registering service...
INFO 3220 --- [tbeatExecutor-0] com.netflix.discovery.DiscoveryClient    : DiscoveryClient_END-USER-SERVICE/DESKTOP-8HAKM3G:end-user-service:8060 - registration status: 204
```

Now, if we visit `localhost:8761`, we'll be able to see it registered on the server:

![eureka server registered service](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-4.png)

Now, let's go ahead and define a `Student` model:

```java
public class Student {
    private String name;
    private double mathGrade;
    private double englishGrade;
    private double historyGrade;
    private double scienceGrade;
    
    // Constructor, getters and setters and toString()
}
```

For a student, we'll want to calculate some *summary statistics* of their performance, such as the *mean, minimum and maximum* of their grades. Since we'll be using Pandas for this - we'll leverage the very handy `DataFrame.describe()` function. Let's make a `GradesResult` model as well, that'll hold our data once returned from the *Data Aggregation Service*:

```java
public class GradesResult {
    private Map<String, Double> mathGrade;
    private Map<String, Double> englishGrade;
    private Map<String, Double> historyGrade;
    private Map<String, Double> scienceGrade;
    
    // Constructor, getters, setters and toString()
}
```

With the models done, let's make a really simple `@RestController` that accepts a `POST` request, deserializes it into a `Student` and sends it off to the *Dat Aggregation* service, which we haven't made yet:

```java
@RestController
public class HomeController {
    @PostMapping("/student")
    public ResponseEntity<String> student(@RequestBody Student student) {
        RestTemplate restTemplate = new RestTemplate();
        GradesResult grades = restTemplate.getForObject("http://data-aggregation-service/calculateGrades", GradesResult.class);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(String.format("Sent the Student to the Data Aggregation Service: %s \nAnd got back:\n %s", student.toString(), gradesResult.toString()));
    }
}
```

This `@RestController` accepts a `POST` request, and deserializes its body into a `Student` object. Then, we're sending a request to our `data-aggregation-service`, which isn't yet implemented, as registered on Eureka, and we pack the JSON results of that call into our `GradesResult` object.

Finally, we print the `student` instance we've sent as well as the `grades` instance we constructed from the result.

Now, let's go ahead and create the *Data Aggregation Service*.

### Creating a Eureka Client - Data Aggregation Service in Flask

The only missing component is the *Data Aggregation Service*, which accepts a *Student*, in JSON format and populates a Pandas `DataFrame`, performs certain operations and returns the result back.

Let's create a directory for our project and start a virtual environment for it:

```console
$ cd..
$ mkdir data-aggregation-service
$ python3 -m venv flask-microservice
```

Now, to activate the virtual environment, run the `activate` file. On Windows:

```console
$ flask-microservice/Scripts/activate.bat
```

On Linux/Mac:

```console
$ source flask-microservice/bin/activate
```

We'll be spinning up a simple Flask application for this, so let's install the dependencies for both Flask and Eureka via `pip` in our activated environment:

```console
(flask-microservice) $ pip install flask pandas py-eureka-client requests
```

And now, we can create our Flask application:

```console
$ touch flask_app.py
```

Now, open the `flask_app.py` file and import Flask, Pandas and the Py-Eureka Client libraries:

```python
from flask import Flask, request
import pandas as pd
import py_eureka_client.eureka_client as eureka_client
```

We'll be using Flask and `request` to handle our incoming requests and return a response, as well a spin up a server. We'll be using Pandas to aggregate data, and we'll use the `py_eureka_client` to register our Flask application to the Eureka Server on `localhost:8761`.

Let's go ahead and set this application up as a Eureka Client and implement a `POST` request handler for the student data:

```python
rest_port = 8050
eureka_client.init(eureka_server="http://localhost:8761/eureka",
                   app_name="data-aggregation-service",
                   instance_port=rest_port)

app = Flask(__name__)

@app.route("/calculateGrades", methods=['POST'])
def hello():
    data = request.json
    df = pd.DataFrame(data, index=[0])
    response = df.describe().to_json()
    return response

if __name__ == "__main__":
    app.run(host='0.0.0.0', port = rest_port)
```

**Note:** We have to set the host to `0.0.0.0` to open it to external services, lest Flask refuse them to connect.

This is a pretty minimal Flask app with a single `@app.route()`. We've extracted the incoming `POST` request body into a `data` dictionary through `request.json`, after which we've made a `DataFrame` with that data.

Since this dictionary doesn't have an index at all, we've manually set one.

Finally, we've returned the `describe()` function's results as JSON. We haven't used `jsonify` here since it returns a `Response` object, not a String. A `Response` object, when sent back would contain extra `\` characters:

```json
{\"mathGrade\":...}
vs
{"mathGrade":...}
```

These would have to be escaped, lest they throw off the deserializer.

In the `init()` function of `eureka_client`, we've set the URL to our Eureka Server, as well as set the name of the application/service for discovery, as well as supplied a port on which it'll be accessible. This is the same information we've provided in the Spring Boot application.

Now, let's run this Flask application:

```console
(flask-microservice) $ python flask_app.py
```

And if we check our Eureka Server on `localhost:8761`, it's registered and ready to receive requests:

![eureka server registered service](https://s3.amazonaws.com/s3.stackabuse.com/media/articles/spring-boot-and-flask-microservices-eureka-client-5.png)

### Calling Flask Service from Spring Boot Service using Eureka

With both of our services up and running, registered to Eureka and able to communicate with each other, let's send a `POST` request to our *End-User Service*, containing some student data, which will in turn send a `POST` request to the *Data Aggregation Service*, retrieve the response, and forward it to us:

```console
$ curl -X POST -H "Content-type: application/json" -d "{\"name\" : \"David\", \"mathGrade\" : \"8\", \"englishGrade\" : \"10\", \"historyGrade\" : \"7\", \"scienceGrade\" : \"10\"}" "http://localhost:8060/student"
```

This results in a response from the server to the end-user:

```plaintext
Sent the Student to the Data Aggregation Service: Student{name='David', mathGrade=8.0, englishGrade=10.0, historyGrade=7.0, scienceGrade=10.0}
And got back:
GradesResult{mathGrade={count=1.0, mean=8.0, std=null, min=8.0, 25%=8.0, 50%=8.0, 75%=8.0, max=8.0}, englishGrade={count=1.0, mean=10.0, std=null, min=10.0, 25%=10.0, 50%=10.0, 75%=10.0, max=10.0}, historyGrade={count=1.0, mean=7.0, std=null, min=7.0, 25%=7.0, 50%=7.0, 75%=7.0, max=7.0}, scienceGrade={count=1.0, mean=10.0, std=null, min=10.0, 25%=10.0, 50%=10.0, 75%=10.0, max=10.0}}
```

### Conclusion

In this guide, we've created a microservice environment, where one service relies on another, and hooked them up using Netflix Eureka.

These services are built using different frameworks, and different programming languages - though, through REST APIs, communicating between them is straightforward and easy.

The source code for these two services, including the Eureka Server is available on <a target="_blank" href="https://github.com/StackAbuse/eureka-microservices-tutorial">Github</a>.